package no.haagensoftware.hyrrokkin.serializer;

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
 * Created by jhsmbp on 08/12/14.
 */
public class RestSerializer  {

    public JsonElement serialize(Object src) {
        Hashtable<String, Hashtable<String, JsonObject>> rootKeys = new Hashtable<>();

        if (List.class.isAssignableFrom(src.getClass())) {
            for (Object obj : ((List)src)) {
                extractObject(obj, rootKeys);
            }
        } else {
            extractObject(src, rootKeys);
        }

        JsonObject topObject = generateJson(rootKeys);

        return topObject;
    }

    private JsonObject generateJson(Hashtable<String, Hashtable<String, JsonObject>> rootKeys) {
        JsonObject topObject = new JsonObject();

        for (String key : rootKeys.keySet()) {
            //System.out.println(key);
            if (rootKeys.get(key).keySet().size() > 1) {
                JsonArray array = new JsonArray();
                for (String objKey : rootKeys.get(key).keySet()) {
                    array.add(rootKeys.get(key).get(objKey));
                    //System.out.println("\t" + objKey + ": " + rootKeys.get(key).get(objKey).toString());
                }
                topObject.add(getPluralFor(key), array);
            } else if (rootKeys.get(key).keySet().size() == 1) {
                for (String objKey : rootKeys.get(key).keySet()) {
                    topObject.add(getSingularFor(key), rootKeys.get(key).get(objKey));
                }
            }
        }

        return topObject;
    }

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
                                if (isPrimitive(o.getClass())) {
                                    array.add(getPrimitiveValue(o.getClass(), o));
                                } else if (hasField(o.getClass(), "id")) {
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

    private boolean isPrimitive(Class clazz) {
        return clazz.equals(String.class) ||
                clazz.equals(Double.TYPE) || clazz.getName().equals("java.lang.Double") ||
                clazz.equals(Integer.TYPE) || clazz.getName().equals("java.lang.Integer") ||
                clazz.equals(Boolean.TYPE) || clazz.getName().equals("java.lang.Boolean") ||
                clazz.equals(Long.TYPE) || clazz.getName().equals("java.lang.Long");

    }

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

    private static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }

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
