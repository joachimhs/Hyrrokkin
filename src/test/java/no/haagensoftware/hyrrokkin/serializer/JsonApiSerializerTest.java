package no.haagensoftware.hyrrokkin.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import junit.framework.Assert;
import no.haagensoftware.hyrrokkin.testmodels.SmsMessage;
import no.haagensoftware.hyrrokkin.testmodels.SmsRecipient;
import no.haagensoftware.hyrrokkin.testmodels.User;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by jhsmbp on 07/09/15.
 */
public class JsonApiSerializerTest {
    private SmsMessage testMessage;
    JsonApiSerializer serializer;

    @Before
    public void setup() {
        testMessage = new SmsMessage();
        testMessage.setId("testMessage1");
        testMessage.setFrom("004741415805");
        testMessage.setText("This is a text message containing less than 160 characters, and is sent as a single SMS.");

        serializer = new JsonApiSerializer();
        serializer.addPluralization("sms", "smses");
    }

    @Test
    public void testSimpleObject() {
        System.out.println(serializer.serialize(testMessage).toString());
        Assert.assertEquals(buildUpSimpleObject().toString(), serializer.serialize(testMessage).toString());
    }

    @Test
    public void testObjectWithListRelationship() {
        SmsRecipient recipient1 = new SmsRecipient("004712341234", "004712341234");
        SmsRecipient recipient2 = new SmsRecipient("The Office", "004798765432");

        testMessage.getRecipients().add(recipient1);
        testMessage.getRecipients().add(recipient2);

        System.out.println(serializer.serialize(testMessage).toString());
        Assert.assertEquals(buildUpExpectedWithListRelationships().toString(), serializer.serialize(testMessage).toString());
    }

    @Test
    public void testObjectWithObjectRelationship() {
        User user = new User();
        user.setId("jhs");
        user.setEpost("jhs@mail.com");

        testMessage.setUser(user);

        JsonObject expected = buildUpExpectedWithObjectRelationship();

        System.out.println(serializer.serialize(testMessage).toString());

        Assert.assertEquals(expected.toString(), serializer.serialize(testMessage).toString());
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

        JsonObject attributes = new JsonObject();
        attributes.addProperty("from", "004741415805");
        attributes.addProperty("text", "This is a text message containing less than 160 characters, and is sent as a single SMS.");

        JsonObject relationships = new JsonObject();

        JsonObject recipientRel1 = new JsonObject();
        recipientRel1.addProperty("id", "004712341234");
        recipientRel1.addProperty("type", "recipient");

        JsonObject recipientRel2 = new JsonObject();
        recipientRel2.addProperty("id", "The Office");
        recipientRel2.addProperty("type", "recipient");

        JsonArray recipientsRelArray = new JsonArray();
        recipientsRelArray.add(recipientRel1);
        recipientsRelArray.add(recipientRel2);

        relationships.add("recipients", recipientsRelArray);

        data.add("attributes", attributes);

        data.addProperty("type", "sms");
        data.addProperty("id", "testMessage1");

        data.add("relationships", relationships);

        JsonArray includedArray = new JsonArray();
        JsonObject recipient1 = new JsonObject();
        recipient1.addProperty("id", "004712341234");
        recipient1.addProperty("phoneNumber", "004712341234");
        recipient1.addProperty("type", "recipient");

        JsonObject recipient2 = new JsonObject();
        recipient2.addProperty("id", "The Office");
        recipient2.addProperty("phoneNumber", "004798765432");
        recipient2.addProperty("type", "recipient");

        includedArray.add(recipient1);
        includedArray.add(recipient2);

        expected.add("data", data);
        expected.add("included", includedArray);
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
