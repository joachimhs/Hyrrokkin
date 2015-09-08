package no.haagensoftware.hyrrokkin.deserializer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import junit.framework.Assert;
import no.haagensoftware.hyrrokkin.serializer.JsonApiSerializer;
import no.haagensoftware.hyrrokkin.testmodels.SmsMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Created by jhsmbp on 08/09/15.
 */
public class JsonApiDeserializerTest {
    JsonApiDeserializer deserializer;

    @Before
    public void setup() {
        deserializer = new JsonApiDeserializer();
        deserializer.addPluralization("sms", "smses");
    }

    @Test
    public void deserializeSimpleObject() {
        String json = "{\"data\":{\"attributes\":{\"from\":\"004741415805\",\"text\":\"This is a text message containing less than 160 characters, and is sent as a single SMS.\"},\"type\":\"sms\",\"id\":\"testMessage1\"}}";

        SmsMessage parsedMessage = deserializer.deserialize(json, SmsMessage.class);

        Assert.assertNotNull(parsedMessage);
        Assert.assertEquals("004741415805", parsedMessage.getFrom());
        Assert.assertEquals("This is a text message containing less than 160 characters, and is sent as a single SMS.", parsedMessage.getText());
        Assert.assertEquals("testMessage1", parsedMessage.getId());

    }

    @Test
    public void deserializeObjectWithArrayRelationship() {
        String json = "{\"data\":{\"attributes\":{\"from\":\"004741415805\",\"text\":\"This is a text message containing less than 160 characters, and is sent as a single SMS.\"},\"type\":\"sms\",\"id\":\"testMessage1\",\"relationships\":{\"recipients\":[{\"id\":\"004712341234\",\"type\":\"recipient\"},{\"id\":\"The Office\",\"type\":\"recipient\"}]}},\"included\":[{\"id\":\"004712341234\",\"phoneNumber\":\"004712341234\",\"type\":\"recipient\"},{\"id\":\"The Office\",\"phoneNumber\":\"004798765432\",\"type\":\"recipient\"}]}";

        SmsMessage parsedMessage = deserializer.deserialize(json, SmsMessage.class);

        Assert.assertNotNull(parsedMessage);
        Assert.assertEquals("004741415805", parsedMessage.getFrom());
        Assert.assertEquals("This is a text message containing less than 160 characters, and is sent as a single SMS.", parsedMessage.getText());
        Assert.assertEquals("testMessage1", parsedMessage.getId());
        Assert.assertNotNull(parsedMessage.getRecipients());
        Assert.assertEquals(new Integer(2), new Integer(parsedMessage.getRecipients().size()));

        Assert.assertEquals("004712341234", parsedMessage.getRecipients().get(0).getId());
        Assert.assertEquals("004712341234", parsedMessage.getRecipients().get(0).getPhoneNumber());

        Assert.assertEquals("The Office", parsedMessage.getRecipients().get(1).getId());
        Assert.assertEquals("004798765432", parsedMessage.getRecipients().get(1).getPhoneNumber());
    }

    @Test
    public void deserializeObjectWithObjectRelationship() {
        String json = "{\"data\":{\"attributes\":{\"from\":\"004741415805\",\"text\":\"This is a text message containing less than 160 characters, and is sent as a single SMS.\"},\"type\":\"sms\",\"id\":\"testMessage1\",\"relationships\":{\"user\":{\"id\":\"jhs\",\"type\":\"user\"}}},\"included\":[{\"id\":\"jhs\",\"epost\":\"jhs@mail.com\",\"type\":\"user\"}]}\n";

        SmsMessage parsedMessage = deserializer.deserialize(json, SmsMessage.class);

        Assert.assertNotNull(parsedMessage);
        Assert.assertEquals("004741415805", parsedMessage.getFrom());
        Assert.assertEquals("This is a text message containing less than 160 characters, and is sent as a single SMS.", parsedMessage.getText());
        Assert.assertEquals("testMessage1", parsedMessage.getId());
        Assert.assertNotNull(parsedMessage.getUser());

        Assert.assertEquals("jhs", parsedMessage.getUser().getId());
        Assert.assertEquals("jhs@mail.com", parsedMessage.getUser().getEpost());
    }


}
