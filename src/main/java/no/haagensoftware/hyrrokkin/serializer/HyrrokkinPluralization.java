package no.haagensoftware.hyrrokkin.serializer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by jhsmbp on 08/09/15.
 */
public class HyrrokkinPluralization {
    private Map<String, String> plurals;
    private Map<String, String> singulars;

    public HyrrokkinPluralization() {
        plurals = new LinkedHashMap<>();
        singulars = new LinkedHashMap<>();
    }

    public HyrrokkinPluralization(Map<String, String> pluralMap) {
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
}
