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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

/**
 * Base class for RPC tests.
 *
 * @author Thomas Pantelis
 */
public class AbstractRpcTest {
    static final String TEST_REV = "2014-08-28";
    static final String TEST_NS = "urn:test";
    static final URI TEST_URI = URI.create(TEST_NS);
    static final QName TEST_RPC = QName.create(TEST_NS, TEST_REV, "test-rpc");
    static final QName TEST_RPC_INPUT = QName.create(TEST_NS, TEST_REV, "input");
    static final QName TEST_RPC_INPUT_DATA = QName.create(TEST_NS, TEST_REV, "input-data");
    static final QName TEST_RPC_OUTPUT = QName.create(TEST_NS, TEST_REV, "output");
    static final QName TEST_RPC_OUTPUT_DATA = new QName(TEST_URI, "output-data");


    static final SchemaPath TEST_RPC_TYPE = SchemaPath.create(true, TEST_RPC);
    static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(TEST_RPC));
    static final DOMRpcIdentifier TEST_RPC_ID = DOMRpcIdentifier.create(TEST_RPC_TYPE, TEST_PATH);

    static ActorSystem node1;
    static ActorSystem node2;
    static RemoteRpcProviderConfig config1;
    static RemoteRpcProviderConfig config2;

    protected ActorRef rpcBroker1;
    protected JavaTestKit rpcRegistry1Probe;
    protected ActorRef rpcBroker2;
    protected JavaTestKit rpcRegistry2Probe;
    protected Broker.ProviderSession brokerSession;
    protected SchemaContext schemaContext;
    protected RemoteRpcImplementation remoteRpcImpl1;
    protected RemoteRpcImplementation remoteRpcImpl2;
    protected DOMRpcService domRpcService1;
    protected DOMRpcService domRpcService2;

    @BeforeClass
    public static void setup() throws InterruptedException {
        config1 = new RemoteRpcProviderConfig.Builder("memberA").build();
        config2 = new RemoteRpcProviderConfig.Builder("memberB").build();
        node1 = ActorSystem.create("opendaylight-rpc", config1.get());
        node2 = ActorSystem.create("opendaylight-rpc", config2.get());
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(node1);
        JavaTestKit.shutdownActorSystem(node2);
        node1 = null;
        node2 = null;
    }

    @Before
    public void setUp() {
        schemaContext = new YangParserImpl().parseFiles(Arrays.asList(
                new File(RpcBrokerTest.class.getResource("/test-rpc.yang").getPath())));

        domRpcService1 = Mockito.mock(DOMRpcService.class);
        domRpcService2 = Mockito.mock(DOMRpcService.class);
        rpcRegistry1Probe = new JavaTestKit(node1);
        rpcBroker1 = node1.actorOf(RpcBroker.props(domRpcService1));
        rpcRegistry2Probe = new JavaTestKit(node2);
        rpcBroker2 = node2.actorOf(RpcBroker.props(domRpcService2));
        remoteRpcImpl1 = new RemoteRpcImplementation(rpcRegistry1Probe.getRef(), config1);
        remoteRpcImpl2 = new RemoteRpcImplementation(rpcRegistry2Probe.getRef(), config2);


    }

    static void assertRpcErrorEquals(final RpcError rpcError, final ErrorSeverity severity,
            final ErrorType errorType, final String tag, final String message, final String applicationTag, final String info,
            final String causeMsg) {
        assertEquals("getSeverity", severity, rpcError.getSeverity());
        assertEquals("getErrorType", errorType, rpcError.getErrorType());
        assertEquals("getTag", tag, rpcError.getTag());
        assertTrue("getMessage contains " + message, rpcError.getMessage().contains(message));
        assertEquals("getApplicationTag", applicationTag, rpcError.getApplicationTag());
        assertEquals("getInfo", info, rpcError.getInfo());

        if(causeMsg == null) {
            assertNull("Unexpected cause " + rpcError.getCause(), rpcError.getCause());
        } else {
            assertEquals("Cause message", causeMsg, rpcError.getCause().getMessage());
        }
    }

    static void assertCompositeNodeEquals(final NormalizedNode<? , ?> exp, final NormalizedNode<? , ? > actual) {
        assertEquals(exp, actual);
    }

    static ContainerNode makeRPCInput(final String data) {
        return Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(TEST_RPC_INPUT))
            .withChild(ImmutableNodes.leafNode(TEST_RPC_INPUT_DATA, data)).build();

    }

    static ContainerNode makeRPCOutput(final String data) {
        return Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(TEST_RPC_OUTPUT))
                .withChild(ImmutableNodes.leafNode(TEST_RPC_OUTPUT, data)).build();
    }

    static void assertFailedRpcResult(final DOMRpcResult rpcResult, final ErrorSeverity severity,
            final ErrorType errorType, final String tag, final String message, final String applicationTag, final String info,
            final String causeMsg) {

        assertNotNull("RpcResult was null", rpcResult);
        final Collection<RpcError> rpcErrors = rpcResult.getErrors();
        assertEquals("RpcErrors count", 1, rpcErrors.size());
        assertRpcErrorEquals(rpcErrors.iterator().next(), severity, errorType, tag, message,
                applicationTag, info, causeMsg);
    }

    static void assertSuccessfulRpcResult(final DOMRpcResult rpcResult,
            final NormalizedNode<? , ?> expOutput) {
        assertNotNull("RpcResult was null", rpcResult);
        assertCompositeNodeEquals(expOutput, rpcResult.getResult());
    }

    static class TestException extends Exception {
        private static final long serialVersionUID = 1L;

        static final String MESSAGE = "mock error";

        TestException() {
            super(MESSAGE);
        }
    }
}
