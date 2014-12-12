package no.haagensoftware.hyrrokkin.serializer;

import com.google.gson.*;
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
 * Created by jhsmbp on 08/12/14.
 */
public class RestSerializer  {
    /**
     * This is the only public method, and will serialize any input POJO
     * @param src
     * @return
     */
    public JsonElement serialize(Object src) {
        //These are used to determine if the root keys are objects or arrays
        boolean inputIsList = false;
        String inputObjectRootKey = null;


        //There is one hash table for the root keys, and one hash table for each object of a specific type
        //This will hinder duplicate objects in the return JSON
        Hashtable<String, Hashtable<String, JsonObject>> rootKeys = new Hashtable<>();

        if (List.class.isAssignableFrom(src.getClass())) {
            inputIsList = true;
            for (Object obj : ((List)src)) {
                extractObject(obj, rootKeys);
            }
        } else {
            String className = src.getClass().getSimpleName();
            if (src.getClass().isAnnotationPresent(SerializedClassName.class)) {
                className = src.getClass().getAnnotation(SerializedClassName.class).value();
            }

            inputObjectRootKey = className;
            extractObject(src, rootKeys);
        }

        JsonObject topObject = generateJson(rootKeys, inputIsList, inputObjectRootKey);

        return topObject;
    }

    /**
     * Generating JSON from the rootKeys hashtable
     * @param rootKeys
     * @return
     */
    private JsonObject generateJson(Hashtable<String, Hashtable<String, JsonObject>> rootKeys, boolean inputIsList, String inputObjectRootKey) {
        JsonObject topObject = new JsonObject();

        for (String key : rootKeys.keySet()) {
            //System.out.println(key);
            //
            if (key.equals(inputObjectRootKey) && !inputIsList) {
                for (String objKey : rootKeys.get(key).keySet()) {
                    topObject.add(decapitalize(getSingularFor(key)), rootKeys.get(key).get(objKey));
                }
            } else {
                JsonArray array = new JsonArray();
                for (String objKey : rootKeys.get(key).keySet()) {
                    array.add(rootKeys.get(key).get(objKey));
                    //System.out.println("\t" + objKey + ": " + rootKeys.get(key).get(objKey).toString());
                }
                topObject.add(decapitalize(getPluralFor(key)), array);
            }
        }

        return topObject;
    }

    /**
     * Gets the ID of the object, either as the id-property or of the property with the
     * annotation @SerializedName("id")
     * @param object
     * @return
     */
    private String getId(Object object) {
        String id = null;

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

        return id;
    }

    /**
     * Return the string value of a field
     * @param object
     * @param fieldName
     * @return
     */
    private String getFieldStringValue(Object object, String fieldName) {
        String value = null;
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            value = field.get(object).toString();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }

    /*
     * This method is in need of refactoring. Its too large, and has a too-wide responsibility. This makes it hard to follow its recursive logic.
     *
     * This method takes an object which can also be an array or a List, and adds any new object to the rootKeys hashtable. It recursively calls itself
     * whenever it reaches a property of type Object, Array or List, marked with the @Expose annotation
     *
     */
    private void extractObject(Object src, Hashtable<String, Hashtable<String, JsonObject>> rootKeys) {
        JsonObject rootObject = new JsonObject();

        for (Field field : src.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Expose.class)) {

                String fName = field.getName();
                if (field.isAnnotationPresent(SerializedName.class)) {
                    fName = ((SerializedName)field.getAnnotation(SerializedName.class)).value();
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
                            Object[] objArray = (Object[])field.get(src);
                            for (Object obj : objArray) {
                                list.add(obj);
                            }
                        } else {
                            list = ((List)field.get(src));
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
                                        array.add(getPrimitiveValue(idField.getType(), idField.get(o)));

                                        extractObject(o, rootKeys);
                                    } catch (NoSuchFieldException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            element = array;
                        }

                    } else if (clazz.getName().equals("java.util.Date")) {
                        Date dValue = (Date)field.get(src);
                        if (dValue != null) {
                            String dateStr = buildIso8601Format().format(dValue);
                            element = new JsonPrimitive(dateStr);
                        }

                    } else if ((!clazz.equals(Object.class)) && field.get(src) != null && hasField(field.get(src).getClass(), "id")) {
                        element = new JsonPrimitive(getId(field.get(src)));
                        extractObject(field.get(src), rootKeys);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                if (element != null) {
                    rootObject.add(fName, element);
                }


                String className = src.getClass().getSimpleName();
                if (src.getClass().isAnnotationPresent(SerializedClassName.class)) {
                    className = src.getClass().getAnnotation(SerializedClassName.class).value();
                }

                if (rootKeys.get(className) == null) {
                    Hashtable<String, JsonObject> objects = new Hashtable<>();
                    rootKeys.put(className, objects);
                }

                if (getId(src) != null && rootKeys.get(className).get(getId(src)) == null ) {
                    rootKeys.get(className).put(getId(src), rootObject);
                }

                //System.out.println("\tField has Expose annotation: " + field.getName() + " field name: " + fName + " isPrimitive: " + isPrimitive + " isWrapperType: " + isWrapperType + " type: " + type + " element: " + element);
            }
        }
    }

    /**
     * Checks is a class is one of the primitive types and returns true||false depending
     * @param clazz
     * @return
     */
    private boolean isPrimitive(Class clazz) {
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
    private JsonPrimitive getPrimitiveValue(Class clazz, Object value) throws IllegalAccessException {
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
    private boolean hasField(Class clazz, String fieldName) {
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
    private static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }

    private String decapitalize(String input) {
            return Introspector.decapitalize(input);
    }

    /**
     * Very simple implementation of singular and plural conversion
     * @param singular
     * @return
     */
    private String getPluralFor(String singular) {
        String plural =  null;

        if (plural == null) {
            plural = singular;

            if (!plural.endsWith("s")) {
                plural = plural + "s";
            }
        }

        return plural;
    }

    /**
     * Very simple implementation of singular and plural conversion
     * @param plural
     * @return
     */
    private String getSingularFor(String plural) {
        String singular = null;

        if (singular == null) {
            singular = plural;

            if (singular.endsWith("s")) {
                singular = singular.substring(0, singular.length()-1);
            }
        }

        return singular;
    }
}
