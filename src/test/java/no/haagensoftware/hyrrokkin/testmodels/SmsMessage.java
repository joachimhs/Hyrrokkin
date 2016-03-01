package no.haagensoftware.hyrrokkin.testmodels;

import com.google.gson.annotations.Expose;
import no.haagensoftware.hyrrokkin.annotations.SerializedClassName;
import no.haagensoftware.hyrrokkin.testmodels.SmsRecipient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jhsmbp on 07/09/15.
 */
@SerializedClassName("sms")
public class SmsMessage {
    @Expose private String id;
    @Expose private String from;
    @Expose private String text;
    @Expose private Date schedule;
    @Expose private Integer validity;
    @Expose private Boolean allowMultipleMessages;
    @Expose private String splitFormat;

    @Expose private String sentStatus;
    @Expose private String errorCode;

    @Expose private List<SmsRecipient> recipients;
    @Expose private List<SmsReceipt> smsReceipts;

    @Expose private User user;

    public SmsMessage() {
        recipients = new ArrayList<>();
        smsReceipts = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getSchedule() {
        return schedule;
    }

    public void setSchedule(Date schedule) {
        this.schedule = schedule;
    }

    public Integer getValidity() {
        return validity;
    }

    public void setValidity(Integer validity) {
        this.validity = validity;
    }

    public Boolean getAllowMultipleMessages() {
        return allowMultipleMessages;
    }

    public void setAllowMultipleMessages(Boolean allowMultipleMessages) {
        this.allowMultipleMessages = allowMultipleMessages;
    }

    public String getSplitFormat() {
        return splitFormat;
    }

    public void setSplitFormat(String splitFormat) {
        this.splitFormat = splitFormat;
    }

    public List<SmsRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<SmsRecipient> recipients) {
        this.recipients = recipients;
    }

    public String getSentStatus() {
        return sentStatus;
    }

    public void setSentStatus(String sentStatus) {
        this.sentStatus = sentStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public List<SmsReceipt> getSmsReceipts() {
        return smsReceipts;
    }

    public void setSmsReceipts(List<SmsReceipt> smsReceipts) {
        this.smsReceipts = smsReceipts;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
