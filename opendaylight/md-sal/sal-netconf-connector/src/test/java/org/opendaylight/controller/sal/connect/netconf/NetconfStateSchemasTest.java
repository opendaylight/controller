package org.opendaylight.controller.sal.connect.netconf;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.ToNormalizedNodeParser;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NetconfStateSchemasTest {

    @Test
    public void testCreate() throws Exception {
        final DataSchemaNode schemasNode = ((ContainerSchemaNode) NetconfDevice.INIT_SCHEMA_CTX.getDataChildByName("netconf-state")).getDataChildByName("schemas");

        final Document schemasXml = XmlUtil.readXmlToDocument(getClass().getResourceAsStream("/netconf-state.schemas.payload.xml"));
        final ToNormalizedNodeParser<Element, ContainerNode, ContainerSchemaNode> containerNodeParser = DomToNormalizedNodeParserFactory.getInstance(XmlUtils.DEFAULT_XML_CODEC_PROVIDER, NetconfDevice.INIT_SCHEMA_CTX).getContainerNodeParser();
        final ContainerNode compositeNodeSchemas = containerNodeParser.parse(Collections.singleton(schemasXml.getDocumentElement()), (ContainerSchemaNode) schemasNode);
        final NetconfStateSchemas schemas = NetconfStateSchemas.create(new RemoteDeviceId("device", new InetSocketAddress(99)), compositeNodeSchemas);

        final Set<QName> availableYangSchemasQNames = schemas.getAvailableYangSchemasQNames();
        assertEquals(73, availableYangSchemasQNames.size());

        assertThat(availableYangSchemasQNames,
                hasItem(QName.create("urn:TBD:params:xml:ns:yang:network-topology", "2013-07-12", "network-topology")));
    }
}
