/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.google.common.collect.ImmutableList;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    static ActorSystem node1;
    static ActorSystem node2;

    protected ActorRef rpcBroker1;
    protected JavaTestKit probeReg1;
    protected ActorRef rpcBroker2;
    protected JavaTestKit probeReg2;
    protected Broker.ProviderSession brokerSession;
    protected SchemaContext schemaContext;

    @BeforeClass
    public static void setup() throws InterruptedException {
        RemoteRpcProviderConfig config1 = new RemoteRpcProviderConfig.Builder("memberA").build();
        RemoteRpcProviderConfig config2 = new RemoteRpcProviderConfig.Builder("memberB").build();
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

        brokerSession = Mockito.mock(Broker.ProviderSession.class);
        probeReg1 = new JavaTestKit(node1);
        rpcBroker1 = node1.actorOf(RpcBroker.props(brokerSession, probeReg1.getRef(), schemaContext));
        probeReg2 = new JavaTestKit(node2);
        rpcBroker2 = node2.actorOf(RpcBroker.props(brokerSession, probeReg2.getRef(), schemaContext));

    }

    static void assertRpcErrorEquals(RpcError rpcError, ErrorSeverity severity,
            ErrorType errorType, String tag, String message, String applicationTag, String info,
            String causeMsg) {
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

    static void assertCompositeNodeEquals(CompositeNode exp, CompositeNode actual) {
        assertEquals("NodeType getNamespace", exp.getNodeType().getNamespace(),
                actual.getNodeType().getNamespace());
        assertEquals("NodeType getLocalName", exp.getNodeType().getLocalName(),
                actual.getNodeType().getLocalName());
        for(Node<?> child: exp.getValue()) {
            List<Node<?>> c = actual.get(child.getNodeType());
            assertNotNull("Missing expected child " + child.getNodeType(), c);
            if(child instanceof CompositeNode) {
                assertCompositeNodeEquals((CompositeNode) child, (CompositeNode)c.get(0));
            } else {
                assertEquals("Value for Node " + child.getNodeType(), child.getValue(),
                        c.get(0).getValue());
            }
        }
    }

    static CompositeNode makeRPCInput(String data) {
        CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder()
                .setQName(TEST_RPC_INPUT).addLeaf(TEST_RPC_INPUT_DATA, data);
        return ImmutableCompositeNode.create(
                TEST_RPC, ImmutableList.<Node<?>>of(builder.build()));
    }

    static CompositeNode makeRPCOutput(String data) {
        CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder()
                .setQName(TEST_RPC_OUTPUT).addLeaf(TEST_RPC_OUTPUT_DATA, data);
        return ImmutableCompositeNode.create(
                TEST_RPC, ImmutableList.<Node<?>>of(builder.build()));
    }

    static void assertFailedRpcResult(RpcResult<CompositeNode> rpcResult, ErrorSeverity severity,
            ErrorType errorType, String tag, String message, String applicationTag, String info,
            String causeMsg) {

        assertNotNull("RpcResult was null", rpcResult);
        assertEquals("isSuccessful", false, rpcResult.isSuccessful());
        Collection<RpcError> rpcErrors = rpcResult.getErrors();
        assertEquals("RpcErrors count", 1, rpcErrors.size());
        assertRpcErrorEquals(rpcErrors.iterator().next(), severity, errorType, tag, message,
                applicationTag, info, causeMsg);
    }

    static void assertSuccessfulRpcResult(RpcResult<CompositeNode> rpcResult,
            CompositeNode expOutput) {

        assertNotNull("RpcResult was null", rpcResult);
        assertEquals("isSuccessful", true, rpcResult.isSuccessful());
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
