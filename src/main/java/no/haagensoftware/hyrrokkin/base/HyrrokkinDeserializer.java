package no.haagensoftware.hyrrokkin.base;

import com.google.gson.annotations.Expose;
import no.haagensoftware.hyrrokkin.annotations.SerializedClassName;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jhsmbp on 09/09/15.
 */
public abstract class HyrrokkinDeserializer extends HyrrokkinSerializationBase {

    public HyrrokkinDeserializer() {
        super();
    }

    public HyrrokkinDeserializer(Map<String, String> pluralMap) {
        super(pluralMap);
    }

    public abstract <T> T  deserialize(String json, Class<T> classOfT);

    protected <T> Map<String, String> extractValidRelationshipTypesFromInputClass(Class<T> classOfT) {
        Map<String, String> relationshipTypes = new HashMap<>();

        for (Field field : classOfT.getDeclaredFields()) {
            if (field.getDeclaredAnnotation(Expose.class) != null) {
                //this field is exposed, and should be included.
                if (List.class.isAssignableFrom(field.getType())) {
                    //Use the class name as the relationship name
                    String relationshipName = getSingularFor(decapitalize(field.getName()));
                    String relationshipType = "array";

                    ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
                    Class<?> stringListClass = (Class<?>) stringListType.getActualTypeArguments()[0];
                    if (stringListClass.getDeclaredAnnotation(SerializedClassName.class) != null) {
                        //this field has a different serialized name than its qualified class name
                        relationshipName = ((SerializedClassName)stringListClass.getDeclaredAnnotation(SerializedClassName.class)).value();
                        relationshipName = getSingularFor(relationshipName);
                        relationshipType = "array";
                    }

                    relationshipTypes.put(relationshipName, relationshipType);
                } else if (!(isPrimitive(field.getType()) || field.getType().getName().equals("java.util.Date"))) {
                    String relationshipName = getSingularFor(decapitalize(field.getName()));
                    String relationshipType = "object";

                    relationshipTypes.put(relationshipName, relationshipType);
                } else {
                    String relationshipName = getSingularFor(decapitalize(field.getName()));
                    String relationshipType = "primitive";

                    relationshipTypes.put(relationshipName, relationshipType);
                }

            }
        }
        return relationshipTypes;
    }
}
