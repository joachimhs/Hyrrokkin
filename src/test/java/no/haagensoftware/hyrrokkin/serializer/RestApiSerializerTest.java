package no.haagensoftware.hyrrokkin.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import junit.framework.Assert;
import no.haagensoftware.hyrrokkin.annotations.SerializeException;
import no.haagensoftware.hyrrokkin.testmodels.SmsMessage;
import no.haagensoftware.hyrrokkin.testmodels.SmsReceipt;
import no.haagensoftware.hyrrokkin.testmodels.SmsRecipient;
import no.haagensoftware.hyrrokkin.testmodels.User;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by jhsmbp on 07/09/15.
 */
public class RestApiSerializerTest {
    private SmsMessage testMessage;
    RestSerializer serializer;

    @Before
    public void setup() {
        testMessage = new SmsMessage();
        testMessage.setId("testMessage1");
        testMessage.setFrom("004741415805");
        testMessage.setText("This is a text message containing less than 160 characters, and is sent as a single SMS.");

        serializer = new RestSerializer();
        serializer.addPluralization("sms", "smses");
    }

    @Test
    @Ignore
    public void testSimpleObject() {
        System.out.println(serializer.serialize(testMessage).toString());
        Assert.assertEquals(buildUpSimpleObject().toString(), serializer.serialize(testMessage).toString());
    }

    @Test(expected = SerializeException.class)
    public void testNoIdWillThrowSerializeException() {
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setFrom("004741415805");
        smsMessage.setText("This is a text message containing less than 160 characters, and is sent as a single SMS.");

        serializer.serialize(smsMessage);
    }

    @Test
    public void testObjectWithListRelationship() {
        SmsRecipient recipient1 = new SmsRecipient("004712341234", "004712341234");
        SmsRecipient recipient2 = new SmsRecipient("The Office", "004798765432");
        SmsReceipt receipt1 = new SmsReceipt("receiptid1", "status1", 160, 160);
        SmsReceipt receipt2 = new SmsReceipt("receiptid2", "status2", 160, 160);

        testMessage.getRecipients().add(recipient1);
        testMessage.getRecipients().add(recipient2);
        testMessage.getSmsReceipts().add(receipt1);
        testMessage.getSmsReceipts().add(receipt2);

        String expected = buildUpExpectedWithListRelationships().toString();
        String serialized = serializer.serialize(testMessage, Arrays.asList("recipient", "smsReceipt"), false).toString();

        System.out.println(expected);
        System.out.println(serialized);

        Assert.assertEquals(expected, serialized);
    }

    private JsonObject buildUpSimpleObject() {
        JsonObject expected = new JsonObject();
        JsonObject data = new JsonObject();

        JsonObject attributes = new JsonObject();
        attributes.addProperty("from", "004741415805");
        attributes.addProperty("text", "This is a text message containing less than 160 characters, and is sent as a single SMS.");

        data.add("attributes", attributes);

        data.addProperty("type", "sms");
        data.addProperty("id", "testMessage1");

        expected.add("data", data);

        return expected;
    }

    private JsonObject buildUpExpectedWithListRelationships() {
        JsonObject expected = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("id", "testMessage1");

        data.addProperty("from", "004741415805");
        data.addProperty("text", "This is a text message containing less than 160 characters, and is sent as a single SMS.");

        JsonArray recipientsRelArray = new JsonArray();
        recipientsRelArray.add(new JsonPrimitive("004712341234"));
        recipientsRelArray.add(new JsonPrimitive("The Office"));

        data.add("recipients", recipientsRelArray);


        JsonArray includedArray = new JsonArray();
        includedArray.add(new JsonPrimitive("receiptid1"));
        includedArray.add(new JsonPrimitive("receiptid2"));
        data.add("smsReceipts", includedArray);

        JsonObject recipient1 = new JsonObject();
        recipient1.addProperty("id", "receiptid2");
        recipient1.addProperty("status", "status2");
        recipient1.addProperty("numberOfMessagesSent", 160);
        recipient1.addProperty("numberOfCharactersSent", 160);

        JsonObject recipient2 = new JsonObject();
        recipient2.addProperty("id", "receiptid1");
        recipient2.addProperty("status", "status1");
        recipient2.addProperty("numberOfMessagesSent", 160);
        recipient2.addProperty("numberOfCharactersSent", 160);

        JsonArray smsReceipts = new JsonArray();
        smsReceipts.add(recipient1);
        smsReceipts.add(recipient2);



        JsonObject recipientRel1 = new JsonObject();
        recipientRel1.addProperty("id", "004712341234");
        recipientRel1.addProperty("phoneNumber", "004712341234");

        JsonObject recipientRel2 = new JsonObject();
        recipientRel2.addProperty("id", "The Office");
        recipientRel2.addProperty("phoneNumber", "004798765432");

        JsonArray recipients = new JsonArray();
        recipients.add(recipientRel1);
        recipients.add(recipientRel2);

        expected.add("sms", data);
        expected.add("smsReceipts", smsReceipts);
        expected.add("recipients", recipients);
        return expected;
    }

    private JsonObject buildUpExpectedWithObjectRelationship() {
        JsonObject expected = new JsonObject();
        JsonObject data = new JsonObject();

        JsonObject attributes = new JsonObject();
        attributes.addProperty("from", "004741415805");
        attributes.addProperty("text", "This is a text message containing less than 160 characters, and is sent as a single SMS.");

        JsonObject relationships = new JsonObject();
        JsonObject userRelationship = new JsonObject();
        userRelationship.addProperty("id", "jhs");
        userRelationship.addProperty("type", "user");
        relationships.add("user", userRelationship);

        data.add("attributes", attributes);

        data.addProperty("type", "sms");
        data.addProperty("id", "testMessage1");

        data.add("relationships", relationships);

        JsonArray includedArray = new JsonArray();
        JsonObject jhsUser = new JsonObject();
        jhsUser.addProperty("id", "jhs");
        jhsUser.addProperty("epost", "jhs@mail.com");
        jhsUser.addProperty("type", "user");

        includedArray.add(jhsUser);

        expected.add("data", data);
        expected.add("included", includedArray);
        return expected;
    }
}
