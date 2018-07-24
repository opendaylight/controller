/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.md.sal.dom.store.impl.TestModel;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Unit tests for DOMRpcRouter.
 *
 * @author Thomas Pantelis
 */
public class DOMRpcRouterTest {

    private static final NormalizedNode<?, ?> RPC_INPUT = ImmutableNodes.leafNode(
            QName.create(TestModel.TEST_QNAME.getModule(), "input-leaf"), "foo");
    private static final NormalizedNode<?, ?> RPC_OUTPUT = ImmutableNodes.leafNode(
            QName.create(TestModel.TEST_QNAME.getModule(), "output-leaf"), "bar");
    private final TestLegacyDOMRpcImplementation testLegacyRpcImpl = new TestLegacyDOMRpcImplementation();
    private final TestMdsalDOMRpcImplementation testMdsalRpcImpl = new TestMdsalDOMRpcImplementation();
    private org.opendaylight.mdsal.dom.broker.DOMRpcRouter mdsalRpcRouter;
    private DOMRpcRouter legacyRpcRouter;
    private DOMRpcIdentifier legacyTestRpcIdentifier;
    private DOMRpcIdentifier legacyTestRpcNoInputIdentifier;
    private org.opendaylight.mdsal.dom.api.DOMRpcIdentifier mdsalTestRpcIdentifier;
    private org.opendaylight.mdsal.dom.api.DOMRpcIdentifier mdsalTestRpcNoInputIdentifier;

    @Before
    public void setup() {
        mdsalRpcRouter = new org.opendaylight.mdsal.dom.broker.DOMRpcRouter();
        final SchemaContext schemaContext = TestModel.createTestContext();
        mdsalRpcRouter.onGlobalContextUpdated(schemaContext);
        legacyRpcRouter = new DOMRpcRouter(mdsalRpcRouter.getRpcService(), mdsalRpcRouter.getRpcProviderService());

        legacyTestRpcIdentifier = DOMRpcIdentifier.create(findRpc(schemaContext, "test-rpc"));
        legacyTestRpcNoInputIdentifier = DOMRpcIdentifier.create(findRpc(schemaContext, "test-rpc-no-input"));
        mdsalTestRpcIdentifier = org.opendaylight.mdsal.dom.api.DOMRpcIdentifier.create(
                findRpc(schemaContext, "test-rpc"));
        mdsalTestRpcNoInputIdentifier = org.opendaylight.mdsal.dom.api.DOMRpcIdentifier.create(
                findRpc(schemaContext, "test-rpc-no-input"));
    }

    @Test
    public void testLegacyRegistrationAndInvocation() throws InterruptedException, ExecutionException {
        final DOMRpcImplementationRegistration<TestLegacyDOMRpcImplementation> reg =
            legacyRpcRouter.registerRpcImplementation(testLegacyRpcImpl, legacyTestRpcIdentifier,
                    legacyTestRpcNoInputIdentifier);

        // Test success

        DefaultDOMRpcResult result = new DefaultDOMRpcResult(RPC_OUTPUT);
        testLegacyRpcImpl.init(Futures.immediateCheckedFuture(result));

        ListenableFuture<DOMRpcResult> future = legacyRpcRouter.invokeRpc(legacyTestRpcIdentifier.getType(), RPC_INPUT);

        assertSame(result, future.get());
        testLegacyRpcImpl.verifyInput(legacyTestRpcIdentifier, RPC_INPUT);

        // Test exception returned

        TestLegacyDOMRpcException rpcEx = new TestLegacyDOMRpcException();
        testLegacyRpcImpl.init(Futures.immediateFailedCheckedFuture(rpcEx));

        try {
            legacyRpcRouter.invokeRpc(legacyTestRpcIdentifier.getType(), RPC_INPUT).get();
            fail("Expected exception");
        } catch (ExecutionException e) {
            assertEquals(rpcEx, e.getCause());
        }

        // Test no input or output

        testLegacyRpcImpl.init(Futures.immediateCheckedFuture(null));

        future = legacyRpcRouter.invokeRpc(legacyTestRpcNoInputIdentifier.getType(), null);

        assertNull(future.get());
        testLegacyRpcImpl.verifyInput(legacyTestRpcNoInputIdentifier, null);

        // Test close

        reg.close();

        try {
            legacyRpcRouter.invokeRpc(legacyTestRpcIdentifier.getType(), RPC_INPUT).get();
            fail("Expected exception");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof DOMRpcImplementationNotAvailableException);
        }
    }

    @Test
    public void testLegacyRegistrationAndMdsalInvocation() throws InterruptedException, ExecutionException {
        legacyRpcRouter.registerRpcImplementation(testLegacyRpcImpl, legacyTestRpcIdentifier,
                legacyTestRpcNoInputIdentifier);

        // Test success

        DefaultDOMRpcResult result = new DefaultDOMRpcResult(RPC_OUTPUT,
                Collections.singleton(RpcResultBuilder.newError(ErrorType.RPC, "tag", "message")));
        testLegacyRpcImpl.init(Futures.immediateCheckedFuture(result));

        ListenableFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult> future =
                mdsalRpcRouter.getRpcService().invokeRpc(mdsalTestRpcIdentifier.getType(), RPC_INPUT);

        assertEquals(RPC_OUTPUT, future.get().getResult());
        assertEquals(1, future.get().getErrors().size());
        assertEquals(ErrorType.RPC, future.get().getErrors().iterator().next().getErrorType());
        assertEquals("tag", future.get().getErrors().iterator().next().getTag());
        assertEquals("message", future.get().getErrors().iterator().next().getMessage());
        testLegacyRpcImpl.verifyInput(legacyTestRpcIdentifier, RPC_INPUT);

        // Test exception returned

        TestLegacyDOMRpcException rpcEx = new TestLegacyDOMRpcException();
        testLegacyRpcImpl.init(Futures.immediateFailedCheckedFuture(rpcEx));

        try {
            mdsalRpcRouter.getRpcService().invokeRpc(mdsalTestRpcIdentifier.getType(), RPC_INPUT).get();
            fail("Expected exception");
        } catch (ExecutionException e) {
            assertEquals(rpcEx, e.getCause());
        }

        // Test no input or output

        testLegacyRpcImpl.init(Futures.immediateCheckedFuture(null));

        future = mdsalRpcRouter.getRpcService().invokeRpc(mdsalTestRpcNoInputIdentifier.getType(), null);

        assertNull(future.get());
        testLegacyRpcImpl.verifyInput(legacyTestRpcNoInputIdentifier, null);
    }

    @Test
    public void testMdsalRegistrationAndLegacyInvocation() throws InterruptedException, ExecutionException {
        mdsalRpcRouter.getRpcProviderService().registerRpcImplementation(testMdsalRpcImpl, mdsalTestRpcIdentifier,
                mdsalTestRpcNoInputIdentifier);

        // Test success

        org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult result =
            new org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult(RPC_OUTPUT,
                Collections.singleton(RpcResultBuilder.newError(ErrorType.RPC, "tag", "message")));
        testMdsalRpcImpl.init(FluentFutures.immediateFluentFuture(result));

        ListenableFuture<DOMRpcResult> future = legacyRpcRouter.invokeRpc(legacyTestRpcIdentifier.getType(), RPC_INPUT);

        assertEquals(RPC_OUTPUT, future.get().getResult());
        assertEquals(1, future.get().getErrors().size());
        assertEquals(ErrorType.RPC, future.get().getErrors().iterator().next().getErrorType());
        assertEquals("tag", future.get().getErrors().iterator().next().getTag());
        assertEquals("message", future.get().getErrors().iterator().next().getMessage());
        testMdsalRpcImpl.verifyInput(mdsalTestRpcIdentifier, RPC_INPUT);

        // Test exception returned

        TestMdsalDOMRpcException rpcEx = new TestMdsalDOMRpcException();
        testMdsalRpcImpl.init(FluentFutures.immediateFailedFluentFuture(rpcEx));

        try {
            legacyRpcRouter.invokeRpc(legacyTestRpcIdentifier.getType(), RPC_INPUT).get();
            fail("Expected exception");
        } catch (ExecutionException e) {
            assertTrue("Unexpected exception " + e.getCause(), e.getCause() instanceof DOMRpcException);
            assertEquals(rpcEx, e.getCause().getCause());
        }

        // Test no input or output

        testMdsalRpcImpl.init(FluentFutures.immediateNullFluentFuture());

        future = legacyRpcRouter.invokeRpc(legacyTestRpcNoInputIdentifier.getType(), null);

        assertNull(future.get());
        testMdsalRpcImpl.verifyInput(mdsalTestRpcNoInputIdentifier, null);
    }

    @Test
    public void testRegisterRpcListener() {
        final TestLegacyDOMRpcImplementation2 testRpcImpl2 = new TestLegacyDOMRpcImplementation2();

        DOMRpcAvailabilityListener listener = mock(DOMRpcAvailabilityListener.class);
        doNothing().when(listener).onRpcAvailable(any());
        doNothing().when(listener).onRpcUnavailable(any());
        doReturn(true).when(listener).acceptsImplementation(any());
        final ListenerRegistration<?> listenerReg = legacyRpcRouter.registerRpcListener(listener);

        DOMRpcAvailabilityListener filteredListener = mock(DOMRpcAvailabilityListener.class);
        doNothing().when(filteredListener).onRpcAvailable(any());
        doNothing().when(filteredListener).onRpcUnavailable(any());
        doReturn(true).when(filteredListener).acceptsImplementation(testLegacyRpcImpl);
        doReturn(false).when(filteredListener).acceptsImplementation(testRpcImpl2);
        final ListenerRegistration<?> filteredListenerReg = legacyRpcRouter.registerRpcListener(filteredListener);

        final DOMRpcImplementationRegistration<?> testRpcReg =
                legacyRpcRouter.registerRpcImplementation(testLegacyRpcImpl, legacyTestRpcIdentifier);

        verify(listener, timeout(5000)).onRpcAvailable(ImmutableList.of(legacyTestRpcIdentifier));
        verify(filteredListener, timeout(5000)).onRpcAvailable(ImmutableList.of(legacyTestRpcIdentifier));

        final DOMRpcImplementationRegistration<?> testRpcNoInputReg =
                legacyRpcRouter.registerRpcImplementation(testRpcImpl2, legacyTestRpcNoInputIdentifier);

        verify(listener, timeout(5000)).onRpcAvailable(ImmutableList.of(legacyTestRpcNoInputIdentifier));
        verify(filteredListener, after(200).never()).onRpcAvailable(ImmutableList.of(legacyTestRpcNoInputIdentifier));

        testRpcReg.close();

        verify(listener, timeout(5000)).onRpcUnavailable(ImmutableList.of(legacyTestRpcIdentifier));
        verify(filteredListener, timeout(5000)).onRpcUnavailable(ImmutableList.of(legacyTestRpcIdentifier));

        testRpcNoInputReg.close();

        verify(listener, timeout(5000)).onRpcUnavailable(ImmutableList.of(legacyTestRpcNoInputIdentifier));
        verify(filteredListener, after(200).never()).onRpcUnavailable(ImmutableList.of(legacyTestRpcNoInputIdentifier));

        reset(listener, filteredListener);

        listenerReg.close();
        filteredListenerReg.close();

        legacyRpcRouter.registerRpcImplementation(testLegacyRpcImpl, legacyTestRpcIdentifier);

        verify(listener, after(200).never()).onRpcAvailable(ImmutableList.of(legacyTestRpcIdentifier));
        verify(filteredListener, never()).onRpcAvailable(ImmutableList.of(legacyTestRpcIdentifier));
    }

    private static SchemaPath findRpc(SchemaContext schemaContext, String name) {
        Module testModule = schemaContext.findModule("odl-datastore-test", TestModel.TEST_QNAME.getRevision()).get();
        RpcDefinition rpcDefinition = null;
        for (RpcDefinition def: testModule.getRpcs()) {
            if (def.getQName().getLocalName().equals(name)) {
                rpcDefinition = def;
                break;
            }
        }

        assertNotNull(name + " rpc not found in " + testModule.getRpcs(), rpcDefinition);
        return rpcDefinition.getPath();
    }

    private abstract static class AbstractDOMRpcImplementation<T> {
        Entry<T, NormalizedNode<?, ?>> rpcInput;

        void verifyInput(T expRpc, NormalizedNode<?, ?> expInput) {
            assertNotNull(rpcInput);
            assertEquals(expRpc, rpcInput.getKey());
            assertEquals(expInput, rpcInput.getValue());
        }
    }

    private static class TestLegacyDOMRpcImplementation extends AbstractDOMRpcImplementation<DOMRpcIdentifier>
            implements DOMRpcImplementation {
        CheckedFuture<DOMRpcResult, DOMRpcException> returnFuture;

        @Override
        public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(
                final DOMRpcIdentifier rpc, final NormalizedNode<?, ?> input) {
            rpcInput = new SimpleEntry<>(rpc, input);
            return returnFuture;
        }

        void init(CheckedFuture<DOMRpcResult, DOMRpcException> retFuture) {
            this.returnFuture = retFuture;
            rpcInput = null;
        }
    }

    private static class TestMdsalDOMRpcImplementation
            extends AbstractDOMRpcImplementation<org.opendaylight.mdsal.dom.api.DOMRpcIdentifier>
            implements org.opendaylight.mdsal.dom.api.DOMRpcImplementation {
        FluentFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult> returnFuture;

        @Override
        public FluentFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult> invokeRpc(
                    final org.opendaylight.mdsal.dom.api.DOMRpcIdentifier rpc, final NormalizedNode<?, ?> input) {
            rpcInput = new SimpleEntry<>(rpc, input);
            return returnFuture;
        }

        void init(FluentFuture<org.opendaylight.mdsal.dom.api.DOMRpcResult> retFuture) {
            this.returnFuture = retFuture;
            rpcInput = null;
        }
    }

    private static class TestLegacyDOMRpcImplementation2 implements DOMRpcImplementation {
        @Override
        public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(
                final DOMRpcIdentifier rpc, final NormalizedNode<?, ?> input) {
            return null;
        }
    }

    private static class TestLegacyDOMRpcException extends DOMRpcException {
        private static final long serialVersionUID = 1L;

        TestLegacyDOMRpcException() {
            super("test");
        }
    }

    private static class TestMdsalDOMRpcException extends org.opendaylight.mdsal.dom.api.DOMRpcException {
        private static final long serialVersionUID = 1L;

        TestMdsalDOMRpcException() {
            super("test");
        }
    }
}
