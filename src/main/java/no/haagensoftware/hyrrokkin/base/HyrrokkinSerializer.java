package no.haagensoftware.hyrrokkin.base;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import no.haagensoftware.hyrrokkin.annotations.SerializedClassName;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jhsmbp on 07/09/15.
 */
public abstract class HyrrokkinSerializer extends HyrrokkinSerializationBase {
    public HyrrokkinSerializer() {
        super();
    }

    public HyrrokkinSerializer(Map<String, String> pluralMap) {
        super(pluralMap);
    }

    public abstract JsonElement serialize(Object src);

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
    protected void extractObject(Object src, Hashtable<String, Hashtable<String, JsonObject>> rootKeys, List<String> sideloadKeys, boolean embedded) {
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
                                        String objectId = getId(o);

                                        String rootKeyForClass = decapitalize(getRootKeyForClass(o));

                                        if (embedded
                                                && sideloadKeys != null
                                                && (sideloadKeys.contains(rootKeyForClass) || sideloadKeys.contains("all"))) {

                                            array.add(new Gson().toJsonTree(o));
                                        } else if (!embedded) { //sideloaded
                                            JsonObject relationshipObject = new JsonObject();
                                            relationshipObject.add("id", new JsonPrimitive(objectId));
                                            relationshipObject.addProperty("type", rootKeyForClass);

                                            array.add(relationshipObject);

                                            if (sideloadKeys != null && (sideloadKeys.contains(rootKeyForClass) || sideloadKeys.contains("all"))) {
                                                extractObject(o, rootKeys, sideloadKeys, embedded);
                                            }
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
                            String rootKeyForClass = decapitalize(getRootKeyForClass(field.get(src)));

                            JsonObject relationshipObject = new JsonObject();
                            relationshipObject.add("id", new JsonPrimitive(getId(field.get(src))));
                            relationshipObject.addProperty("type", rootKeyForClass);

                            element = relationshipObject;

                            if (sideloadKeys != null && (sideloadKeys.contains(rootKeyForClass) || sideloadKeys.contains("all"))) {
                                extractObject(field.get(src), rootKeys, sideloadKeys, embedded);
                            }
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
}
