package no.haagensoftware.hyrrokkin.testmodels;

import com.google.gson.annotations.Expose;

/**
 * Created by jhsmbp on 08/09/15.
 */
public class User {
    @Expose private String id;
    @Expose private String epost;

    public User() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEpost() {
        return epost;
    }

    public void setEpost(String epost) {
        this.epost = epost;
    }
}
