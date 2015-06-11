/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
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

    static ActorSystem node1;
    static ActorSystem node2;

    protected ActorRef rpcBroker1;
    protected JavaTestKit probeReg1;
    protected ActorRef rpcBroker2;
    protected JavaTestKit probeReg2;
    protected Broker.ProviderSession brokerSession;
    protected SchemaContext schemaContext;
    protected DOMRpcService rpcService;

    @BeforeClass
    public static void setup() throws InterruptedException {
        final RemoteRpcProviderConfig config1 = new RemoteRpcProviderConfig.Builder("memberA").build();
        final RemoteRpcProviderConfig config2 = new RemoteRpcProviderConfig.Builder("memberB").build();
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
        rpcService = Mockito.mock(DOMRpcService.class);

        probeReg1 = new JavaTestKit(node1);
        rpcBroker1 = node1.actorOf(RpcBroker.props(rpcService));
        probeReg2 = new JavaTestKit(node2);
        rpcBroker2 = node2.actorOf(RpcBroker.props(rpcService));

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

    static class TestException extends Exception {
        private static final long serialVersionUID = 1L;

        static final String MESSAGE = "mock error";

        TestException() {
            super(MESSAGE);
        }
    }
}
