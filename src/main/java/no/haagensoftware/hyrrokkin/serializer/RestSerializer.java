package no.haagensoftware.hyrrokkin.serializer;

import com.google.gson.*;
import no.haagensoftware.hyrrokkin.base.HyrrokkinSerializer;

import java.util.*;

/**
 * Created by jhsmbp on 08/12/14.
 */
public class RestSerializer extends HyrrokkinSerializer {

    public RestSerializer() {
        super();
    }

    public RestSerializer(Map<String, String> pluralMap) {
        super(pluralMap);
    }

    /**
     * This is the only public method, and will serialize any input POJO
     * @param src
     * @return
     */
    public JsonElement serialize(Object src, List<String> sideloadKeys, boolean embedded) {
        //These are used to determine if the root keys are objects or arrays
        boolean inputIsList = false;
        String inputObjectRootKey = null;

        //There is one hash table for the root keys, and one hash table for each object of a specific type
        //This will hinder duplicate objects in the return JSON
        Hashtable<String, Hashtable<String, JsonObject>> rootKeys = new Hashtable<>();

        if (List.class.isAssignableFrom(src.getClass())) {
            inputIsList = true;
            for (Object obj : ((List)src)) {
                extractObject(obj, rootKeys, sideloadKeys, embedded);
            }
        } else {
            String className = getRootKeyForClass(src);

            inputObjectRootKey = className;
            extractObject(src, rootKeys, sideloadKeys, embedded);
        }

        JsonObject topObject = null;
        if (embedded) {
            topObject = generateJson(rootKeys, inputIsList, inputObjectRootKey, true);
        } else {
            topObject = generateJson(rootKeys, inputIsList, inputObjectRootKey, false);
        }

        return topObject;
    }

    /**
     * This is the only public method, and will serialize any input POJO
     * @param src
     * @return
     * @deprecated
     */
    public JsonElement serialize(Object src, List<String> sideloadKeys) {
        return this.serialize(src, sideloadKeys, false);
    }
    /**
     * This is the only public method, and will serialize any input POJO
     * @param src
     * @return
     * @deprecated Use the serialize(Object src, Boolean sideload) instead
     */
    public JsonElement serialize(Object src) {
        return this.serialize(src, null);
    }

    /**
     * Generating JSON from the rootKeys hashtable
     * @param rootKeys
     * @return
     */
    private JsonObject generateJson(
            Hashtable<String, Hashtable<String, JsonObject>> rootKeys,
            boolean inputIsList,
            String inputObjectRootKey,
            boolean isEmbedded) {

        JsonObject topObject = new JsonObject();

        for (String key : rootKeys.keySet()) {
            boolean thisInputIsList = inputIsList;

            //System.out.println(key);
            //

            //Experimental. Forcing list if size is greater than one for the root key
            //if (rootKeys.get(key).keySet().size() > 1) {
            //    inputIsList = true;
            //}

            //Experimental: Forcin list if root key is not the main object
            if (!key.equals(inputObjectRootKey)) {
                thisInputIsList = true;
            } else if (rootKeys.get(key).keySet().size() > 1) {
                thisInputIsList = true;
            }

            if (key.equals(inputObjectRootKey) && !thisInputIsList) {
                JsonObject mainObject = new JsonObject();

                for (String objKey : rootKeys.get(key).keySet()) {
                    JsonObject payloadObject = rootKeys.get(key).get(objKey);


                    for (Map.Entry<String, JsonElement> field : payloadObject.entrySet()) {
                        if (field.getValue().isJsonObject()) {
                            mainObject.add(field.getKey(), field.getValue().getAsJsonObject().get("id"));
                        } else if (field.getValue().isJsonArray()) {
                            if (isEmbedded) {
                                mainObject.add(field.getKey(), field.getValue());
                            } else {
                                JsonArray idStrings = new JsonArray();
                                for (JsonElement idElement : field.getValue().getAsJsonArray()) {
                                    if (idElement.isJsonPrimitive()) {
                                        idStrings.add(idElement.getAsJsonPrimitive());
                                    } else {
                                        idStrings.add(idElement.getAsJsonObject().get("id"));
                                    }
                                }
                                mainObject.add(field.getKey(), idStrings);
                            }
                        } else {
                            mainObject.add(field.getKey(), field.getValue());
                        }
                    }

                }

                topObject.add(decapitalize(getSingularFor(key)), mainObject);
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
}
