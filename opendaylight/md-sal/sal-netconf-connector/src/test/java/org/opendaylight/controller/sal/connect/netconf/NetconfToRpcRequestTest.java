package org.opendaylight.controller.sal.connect.netconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
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

        messageTransformer = new NetconfMessageTransformer();
    }

    @Test
    public void testIsDataEditOperation() throws Exception {
        messageTransformer.onGlobalContextUpdated(cfgCtx);

        final CompositeNodeBuilder<ImmutableCompositeNode> rootBuilder = ImmutableCompositeNode.builder();
        rootBuilder.setQName(EDIT_CONFIG_QNAME);

        final CompositeNodeBuilder<ImmutableCompositeNode> inputBuilder = ImmutableCompositeNode.builder();
        inputBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "input"));

        final CompositeNodeBuilder<ImmutableCompositeNode> targetBuilder = ImmutableCompositeNode.builder();
        targetBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "target"));
        targetBuilder.addLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "running"), null);

        final CompositeNodeBuilder<ImmutableCompositeNode> configBuilder = ImmutableCompositeNode.builder();
        configBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "config"));

        final CompositeNodeBuilder<ImmutableCompositeNode> anyxmlTopBuilder = ImmutableCompositeNode.builder();
        anyxmlTopBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "top"));

        final CompositeNodeBuilder<ImmutableCompositeNode> anyxmlInterfBuilder = ImmutableCompositeNode.builder();
        anyxmlInterfBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "interface"));

        anyxmlInterfBuilder.addLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "name"), "Ethernet0/0");
        anyxmlInterfBuilder.addLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "mtu"), "1500");

        anyxmlTopBuilder.add(anyxmlInterfBuilder.build());
        configBuilder.add(anyxmlTopBuilder.build());

        inputBuilder.add(targetBuilder.build());
        inputBuilder.add(configBuilder.build());

        rootBuilder.add(inputBuilder.build());
        final ImmutableCompositeNode root = rootBuilder.build();

        final NetconfMessage message = messageTransformer.toRpcRequest(EDIT_CONFIG_QNAME, root);
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

    @Test
    public void testIsGetOperation() throws Exception {
        messageTransformer.onGlobalContextUpdated(cfgCtx);

        final CompositeNodeBuilder<ImmutableCompositeNode> rootBuilder = ImmutableCompositeNode.builder();
        rootBuilder.setQName(GET_QNAME);

        final CompositeNodeBuilder<ImmutableCompositeNode> inputBuilder = ImmutableCompositeNode.builder();
        inputBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "input"));

        rootBuilder.add(inputBuilder.build());
        final ImmutableCompositeNode root = rootBuilder.build();

        final NetconfMessage message = messageTransformer.toRpcRequest(GET_QNAME, root);
        assertNotNull(message);

        final Document xmlDoc = message.getDocument();
        final org.w3c.dom.Node rpcChild = xmlDoc.getFirstChild();
        assertEquals(rpcChild.getLocalName(), "rpc");

        final org.w3c.dom.Node get = rpcChild.getFirstChild();
        assertEquals(get.getLocalName(), "get");
    }

    @Test
    public void testIsGetConfigOperation() throws Exception {
        messageTransformer.onGlobalContextUpdated(cfgCtx);

        final CompositeNodeBuilder<ImmutableCompositeNode> rootBuilder = ImmutableCompositeNode.builder();
        rootBuilder.setQName(GET_CONFIG_QNAME);

        final CompositeNodeBuilder<ImmutableCompositeNode> inputBuilder = ImmutableCompositeNode.builder();
        inputBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "input"));

        final CompositeNodeBuilder<ImmutableCompositeNode> sourceBuilder = ImmutableCompositeNode.builder();
        sourceBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "source"));
        sourceBuilder.addLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "running"), null);

        final CompositeNodeBuilder<ImmutableCompositeNode> anyxmlFilterBuilder = ImmutableCompositeNode.builder();
        anyxmlFilterBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "filter"));

        final CompositeNodeBuilder<ImmutableCompositeNode> anyxmlTopBuilder = ImmutableCompositeNode.builder();
        anyxmlTopBuilder.setQName(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "top"));
        anyxmlTopBuilder.addLeaf(QName.create(CONFIG_TEST_NAMESPACE, CONFIG_TEST_REVISION, "users"), null);

        anyxmlFilterBuilder.add(anyxmlTopBuilder.build());

        inputBuilder.add(sourceBuilder.build());
        inputBuilder.add(anyxmlFilterBuilder.build());
        rootBuilder.add(inputBuilder.build());
        final ImmutableCompositeNode root = rootBuilder.build();

        final NetconfMessage message = messageTransformer.toRpcRequest(GET_CONFIG_QNAME, root);
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
        messageTransformer.onGlobalContextUpdated(notifCtx);

        final CompositeNodeBuilder<ImmutableCompositeNode> rootBuilder = ImmutableCompositeNode.builder();
        rootBuilder.setQName(SUBSCRIBE_RPC_NAME);

        final CompositeNodeBuilder<ImmutableCompositeNode> inputBuilder = ImmutableCompositeNode.builder();
        inputBuilder.setQName(INPUT_QNAME);
        inputBuilder.addLeaf(STREAM_NAME, "NETCONF");

        rootBuilder.add(inputBuilder.build());
        final ImmutableCompositeNode root = rootBuilder.build();

        final CompositeNode flattenedNode = NetconfMessageTransformUtil.flattenInput(root);
        assertNotNull(flattenedNode);
        assertEquals(1, flattenedNode.size());

        final List<CompositeNode> inputNode = flattenedNode.getCompositesByName(INPUT_QNAME);
        assertNotNull(inputNode);
        assertTrue(inputNode.isEmpty());

        final NetconfMessage message = messageTransformer.toRpcRequest(SUBSCRIBE_RPC_NAME, root);
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
        final RpcResult<CompositeNode> compositeNodeRpcResult = messageTransformer.toRpcResult(response, SUBSCRIBE_RPC_NAME);
        final Node<?> dataNode = compositeNodeRpcResult.getResult().getValue().get(0);
        assertEquals("module schema", dataNode.getValue());
    }

}
