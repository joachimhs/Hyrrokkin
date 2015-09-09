package no.haagensoftware.hyrrokkin.deserializer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import no.haagensoftware.hyrrokkin.base.HyrrokkinDeserializer;

import java.util.Map;

/**
 * Created by jhsmbp on 05/04/15.
 */
public class RestDeserializer extends HyrrokkinDeserializer {

    public RestDeserializer() {
    }

    public RestDeserializer(Map<String, String> pluralMap) {
        super(pluralMap);
    }

    @Override
    public <T> T deserialize(String json, Class<T> classOfT) {
        json = json.replaceAll("\\r", "");
        json = json.replaceAll("\\t", "");

        JsonElement parsedElement = new JsonParser().parse(json); //Holds the parsed Json as Gson objects

        //only the properties that are marked with @Expose should be extracted from the JSON.
        //The map below will also tell if the relationships should be "array" or "object"
        Map<String, String> relationshipTypes = extractValidRelationshipTypesFromInputClass(classOfT);

        JsonObject deserializedObject = new JsonObject();

        String rootKey = getRootKeyFromClass(classOfT);

        if (parsedElement.isJsonObject() && parsedElement.getAsJsonObject().has(rootKey)) {
            //A single main data-object has been passed in
            JsonElement dataElement = parsedElement.getAsJsonObject().get(rootKey);

            for (Map.Entry<String, JsonElement> fieldEntry : dataElement.getAsJsonObject().entrySet()) {
                if (relationshipTypes.get(getSingularFor(fieldEntry.getKey())) != null &&
                        relationshipTypes.get(getSingularFor(fieldEntry.getKey())).equals("primitive")) {
                    //Extract all primitive types as-is
                    deserializedObject.add(fieldEntry.getKey(), fieldEntry.getValue());
                } //Arrays and Objects will be extracted below
            }

            for (String key : relationshipTypes.keySet()) {
                if (relationshipTypes.get(key).equals("array") &&
                        dataElement.getAsJsonObject().has(getPluralFor(key)) &&
                        dataElement.getAsJsonObject().get(getPluralFor(key)).isJsonArray()) {

                    //Extract the array as-is as a field on the deserialized object
                    deserializedObject.add(getPluralFor(key), dataElement.getAsJsonObject().get(getPluralFor(key)));
                } else if (relationshipTypes.get(key).equals("object") &&
                        parsedElement.getAsJsonObject().has(key) &&
                        parsedElement.getAsJsonObject().get(key).isJsonObject()) {

                    //Extract the object as-is as a field on the deserialized object
                    deserializedObject.add(key, parsedElement.getAsJsonObject().get(key));
                }
            }

        } else if (parsedElement.isJsonObject() && parsedElement.getAsJsonObject().has(getPluralFor(rootKey))) {
            //A list of data-objects have been passed in
            //TODO: NOT YET SUPPORTED
        }

        return new Gson().fromJson(deserializedObject, classOfT);
    }
}
