package org.opendaylight.controller.sal.connect.netconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.w3c.dom.Document;

public class NetconfStateSchemasTest {

    @Test
    public void testCreate() throws Exception {
        final Document schemasXml = XmlUtil.readXmlToDocument(getClass().getResourceAsStream("/netconf-state.schemas.payload.xml"));
        final CompositeNode compositeNodeSchemas = (CompositeNode) XmlDocumentUtils.toDomNode(schemasXml);
        final NetconfStateSchemas schemas = NetconfStateSchemas.create(new RemoteDeviceId("device"), compositeNodeSchemas);

        final Set<QName> availableYangSchemasQNames = schemas.getAvailableYangSchemasQNames();
        assertEquals(73, availableYangSchemasQNames.size());

        assertThat(availableYangSchemasQNames,
                hasItem(QName.create("urn:TBD:params:xml:ns:yang:network-topology", "2013-07-12", "network-topology")));
    }
}
