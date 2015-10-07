package no.haagensoftware.hyrrokkin.base;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import no.haagensoftware.hyrrokkin.annotations.SerializedClassName;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jhsmbp on 08/09/15.
 */
public class HyrrokkinSerializationBase {
    private Map<String, String> plurals;
    private Map<String, String> singulars;

    public HyrrokkinSerializationBase() {
        plurals = new LinkedHashMap<>();
        singulars = new LinkedHashMap<>();
    }

    public HyrrokkinSerializationBase(Map<String, String> pluralMap) {
        this();

        if (pluralMap != null && pluralMap.size() > 0) {
            for (String singular : pluralMap.keySet()) {
                singulars.put(pluralMap.get(singular), singular);
                plurals.put(singular, pluralMap.get(singular));

            }
        }
    }

    public void addPluralization(String singular, String plural) {
        singulars.put(plural, singular);
        plurals.put(singular, plural);
    }

    /**
     * Simple implementation of singular and plural conversion,
     * using a singular and plural map to convert
     * @param singular
     * @return
     */
    protected String getPluralFor(String singular) {
        String plural = plurals.get(singular);

        if (plural == null && singulars.get(singular) != null) {
            plural = singular;
        }

        if (plural == null) {
            plural = singular;

            if (!plural.endsWith("s")) {
                plural = plural + "s";
            }
        }

        return plural;
    }

    /**
     * Simple implementation of singular and plural conversion,
     * using a singular and plural map to convert
     * @param plural
     * @return
     */
    protected String getSingularFor(String plural) {

        //If there is a plural registered for the input plural, use the registered one
        if (plurals.containsKey(plural)) {
            plural = plurals.get(plural);
        }

        String singular = singulars.get(plural);

        if (singular == null) {
            singular = plural;

            if (singular.endsWith("s")) {
                singular = singular.substring(0, singular.length()-1);
            }
        }

        return singular;
    }

    protected String decapitalize(String input) {
        return Introspector.decapitalize(input);
    }

    /**
     * Gets the root key of this object. Either derived from the object name, or
     * from the @SerializedClassName annotation
     */
    protected String getRootKeyFromClass(Class clazz) {
        //Use Class name as rootKey by default
        String rootKey = getSingularFor(clazz.getName());

        //If object has SerializedClassName annotation, use that instead as rootKey
        if (clazz.isAnnotationPresent(SerializedClassName.class)) {
            rootKey = ((SerializedClassName)clazz.getDeclaredAnnotation(SerializedClassName.class)).value();
        }

        return rootKey;
    }

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

        if (object instanceof String) {
            id = (String)object;
        } else if (hasField(object.getClass(), "id")) {
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

    protected static Set<Class<?>> getWrapperTypes()
    {
        Set<Class<?>> ret = new HashSet<Class<?>>();
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        return ret;
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
}
