package org.opendaylight.controller.sal.connect.netconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toId;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import java.io.File;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;
import org.opendaylight.yangtools.yang.parser.util.NamedFileInputStream;
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

    static SchemaContext cfgCtx;
    static NetconfMessageTransformer messageTransformer;

    @SuppressWarnings("deprecation")
    @BeforeClass
    public static void setup() throws Exception {
        CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();

        File rpcYang = new File(NetconfToRpcRequestTest.class.getResource("/schemas/rpc-notification-subscription.yang").toURI());
        File configYang = new File(NetconfToRpcRequestTest.class.getResource("/schemas/config-test-rpc.yang").toURI());

        YangStatementSourceImpl rpcSource = new YangStatementSourceImpl(new NamedFileInputStream(rpcYang, rpcYang.getPath()));
        YangStatementSourceImpl configSource = new YangStatementSourceImpl(new NamedFileInputStream(configYang, configYang.getPath()));

        reactor.addSource(rpcSource);
        reactor.addSource(configSource);

        cfgCtx = reactor.buildEffective();
        assertNotNull(cfgCtx);

        messageTransformer = new NetconfMessageTransformer(cfgCtx, true);
    }

    private LeafNode<Object> buildLeaf(final QName running, final Object value) {
        return Builders.leafBuilder().withNodeIdentifier(toId(running)).withValue(value).build();
    }

    @Test
    public void testUserDefinedRpcCall() throws Exception {
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> rootBuilder = Builders.containerBuilder();
        rootBuilder.withNodeIdentifier(toId(SUBSCRIBE_RPC_NAME));

        rootBuilder.withChild(buildLeaf(STREAM_NAME, "NETCONF"));
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

    // The edit config defined in yang has no output
    @Test(expected = IllegalArgumentException.class)
    public void testRpcResponse() throws Exception {
        final NetconfMessage response = new NetconfMessage(XmlUtil.readXmlToDocument(
                "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"m-5\">\n" +
                "<data xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">" +
                "module schema" +
                "</data>\n" +
                "</rpc-reply>\n"
        ));

        messageTransformer.toRpcResult(response, toPath(EDIT_CONFIG_QNAME));
    }

}
