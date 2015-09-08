package no.haagensoftware.hyrrokkin.testmodels;

import com.google.gson.annotations.Expose;
import no.haagensoftware.hyrrokkin.annotations.SerializedClassName;

/**
 * Created by jhsmbp on 07/09/15.
 */
@SerializedClassName("recipient")
public class SmsRecipient {
    @Expose private String id;
    @Expose private String phoneNumber;

    public SmsRecipient() {
    }

    public SmsRecipient(String id, String phoneNumber) {
        this.id = id;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
