package no.haagensoftware.hyrrokkin.testmodels;

import com.google.gson.annotations.Expose;

/**
 * Created by jhsmbp on 08/09/15.
 */
public class SmsReceipt {
    @Expose private String id;
    @Expose private String status;
    @Expose private Integer numberOfMessagesSent;
    @Expose private Integer numberOfCharactersSent;

    public SmsReceipt() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getNumberOfMessagesSent() {
        return numberOfMessagesSent;
    }

    public void setNumberOfMessagesSent(Integer numberOfMessagesSent) {
        this.numberOfMessagesSent = numberOfMessagesSent;
    }

    public Integer getNumberOfCharactersSent() {
        return numberOfCharactersSent;
    }

    public void setNumberOfCharactersSent(Integer numberOfCharactersSent) {
        this.numberOfCharactersSent = numberOfCharactersSent;
    }
}
