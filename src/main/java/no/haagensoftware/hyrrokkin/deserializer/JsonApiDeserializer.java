package no.haagensoftware.hyrrokkin.deserializer;

import com.google.gson.*;
import no.haagensoftware.hyrrokkin.serializer.HyrrokkinPluralization;

import java.util.*;

/**
 * Created by jhsmbp on 08/09/15.
 */
public class JsonApiDeserializer extends HyrrokkinPluralization {
    public JsonApiDeserializer() {
        super();
    }

    public JsonApiDeserializer(Map<String, String> pluralMap) {
        super(pluralMap);
    }

    //public <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
    public <T> T  deserialize(String json, Class<T> classOfT) {
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
                    deserializedObject.add(attributeEntry.getKey(), attributeEntry.getValue());
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
                } else if (objectsForKey.size() == 1) {
                    deserializedObject.add(key, objectsForKey.get(0));
                }

            }

        }

        return new Gson().fromJson(deserializedObject, classOfT);
    }
}

/*
{
    "data": {
        "attributes": {
            "from": "004741415805",
            "text": "This is a text message containing less than 160 characters, and is sent as a single SMS."
        },
        "type": "sms",
        "id": "testMessage1",
        "relationships": {
            "user": {
                "id": "jhs",
                "type": "user"
            }
        }
    },
    "included": [
        {
            "id": "jhs",
            "epost": "jhs@mail.com",
            "type": "user"
        }
    ]
}
 */
