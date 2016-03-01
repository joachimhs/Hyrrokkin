package no.haagensoftware.hyrrokkin.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import no.haagensoftware.hyrrokkin.base.HyrrokkinSerializer;

import java.util.*;

/**
 * Created by jhsmbp on 07/09/15.
 */
public class JsonApiSerializer extends HyrrokkinSerializer {


    /**
     * This is the only public method, and will serialize any input POJO
     * @param src
     * @return
     */
    @Override
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
                extractObject(obj, rootKeys, null, false);
            }
        } else {
            String className = getRootKeyForClass(src);

            inputObjectRootKey = className;
            extractObject(src, rootKeys, null, false);
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
        JsonObject topObject = new JsonObject(); //Holds the very top JSON object
        JsonObject dataObject = new JsonObject(); //Holds the data-object representing the main payload
        JsonArray sideLoadedArray = new JsonArray(); //Holds any side-loaded data

        Map<String, List<JsonElement>> relationshipsByType = new HashMap<>(); //Holds any relationships, grouped by type for the data-object

        topObject.add("data", dataObject); //Add the data-object to the top-level object

        for (String key : rootKeys.keySet()) {
            if (key.equals(inputObjectRootKey)) {
                //This is the "data" payload

                JsonObject attributesObject = new JsonObject();
                dataObject.add("attributes", attributesObject);

                dataObject.addProperty("type", key);
                //get all fields
                for (String objKey : rootKeys.get(key).keySet()) {
                    JsonObject valueObject = rootKeys.get(key).get(objKey);

                    for (Map.Entry<String, JsonElement> valueObjectField : valueObject.entrySet()) {
                        if (valueObjectField.getKey().equals("id") || valueObjectField.getKey().equals("type")) {
                            dataObject.add(decapitalize(getSingularFor(valueObjectField.getKey())), valueObjectField.getValue());
                        } else if (valueObjectField.getValue().isJsonArray()) {
                            for (JsonElement relElem : valueObjectField.getValue().getAsJsonArray()) {
                                String type = relElem.getAsJsonObject().get("type").getAsString();

                                if ( relationshipsByType.get(type) == null) {
                                    relationshipsByType.put(type, new ArrayList<JsonElement>());
                                }

                                relationshipsByType.get(type).add(relElem);
                            }
                        } else if (valueObjectField.getValue().isJsonObject()) {
                            JsonObject relElem = valueObjectField.getValue().getAsJsonObject();

                            String type = relElem.getAsJsonObject().get("type").getAsString();

                            if ( relationshipsByType.get(type) == null) {
                                relationshipsByType.put(type, new ArrayList<JsonElement>());
                            }

                            relationshipsByType.get(type).add(relElem);
                        } else {
                            attributesObject.add(decapitalize(getSingularFor(valueObjectField.getKey())), valueObjectField.getValue());
                        }
                    }
                }
            } else {
                //The remaining objects go in the "included" object for side-loading

                for (String objKey : rootKeys.get(key).keySet()) {
                    JsonObject sideLoadedObject = rootKeys.get(key).get(objKey);
                    sideLoadedObject.addProperty("type", decapitalize(key));
                    sideLoadedArray.add(sideLoadedObject);
                }
            }
        }

        //If the data-object have relationships, add them by type to the relationshipObject
        if (relationshipsByType.size() > 0) {
            JsonObject relationshipObject = new JsonObject();

            for (String relType : relationshipsByType.keySet()) {
                List<JsonElement> rels = relationshipsByType.get(relType);
                if (rels.size() == 1) {
                    relationshipObject.add(decapitalize(relType), rels.get(0));
                } else if (rels.size() > 1) {
                    JsonArray relsArray = new JsonArray();
                    for (JsonElement relsElem : rels) {
                        relsArray.add(relsElem);
                    }

                    relationshipObject.add(getPluralFor(decapitalize(relType)), relsArray);
                }
            }

            dataObject.add("relationships", relationshipObject);
        }

        if (sideLoadedArray.size() > 0) {
            topObject.add("included", sideLoadedArray);
        }

        return topObject;
    }
}
