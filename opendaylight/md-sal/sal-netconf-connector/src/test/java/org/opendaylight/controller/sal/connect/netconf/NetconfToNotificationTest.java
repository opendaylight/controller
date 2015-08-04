package org.opendaylight.controller.sal.connect.netconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.Iterables;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;
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

    static SchemaContext getNotificationSchemaContext(Class<?> loadClass) throws ReactorException {
        final List<InputStream> modelsToParse = Collections.singletonList(loadClass.getResourceAsStream("/schemas/user-notification.yang"));
        CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        for (InputStream is : modelsToParse) {
            reactor.addSource(new YangStatementSourceImpl(is));
        }
        final SchemaContext schemaContext = reactor.buildEffective();
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
        assertEquals(new SimpleDateFormat(NetconfNotification.RFC3339_DATE_FORMAT_BLUEPRINT).parse("2007-07-08T00:01:00Z"),
                ((DOMEvent) domNotification).getEventTime());
    }
}
