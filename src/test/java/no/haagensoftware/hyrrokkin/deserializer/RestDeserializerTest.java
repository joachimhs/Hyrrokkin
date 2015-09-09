package no.haagensoftware.hyrrokkin.deserializer;

import junit.framework.Assert;
import no.haagensoftware.hyrrokkin.testmodels.SmsMessage;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by jhsmbp on 08/09/15.
 */
public class RestDeserializerTest {
    RestDeserializer deserializer;

    @Before
    public void setup() {
        deserializer = new RestDeserializer();
        deserializer.addPluralization("sms", "smses");
    }

    @Test
    public void deserializeSimpleObject() {
        String json = "{\"sms\": {\"from\": \"004712345678\",\"text\": \"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas dolor neque, egestas vitae vestibulum et, consectetur quis sapien.\"}}";

        SmsMessage parsedMessage = deserializer.deserialize(json, SmsMessage.class);

        Assert.assertNotNull(parsedMessage);
        Assert.assertEquals("004712345678", parsedMessage.getFrom());
        Assert.assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas dolor neque, egestas vitae vestibulum et, consectetur quis sapien.", parsedMessage.getText());

    }

    @Test
    public void deserializeObjectWithArrayRelationship() {
        String json = "{\"sms\": {\"from\": \"004712345678\",\"text\": \"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas dolor neque, egestas vitae vestibulum et, consectetur quis sapien.\",\"recipients\": [{\"type\": \"recipients\",\"id\": \"+4787654321\",\"phoneNumber\": \"004787654321\"}]}}";

        SmsMessage parsedMessage = deserializer.deserialize(json, SmsMessage.class);

        Assert.assertNotNull(parsedMessage);
        Assert.assertEquals("004712345678", parsedMessage.getFrom());
        Assert.assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas dolor neque, egestas vitae vestibulum et, consectetur quis sapien.", parsedMessage.getText());
        Assert.assertNotNull(parsedMessage.getRecipients());
        Assert.assertEquals(new Integer(1), new Integer(parsedMessage.getRecipients().size()));

        Assert.assertEquals("+4787654321", parsedMessage.getRecipients().get(0).getId());
        Assert.assertEquals("004787654321", parsedMessage.getRecipients().get(0).getPhoneNumber());
    }

    @Test
    public void deserializeObjectWithObjectRelationship() {
        String json = "{\"sms\": {\"from\": \"004712345678\",\"text\": \"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas dolor neque, egestas vitae vestibulum et, consectetur quis sapien.\",\"user\": \"jhs\"},\"user\": {\"id\": \"jhs\",\"epost\": \"jhs@mail.com\",\"type\": \"user\"}}";

        SmsMessage parsedMessage = deserializer.deserialize(json, SmsMessage.class);

        Assert.assertNotNull(parsedMessage);
        Assert.assertEquals("004712345678", parsedMessage.getFrom());
        Assert.assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas dolor neque, egestas vitae vestibulum et, consectetur quis sapien.", parsedMessage.getText());
        Assert.assertNotNull(parsedMessage.getUser());

        Assert.assertEquals("jhs", parsedMessage.getUser().getId());
        Assert.assertEquals("jhs@mail.com", parsedMessage.getUser().getEpost());
    }


}
