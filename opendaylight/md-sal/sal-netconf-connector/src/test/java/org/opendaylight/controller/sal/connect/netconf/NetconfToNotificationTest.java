package org.opendaylight.controller.sal.connect.netconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Iterables;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.notifications.NetconfNotification;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.w3c.dom.Document;

/**
 * @author Lukas Sedlak <lsedlak@cisco.com>
 */
public class NetconfToNotificationTest {

    NetconfMessageTransformer messageTransformer;

    NetconfMessage userNotification;

    @SuppressWarnings("deprecation")
    @Before
    public void setup() throws Exception {
        final SchemaContext schemaContext = getNotificationSchemaContext(getClass());

        messageTransformer = new NetconfMessageTransformer(schemaContext, true);

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        InputStream notifyPayloadStream = getClass().getResourceAsStream("/notification-payload.xml");
        assertNotNull(notifyPayloadStream);

        final Document doc = XmlUtil.readXmlToDocument(notifyPayloadStream);
        assertNotNull(doc);
        userNotification = new NetconfMessage(doc);
    }

    static SchemaContext getNotificationSchemaContext(Class<?> loadClass) {
        final List<InputStream> modelsToParse = Collections.singletonList(loadClass.getResourceAsStream("/schemas/user-notification.yang"));
        final YangContextParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModelsFromStreams(modelsToParse);
        assertTrue(!modules.isEmpty());
        final SchemaContext schemaContext = parser.resolveSchemaContext(modules);
        assertNotNull(schemaContext);
        return schemaContext;
    }

    @Test
    public void test() throws Exception {
        final DOMNotification domNotification = messageTransformer.toNotification(userNotification);
        final ContainerNode root = domNotification.getBody();
        assertNotNull(root);
        assertEquals(6, Iterables.size(root.getValue()));
        assertEquals("user-visited-page", root.getNodeType().getLocalName());
        assertEquals(new SimpleDateFormat(NetconfNotification.RFC3339_DATE_FORMAT_WITH_MILLIS_BLUEPRINT).parse("2015-10-23T09:42:27.67175+00:00"),
                ((DOMEvent) domNotification).getEventTime());
    }
}
