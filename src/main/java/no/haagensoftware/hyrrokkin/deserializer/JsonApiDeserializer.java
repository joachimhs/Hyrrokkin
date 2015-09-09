package no.haagensoftware.hyrrokkin.deserializer;

import com.google.gson.*;
import no.haagensoftware.hyrrokkin.base.HyrrokkinDeserializer;

import java.util.*;

/**
 * Created by jhsmbp on 08/09/15.
 */
public class JsonApiDeserializer extends HyrrokkinDeserializer {
    public JsonApiDeserializer() {
        super();
    }

    public JsonApiDeserializer(Map<String, String> pluralMap) {
        super(pluralMap);
    }

    //public <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
    public <T> T  deserialize(String json, Class<T> classOfT) {
        json = json.replaceAll("\\r", "");
        json = json.replaceAll("\\t", "");

        //only the properties that are marked with @Expose should be extracted from the JSON.
        //The map below will also tell if the relationships should be "array" or "object"
        Map<String, String> relationshipTypes = extractValidRelationshipTypesFromInputClass(classOfT);

        JsonObject deserializedObject = new JsonObject();

        JsonElement parsedElement = new JsonParser().parse(json); //Holds the parsed Json as Gson objects
        Map<String, List<JsonElement>> relationshipsByType = new HashMap<>(); //Holds any relationships, grouped by type for the data-object

        if (parsedElement.isJsonObject() && parsedElement.getAsJsonObject().has("data")) {
            JsonElement dataElement = parsedElement.getAsJsonObject().get("data");
            deserializedObject.add("id", dataElement.getAsJsonObject().get("id"));
            deserializedObject.add("type", dataElement.getAsJsonObject().get("type"));

            JsonElement attributesElement = dataElement.getAsJsonObject().get("attributes");
            if (attributesElement != null && attributesElement.isJsonObject()) {
                for (Map.Entry<String, JsonElement> attributeEntry : attributesElement.getAsJsonObject().entrySet()) {
                    if (relationshipTypes.get(getSingularFor(attributeEntry.getKey())) != null && relationshipTypes.get(getSingularFor(attributeEntry.getKey())).equals("primitive")) {
                        deserializedObject.add(attributeEntry.getKey(), attributeEntry.getValue());
                    }
                }
            }
        }

        if (parsedElement.isJsonObject() && parsedElement.getAsJsonObject().has("included") && parsedElement.getAsJsonObject().get("included").isJsonArray()) {
            JsonArray includedArray = parsedElement.getAsJsonObject().get("included").getAsJsonArray();

            //Group all included objects by type
            for (JsonElement includedElement : includedArray) {
                if (includedElement.isJsonObject() && includedElement.getAsJsonObject().has("type")) {
                    String type = includedElement.getAsJsonObject().get("type").getAsString();

                    if (relationshipsByType.get(type) == null) {
                        relationshipsByType.put(type, new ArrayList<JsonElement>());
                    }

                    relationshipsByType.get(type).add(includedElement);
                }
            }

            for (String key : relationshipsByType.keySet()) {
                JsonArray objectsForKey = new JsonArray();
                for (JsonElement element : relationshipsByType.get(key)) {
                    objectsForKey.add(element);
                }

                if (objectsForKey.size() > 1) {
                    deserializedObject.add(getPluralFor(key), objectsForKey);
                } else if (objectsForKey.size() == 1 && relationshipTypes.get(getSingularFor(key)) != null && relationshipTypes.get(getSingularFor(key)).equals("array")) {
                    //only one object in JSON, but Class requires a list
                    deserializedObject.add(key, objectsForKey);
                } else if (objectsForKey.size() == 1 && relationshipTypes.get(getSingularFor(key)) != null && relationshipTypes.get(getSingularFor(key)).equals("object")) {
                    //Only one object in JSON, and class requires OBJECT
                    deserializedObject.add(key, objectsForKey.get(0));
                }
            }
        }

        return new Gson().fromJson(deserializedObject, classOfT);
    }
}