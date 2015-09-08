package no.haagensoftware.hyrrokkin.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import no.haagensoftware.hyrrokkin.annotations.SerializedClassName;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jhsmbp on 07/09/15.
 */
public abstract class HyrrokkinSerializer extends HyrrokkinPluralization {
    public HyrrokkinSerializer() {
        super();
    }

    public HyrrokkinSerializer(Map<String, String> pluralMap) {
        super(pluralMap);
    }

    public abstract JsonElement serialize(Object src);

    /**
     * Gets the ID of the object, either as the id-property or of the property with the
     * annotation @SerializedName("id")
     * @param object
     * @return
     */
    protected String getId(Object object) {
        String id = null;

        //Use Class name as rootKey by default
        String rootKey = object.getClass().getName().substring(0, 1).toLowerCase() + object.getClass().getName().substring(1);

        //If object has SerializedClassName annotation, use that instead as rootKey
        if (object.getClass().isAnnotationPresent(SerializedClassName.class)) {
            rootKey = object.getClass().getAnnotation(SerializedClassName.class).value();
        }

        rootKey = getPluralFor(rootKey);

        if (hasField(object.getClass(), "id")) {
            id = getFieldStringValue(object, "id");
        } else {
            for (Field field : object.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(SerializedName.class) && field.isAnnotationPresent(Expose.class)) {
                    String serializedName = ((SerializedName)field.getAnnotation(SerializedName.class)).value();
                    if (serializedName.equals("id")) {
                        id = getFieldStringValue(object, field.getName());
                        break;
                    }
                }
            }
        }

        if (id.startsWith(rootKey + "_")) {
            id = id.substring(0, rootKey.length() + 1);
        }

        return id;
    }

    /**
     * Return the string value of a field
     * @param object
     * @param fieldName
     * @return
     */
    protected String getFieldStringValue(Object object, String fieldName) {
        String value = null;
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            if (field.get(object) != null) {
                value = field.get(object).toString();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * This method will get all declared fields from this class, and any super class up to the exclusiveParent, which
     * in most cases is Object.
     * @param startClass
     * @param exclusiveParent
     * @return
     */
    public static Iterable<Field> getFieldsUpTo(Class<?> startClass,
                                                Class<?> exclusiveParent) {

        List<Field> currentClassFields = new ArrayList<>();
        for (Field f : startClass.getDeclaredFields()) {
            currentClassFields.add(f);
        }
        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null &&
                (exclusiveParent == null || !(parentClass.equals(exclusiveParent)))) {
            List<Field> parentClassFields =
                    (List<Field>) getFieldsUpTo(parentClass, exclusiveParent);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }

    /*
     * This method is in need of refactoring. Its too large, and has a too-wide responsibility. This makes it hard to follow its recursive logic.
     *
     * This method takes an object which can also be an array or a List, and adds any new object to the rootKeys hashtable. It recursively calls itself
     * whenever it reaches a property of type Object, Array or List, marked with the @Expose annotation
     *
     */
    protected void extractObject(Object src, Hashtable<String, Hashtable<String, JsonObject>> rootKeys) {
        JsonObject rootObject = new JsonObject();

        String classId = getId(src);
        String className = getRootKeyForClass(src);

        //Only extract this object if an object with the same root key and id has NOT been
        //extracted before. This is to prevent circular references causing StackOverflows
        if (rootKeys.get(className) == null || rootKeys.get(className).get(classId) == null) {
            for (Field field : getFieldsUpTo(src.getClass(), Object.class)) {
                if (field.isAnnotationPresent(Expose.class)) {

                    String fName = field.getName();
                    if (field.isAnnotationPresent(SerializedName.class)) {
                        fName = ((SerializedName) field.getAnnotation(SerializedName.class)).value();
                    }
                    Type fType = field.getGenericType();

                    Class clazz = field.getType();

                    //System.out.println("class: " + src.getClass().getSimpleName() + " field: " + field.getType().getName());

                    JsonElement element = null;

                    field.setAccessible(true);
                    try {
                        if (isPrimitive(clazz)) {
                            element = getPrimitiveValue(clazz, field.get(src));
                        } else if (clazz.isArray() || clazz.equals(List.class)) {
                            List list = null;
                            if (clazz.isArray()) {
                                list = new ArrayList();
                                Object[] objArray = (Object[]) field.get(src);
                                for (Object obj : objArray) {
                                    list.add(obj);
                                }
                            } else {
                                list = ((List) field.get(src));
                            }

                            if (list == null || list.size() == 0) {
                                element = new JsonArray();
                            } else if (list != null && list.size() > 0) {
                                JsonArray array = new JsonArray();

                                for (Object o : list) {
                                    String id = getId(o);
                                    if (isPrimitive(o.getClass())) {
                                        array.add(getPrimitiveValue(o.getClass(), o));
                                    } else if (id != null) {
                                        try {
                                            Field idField = o.getClass().getDeclaredField("id");
                                            idField.setAccessible(true);

                                            JsonObject relationshipObject = new JsonObject();
                                            relationshipObject.add("id", getPrimitiveValue(idField.getType(), idField.get(o)));
                                            relationshipObject.addProperty("type", getRootKeyForClass(o));

                                            array.add(relationshipObject);

                                            extractObject(o, rootKeys);
                                        } catch (NoSuchFieldException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                element = array;
                            }

                        } else if (clazz.getName().equals("java.util.Date")) {
                            Date dValue = (Date) field.get(src);
                            if (dValue != null) {
                                String dateStr = buildIso8601Format().format(dValue);
                                element = new JsonPrimitive(dateStr);
                            }

                        } else if ((!clazz.equals(Object.class)) && field.get(src) != null && hasField(field.get(src).getClass(), "id")) {
                            JsonObject relationshipObject = new JsonObject();
                            relationshipObject.add("id", new JsonPrimitive(getId(field.get(src))));
                            relationshipObject.addProperty("type", decapitalize(getRootKeyForClass(field.get(src))));

                            element = relationshipObject;
                            extractObject(field.get(src), rootKeys);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    if (element != null) {
                        rootObject.add(fName, element);
                    }

                    if (rootKeys.get(className) == null) {
                        Hashtable<String, JsonObject> objects = new Hashtable<>();
                        rootKeys.put(className, objects);
                    }

                    if (getId(src) != null && rootKeys.get(className).get(getId(src)) == null) {
                        rootKeys.get(className).put(getId(src), rootObject);
                    }

                    //System.out.println("\tField has Expose annotation: " + field.getName() + " field name: " + fName + " isPrimitive: " + isPrimitive + " isWrapperType: " + isWrapperType + " type: " + type + " element: " + element);
                }
            }
        }
    }

    protected String getRootKeyForClass(Object src) {
        String className = src.getClass().getSimpleName();
        if (src.getClass().isAnnotationPresent(SerializedClassName.class)) {
            className = src.getClass().getAnnotation(SerializedClassName.class).value();
        }
        return className;
    }

    /**
     * Checks is a class is one of the primitive types and returns true||false depending
     * @param clazz
     * @return
     */
    protected boolean isPrimitive(Class clazz) {
        return clazz.equals(String.class) ||
                clazz.equals(Double.TYPE) || clazz.getName().equals("java.lang.Double") ||
                clazz.equals(Integer.TYPE) || clazz.getName().equals("java.lang.Integer") ||
                clazz.equals(Boolean.TYPE) || clazz.getName().equals("java.lang.Boolean") ||
                clazz.equals(Long.TYPE) || clazz.getName().equals("java.lang.Long");

    }

    /**
     * Getting the primitive value returned from the class
     * @param clazz
     * @param value
     * @return
     * @throws IllegalAccessException
     */
    protected JsonPrimitive getPrimitiveValue(Class clazz, Object value) throws IllegalAccessException {
        JsonPrimitive element = null;

        if (value == null) {
            return null;
        }

        if (clazz.equals(String.class)) {
            element = new JsonPrimitive((String)value);
        } else if (clazz.equals(Double.TYPE) || clazz.getName().equals("java.lang.Double")) {
            element = new JsonPrimitive((Double)value);
        } else if (clazz.equals(Integer.TYPE) || clazz.getName().equals("java.lang.Integer")) {
            element = new JsonPrimitive((Integer)value);
        } else if (clazz.equals(Boolean.TYPE) || clazz.getName().equals("java.lang.Boolean")) {
            element = new JsonPrimitive((Boolean)value);
        } else if (clazz.equals(Long.TYPE) || clazz.getName().equals("java.lang.Long")) {
            element = new JsonPrimitive((Long)value);
        }

        return element;
    }

    /**
     * Checks if a class has a field or not. Mostly used to check if there is an id-property on the class
     * @param clazz
     * @param fieldName
     * @return
     */
    protected boolean hasField(Class clazz, String fieldName) {
        boolean hasField = false;

        try {
            Field f = clazz.getDeclaredField(fieldName);
            hasField = (f != null);
        } catch (NoSuchFieldException e) {
            hasField = false;
        }

        return hasField;
    }

    /**
     * JavaScript expects dates to be encoded in ISO8601. This method returns a DateFormat
     * @return
     */
    protected static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }

    protected String decapitalize(String input) {
        return Introspector.decapitalize(input);
    }


}
