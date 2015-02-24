package org.opendaylight.controller.sal.connect.netconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toId;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.w3c.dom.Document;


/**
 * Test case for reported bug 1355
 *
 * @author Lukas Sedlak
 * @see <a
 *      https://bugs.opendaylight.org/show_bug.cgi?id=1355</a>
 */
public class NetconfToRpcRequestTest {

    private final static String TEST_MODEL_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:rpc-test";
    private final static String REVISION = "2014-07-14";
    private final static QName INPUT_QNAME = QName.create(TEST_MODEL_NAMESPACE, REVISION, "input");
    private final static QName STREAM_NAME = QName.create(TEST_MODEL_NAMESPACE, REVISION, "stream-name");
    private final static QName SUBSCRIBE_RPC_NAME = QName.create(TEST_MODEL_NAMESPACE, REVISION, "subscribe");

    private final static String CONFIG_TEST_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:test:rpc:config:defs";
    private final static String CONFIG_TEST_REVISION = "2014-07-21";
    private final static QName EDIT_CONFIG_QNAME = QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "edit-config");
    private final static QName GET_QNAME = QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "get");
    private final static QName GET_CONFIG_QNAME = QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "get-config");

    static SchemaContext notifCtx;
    static SchemaContext cfgCtx;
    static NetconfMessageTransformer messageTransformer;

    @SuppressWarnings("deprecation")
    @BeforeClass
    public static void setup() throws Exception {
        List<InputStream> modelsToParse = Collections
            .singletonList(NetconfToRpcRequestTest.class.getResourceAsStream("/schemas/rpc-notification-subscription.yang"));
        YangContextParser parser = new YangParserImpl();
        final Set<Module> notifModules = parser.parseYangModelsFromStreams(modelsToParse);
        assertTrue(!notifModules.isEmpty());

        notifCtx = parser.resolveSchemaContext(notifModules);
        assertNotNull(notifCtx);

        modelsToParse = Collections
            .singletonList(NetconfToRpcRequestTest.class.getResourceAsStream("/schemas/config-test-rpc.yang"));
        parser = new YangParserImpl();
        final Set<Module> configModules = parser.parseYangModelsFromStreams(modelsToParse);
        cfgCtx = parser.resolveSchemaContext(configModules);
        assertNotNull(cfgCtx);

        messageTransformer = new NetconfMessageTransformer(cfgCtx);
    }

    @Test
    public void testIsDataEditOperation() throws Exception {
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> rootBuilder = Builders.containerBuilder();
        rootBuilder.withNodeIdentifier(toId(EDIT_CONFIG_QNAME));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> inputBuilder = Builders.containerBuilder();
        inputBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "input")));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> targetBuilder = Builders.containerBuilder();
        targetBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "target")));
        targetBuilder.withChild(buildLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "running"), null));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> configBuilder = Builders.containerBuilder();
        configBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "config")));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> anyxmlTopBuilder = Builders.containerBuilder();
        anyxmlTopBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "top")));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> anyxmlInterfBuilder = Builders.containerBuilder();
        anyxmlInterfBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "interface")));

        anyxmlInterfBuilder.withChild(buildLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "name"), "Ethernet0/0"));
        anyxmlInterfBuilder.withChild(buildLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "mtu"), "1500"));

        anyxmlTopBuilder.withChild(anyxmlInterfBuilder.build());
        configBuilder.withChild(anyxmlTopBuilder.build());

        inputBuilder.withChild(targetBuilder.build());
        inputBuilder.withChild(configBuilder.build());

        rootBuilder.withChild(inputBuilder.build());
        final ContainerNode root = rootBuilder.build();

        final NetconfMessage message = messageTransformer.toRpcRequest(toPath(EDIT_CONFIG_QNAME), root);
        assertNotNull(message);

        final Document xmlDoc = message.getDocument();
        org.w3c.dom.Node rpcChild = xmlDoc.getFirstChild();
        assertEquals(rpcChild.getLocalName(), "rpc");

        final org.w3c.dom.Node editConfigNode = rpcChild.getFirstChild();
        assertEquals(editConfigNode.getLocalName(), "edit-config");

        final org.w3c.dom.Node targetNode = editConfigNode.getFirstChild();
        assertEquals(targetNode.getLocalName(), "target");

        final org.w3c.dom.Node runningNode = targetNode.getFirstChild();
        assertEquals(runningNode.getLocalName(), "running");

        final org.w3c.dom.Node configNode = targetNode.getNextSibling();
        assertEquals(configNode.getLocalName(), "config");

        final org.w3c.dom.Node topNode = configNode.getFirstChild();
        assertEquals(topNode.getLocalName(), "top");

        final org.w3c.dom.Node interfaceNode = topNode.getFirstChild();
        assertEquals(interfaceNode.getLocalName(), "interface");

        final org.w3c.dom.Node nameNode = interfaceNode.getFirstChild();
        assertEquals(nameNode.getLocalName(), "name");

        final org.w3c.dom.Node mtuNode = nameNode.getNextSibling();
        assertEquals(mtuNode.getLocalName(), "mtu");
    }

    private LeafNode<Object> buildLeaf(final QName running, final Object value) {
        return Builders.leafBuilder().withNodeIdentifier(toId(running)).withValue(value).build();
    }

    @Test
    public void testIsGetOperation() throws Exception {
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> rootBuilder = Builders.containerBuilder();
        rootBuilder.withNodeIdentifier(toId(GET_QNAME));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> inputBuilder = Builders.containerBuilder();
        inputBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "input")));

        rootBuilder.withChild(inputBuilder.build());
        final ContainerNode root = rootBuilder.build();

        final NetconfMessage message = messageTransformer.toRpcRequest(toPath(GET_QNAME), root);
        assertNotNull(message);

        final Document xmlDoc = message.getDocument();
        final org.w3c.dom.Node rpcChild = xmlDoc.getFirstChild();
        assertEquals(rpcChild.getLocalName(), "rpc");

        final org.w3c.dom.Node get = rpcChild.getFirstChild();
        assertEquals(get.getLocalName(), "get");
    }

    @Test
    public void testIsGetConfigOperation() throws Exception {
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> rootBuilder = Builders.containerBuilder();
        rootBuilder.withNodeIdentifier(toId(GET_CONFIG_QNAME));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> inputBuilder = Builders.containerBuilder();
        inputBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "input")));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> sourceBuilder = Builders.containerBuilder();
        sourceBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "source")));
        sourceBuilder.withChild(buildLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "running"), null));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> anyxmlFilterBuilder = Builders.containerBuilder();
        anyxmlFilterBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "filter")));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> anyxmlTopBuilder = Builders.containerBuilder();
        anyxmlTopBuilder.withNodeIdentifier(toId(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "top")));
        anyxmlTopBuilder.withChild(buildLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "users"), null));

        anyxmlFilterBuilder.withChild(anyxmlTopBuilder.build());

        inputBuilder.withChild(sourceBuilder.build());
        inputBuilder.withChild(anyxmlFilterBuilder.build());
        rootBuilder.withChild(inputBuilder.build());
        final ContainerNode root = rootBuilder.build();

        final NetconfMessage message = messageTransformer.toRpcRequest(toPath(GET_CONFIG_QNAME), root);
        assertNotNull(message);

        final Document xmlDoc = message.getDocument();
        final org.w3c.dom.Node rpcChild = xmlDoc.getFirstChild();
        assertEquals(rpcChild.getLocalName(), "rpc");

        final org.w3c.dom.Node getConfig = rpcChild.getFirstChild();
        assertEquals(getConfig.getLocalName(), "get-config");

        final org.w3c.dom.Node sourceNode = getConfig.getFirstChild();
        assertEquals(sourceNode.getLocalName(), "source");

        final org.w3c.dom.Node runningNode = sourceNode.getFirstChild();
        assertEquals(runningNode.getLocalName(), "running");

        final org.w3c.dom.Node filterNode = sourceNode.getNextSibling();
        assertEquals(filterNode.getLocalName(), "filter");

        final org.w3c.dom.Node topNode = filterNode.getFirstChild();
        assertEquals(topNode.getLocalName(), "top");

        final org.w3c.dom.Node usersNode = topNode.getFirstChild();
        assertEquals(usersNode.getLocalName(), "users");
    }

    @Test
    public void testUserDefinedRpcCall() throws Exception {
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> rootBuilder = Builders.containerBuilder();
        rootBuilder.withNodeIdentifier(toId(SUBSCRIBE_RPC_NAME));

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> inputBuilder = Builders.containerBuilder();
        inputBuilder.withNodeIdentifier(toId(INPUT_QNAME));
        inputBuilder.withChild(buildLeaf(STREAM_NAME, "NETCONF"));

        rootBuilder.withChild(inputBuilder.build());
        final ContainerNode root = rootBuilder.build();

        final NetconfMessage message = messageTransformer.toRpcRequest(toPath(SUBSCRIBE_RPC_NAME), root);
        assertNotNull(message);

        final Document xmlDoc = message.getDocument();
        final org.w3c.dom.Node rpcChild = xmlDoc.getFirstChild();
        assertEquals(rpcChild.getLocalName(), "rpc");

        final org.w3c.dom.Node subscribeName = rpcChild.getFirstChild();
        assertEquals(subscribeName.getLocalName(), "subscribe");

        final org.w3c.dom.Node streamName = subscribeName.getFirstChild();
        assertEquals(streamName.getLocalName(), "stream-name");

    }

    @Test
    public void testRpcResponse() throws Exception {
        final NetconfMessage response = new NetconfMessage(XmlUtil.readXmlToDocument(
                "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"m-5\">\n" +
                "<data xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">" +
                "module schema" +
                "</data>\n" +
                "</rpc-reply>\n"
        ));
        final DOMRpcResult compositeNodeRpcResult = messageTransformer.toRpcResult(response, toPath(SUBSCRIBE_RPC_NAME));
        final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataNode = ((ContainerNode) compositeNodeRpcResult.getResult()).getValue().iterator().next();
        assertEquals("module schema", dataNode.getValue());
    }

}
