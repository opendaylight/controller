package org.opendaylight.controller.sal.connect.netconf;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangContextParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

/**
 * Test case for reported bug 1355
 *
 * @author Lukas Sedlak
 * @see <a
 *      https://bugs.opendaylight.org/show_bug.cgi?id=1355</a>
 */
public class NetconfToRpcRequestTest {

    private String TEST_MODEL_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:rpc-test";
    private String REVISION = "2014-07-14";
    private QName INPUT_QNAME = QName.create(TEST_MODEL_NAMESPACE, REVISION, "input");
    private QName STREAM_NAME = QName.create(TEST_MODEL_NAMESPACE, REVISION, "stream-name");
    private QName RPC_NAME = QName.create(TEST_MODEL_NAMESPACE, REVISION, "subscribe");

    NetconfMessageTransformer messageTransformer;

    @SuppressWarnings("deprecation")
    @Before
    public void setup() throws Exception {
        final List<InputStream> modelsToParse = Collections
            .singletonList(getClass().getResourceAsStream("/schemas/rpc-notification-subscription.yang"));
        final YangContextParser parser = new YangParserImpl();
        final Set<Module> modules = parser.parseYangModelsFromStreams(modelsToParse);
        assertTrue(!modules.isEmpty());
        final SchemaContext schemaContext = parser.resolveSchemaContext(modules);
        assertNotNull(schemaContext);

        messageTransformer = new NetconfMessageTransformer();
        messageTransformer.onGlobalContextUpdated(schemaContext);
    }

    @Test
    public void test() throws Exception {
        final CompositeNodeBuilder<ImmutableCompositeNode> rootBuilder = ImmutableCompositeNode.builder();
        rootBuilder.setQName(RPC_NAME);

        final CompositeNodeBuilder<ImmutableCompositeNode> inputBuilder = ImmutableCompositeNode.builder();
        inputBuilder.setQName(INPUT_QNAME);
        inputBuilder.addLeaf(STREAM_NAME, "NETCONF");

        rootBuilder.add(inputBuilder.toInstance());
        final ImmutableCompositeNode root = rootBuilder.toInstance();

        final CompositeNode flattenedNode = NetconfMessageTransformUtil.flattenInput(root);
        assertNotNull(flattenedNode);
        assertEquals(1, flattenedNode.size());

        final List<CompositeNode> inputNode = flattenedNode.getCompositesByName(INPUT_QNAME);
        assertNotNull(inputNode);
        assertTrue(inputNode.isEmpty());

        final NetconfMessage message = messageTransformer.toRpcRequest(RPC_NAME, root);
        assertNotNull(message);
    }
}
