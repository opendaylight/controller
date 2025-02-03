/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.yang.common.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

/**
 * Base class for RPC tests.
 *
 * @author Thomas Pantelis
 */
public class AbstractOpsTest {
    static final String TEST_REV = "2014-08-28";
    static final String TEST_NS = "urn:test";
    static final URI TEST_URI = URI.create(TEST_NS);
    static final QName TEST_RPC = QName.create(TEST_NS, TEST_REV, "test-something");
    static final QName TEST_RPC_INPUT = QName.create(TEST_NS, TEST_REV, "input");
    static final QName TEST_RPC_INPUT_DATA = QName.create(TEST_NS, TEST_REV, "input-data");
    static final QName TEST_RPC_OUTPUT = QName.create(TEST_NS, TEST_REV, "output");


    static final Absolute TEST_RPC_TYPE = Absolute.of(TEST_RPC);
    static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_RPC);
    public static final DOMRpcIdentifier TEST_RPC_ID = DOMRpcIdentifier.create(TEST_RPC, TEST_PATH);
    public static final DOMDataTreeIdentifier TEST_DATA_TREE_ID =
        DOMDataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, TEST_PATH);

    static ActorSystem node1;
    static ActorSystem node2;
    static RemoteOpsProviderConfig config1;
    static RemoteOpsProviderConfig config2;

    protected ActorRef rpcInvoker1;
    protected TestKit rpcRegistry1Probe;
    protected ActorRef rpcInvoker2;
    protected TestKit rpcRegistry2Probe;
    protected SchemaContext schemaContext;
    protected RemoteRpcImplementation remoteRpcImpl1;
    protected RemoteRpcImplementation remoteRpcImpl2;
    protected RemoteActionImplementation remoteActionImpl1;
    protected RemoteActionImplementation remoteActionImpl2;

    @Mock
    protected DOMRpcService domRpcService1;
    @Mock
    protected DOMActionService domActionService1;
    @Mock
    protected DOMRpcService domRpcService2;
    @Mock
    protected DOMActionService domActionService2;

    @BeforeClass
    public static void setup() {
        config1 = new RemoteOpsProviderConfig.Builder("memberA").build();
        config2 = new RemoteOpsProviderConfig.Builder("memberB").build();
        node1 = ActorSystem.create("opendaylight-rpc", config1.get());
        node2 = ActorSystem.create("opendaylight-rpc", config2.get());
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(node1);
        TestKit.shutdownActorSystem(node2);
        node1 = null;
        node2 = null;
    }

    @Before
    public void setUp() {
        schemaContext = YangParserTestUtils.parseYangResources(AbstractOpsTest.class, "/test-rpc.yang");

        MockitoAnnotations.initMocks(this);

        rpcRegistry1Probe = new TestKit(node1);
        rpcInvoker1 = node1.actorOf(OpsInvoker.props("test1", domRpcService1, domActionService1));
        rpcRegistry2Probe = new TestKit(node2);
        rpcInvoker2 = node2.actorOf(OpsInvoker.props("test2", domRpcService2, domActionService2));
        remoteRpcImpl1 = new RemoteRpcImplementation(rpcInvoker2, config1);
        remoteRpcImpl2 = new RemoteRpcImplementation(rpcInvoker1, config2);
        remoteActionImpl1 = new RemoteActionImplementation(rpcInvoker2, config1);
        remoteActionImpl2 = new RemoteActionImplementation(rpcInvoker1, config2);
    }

    static void assertRpcErrorEquals(final RpcError rpcError, final ErrorSeverity severity,
                                     final ErrorType errorType, final String tag, final String message,
                                     final String applicationTag, final String info, final String causeMsg) {
        assertEquals("getSeverity", severity, rpcError.getSeverity());
        assertEquals("getErrorType", errorType, rpcError.getErrorType());
        assertEquals("getTag", tag, rpcError.getTag());
        assertTrue("getMessage contains " + message, rpcError.getMessage().contains(message));
        assertEquals("getApplicationTag", applicationTag, rpcError.getApplicationTag());
        assertEquals("getInfo", info, rpcError.getInfo());

        if (causeMsg == null) {
            assertNull("Unexpected cause " + rpcError.getCause(), rpcError.getCause());
        } else {
            assertEquals("Cause message", causeMsg, rpcError.getCause().getMessage());
        }
    }

    static void assertCompositeNodeEquals(final NormalizedNode exp, final NormalizedNode actual) {
        assertEquals(exp, actual);
    }

    public static ContainerNode makeRPCInput(final String data) {
        return ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_RPC_INPUT))
            .withChild(ImmutableNodes.leafNode(TEST_RPC_INPUT_DATA, data))
            .build();

    }

    public static ContainerNode makeRPCOutput(final String data) {
        return ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_RPC_OUTPUT))
            .withChild(ImmutableNodes.leafNode(TEST_RPC_OUTPUT, data))
            .build();
    }

    static void assertFailedRpcResult(final DOMRpcResult rpcResult, final ErrorSeverity severity,
                                      final ErrorType errorType, final String tag, final String message,
                                      final String applicationTag, final String info, final String causeMsg) {
        assertNotNull("RpcResult was null", rpcResult);
        final var rpcErrors = rpcResult.errors();
        assertEquals("RpcErrors count", 1, rpcErrors.size());
        assertRpcErrorEquals(rpcErrors.iterator().next(), severity, errorType, tag, message,
                applicationTag, info, causeMsg);
    }

    static void assertSuccessfulRpcResult(final DOMRpcResult rpcResult, final NormalizedNode expOutput) {
        assertNotNull("RpcResult was null", rpcResult);
        assertCompositeNodeEquals(expOutput, rpcResult.value());
    }

    static class TestException extends Exception {
        private static final long serialVersionUID = 1L;

        static final String MESSAGE = "mock error";

        TestException() {
            super(MESSAGE);
        }
    }
}
