import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSessionPreferences;
import org.w3c.dom.Document;


public class NetconfSessionPreferencesTest {

    @Test
    public void testHello() throws Exception {

        Document dco = Mockito.mock(Document.class, Mockito.CALLS_REAL_METHODS);
        NetconfMessage message = new NetconfMessage(dco);
        NetconfSessionPreferences pref = new NetconfSessionPreferences(message);
        Assert.assertEquals(message, pref.getHelloMessage());
    }
}
