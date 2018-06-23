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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.AbstractMap.SimpleEntry;
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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Unit tests for DOMRpcRouter.
 *
 * @author Thomas Pantelis
 */
public class DOMRpcRouterTest {

    private final TestRpcImplementation testRpcImpl = new TestRpcImplementation();
    private DOMRpcRouter rpcRouter;
    private DOMRpcIdentifier testRpcIdentifier;
    private DOMRpcIdentifier testRpcNoInputIdentifier;

    @Before
    public void setup() {
        org.opendaylight.mdsal.dom.broker.DOMRpcRouter delegateRouter =
                new org.opendaylight.mdsal.dom.broker.DOMRpcRouter();
        final SchemaContext schemaContext = TestModel.createTestContext();
        delegateRouter.onGlobalContextUpdated(schemaContext);
        rpcRouter = new DOMRpcRouter(delegateRouter, delegateRouter);

        testRpcIdentifier = findRpc(schemaContext, "test-rpc");
        testRpcNoInputIdentifier = findRpc(schemaContext, "test-rpc-no-input");
    }

    @Test
    public void testRegisterAndInvoke() throws InterruptedException, ExecutionException {
        final DOMRpcImplementationRegistration<TestRpcImplementation> reg =
                rpcRouter.registerRpcImplementation(testRpcImpl, testRpcIdentifier, testRpcNoInputIdentifier);

        // Test success

        DefaultDOMRpcResult result = new DefaultDOMRpcResult();
        testRpcImpl.init(Futures.immediateCheckedFuture(result));

        NormalizedNode<?, ?> input = ImmutableNodes.leafNode(
                QName.create(TestModel.TEST_QNAME.getModule(), "input-leaf"), "foo");
        ListenableFuture<DOMRpcResult> future = rpcRouter.invokeRpc(testRpcIdentifier.getType(), input);

        assertSame(result, future.get());
        testRpcImpl.verify(testRpcIdentifier, input);

        // Test exception returned

        TestDOMRpcException rpcEx = new TestDOMRpcException();
        testRpcImpl.init(Futures.immediateFailedCheckedFuture(rpcEx));

        try {
            rpcRouter.invokeRpc(testRpcIdentifier.getType(), input).get();
            fail("Expected exception");
        } catch (ExecutionException e) {
            assertEquals(rpcEx, e.getCause());
        }

        // Test no input or output

        testRpcImpl.init(Futures.immediateCheckedFuture(null));

        future = rpcRouter.invokeRpc(testRpcNoInputIdentifier.getType(), null);

        assertNull(future.get());
        testRpcImpl.verify(testRpcNoInputIdentifier, null);

        // Test close

        reg.close();

        try {
            rpcRouter.invokeRpc(testRpcIdentifier.getType(), input).get();
            fail("Expected exception");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof DOMRpcImplementationNotAvailableException);
        }
    }

    @Test
    public void testRegisterRpcListener() {
        final TestRpcImplementation2 testRpcImpl2 = new TestRpcImplementation2();

        DOMRpcAvailabilityListener listener = mock(DOMRpcAvailabilityListener.class);
        doNothing().when(listener).onRpcAvailable(any());
        doNothing().when(listener).onRpcUnavailable(any());
        doReturn(true).when(listener).acceptsImplementation(any());
        final ListenerRegistration<?> listenerReg = rpcRouter.registerRpcListener(listener);

        DOMRpcAvailabilityListener filteredListener = mock(DOMRpcAvailabilityListener.class);
        doNothing().when(filteredListener).onRpcAvailable(any());
        doNothing().when(filteredListener).onRpcUnavailable(any());
        doReturn(true).when(filteredListener).acceptsImplementation(testRpcImpl);
        doReturn(false).when(filteredListener).acceptsImplementation(testRpcImpl2);
        final ListenerRegistration<?> filteredListenerReg = rpcRouter.registerRpcListener(filteredListener);

        final DOMRpcImplementationRegistration<?> testRpcReg =
                rpcRouter.registerRpcImplementation(testRpcImpl, testRpcIdentifier);

        verify(listener, timeout(5000)).onRpcAvailable(ImmutableList.of(testRpcIdentifier));
        verify(filteredListener, timeout(5000)).onRpcAvailable(ImmutableList.of(testRpcIdentifier));

        final DOMRpcImplementationRegistration<?> testRpcNoInputReg =
                rpcRouter.registerRpcImplementation(testRpcImpl2, testRpcNoInputIdentifier);

        verify(listener, timeout(5000)).onRpcAvailable(ImmutableList.of(testRpcNoInputIdentifier));
        verify(filteredListener, after(200).never()).onRpcAvailable(ImmutableList.of(testRpcNoInputIdentifier));

        testRpcReg.close();

        verify(listener, timeout(5000)).onRpcUnavailable(ImmutableList.of(testRpcIdentifier));
        verify(filteredListener, timeout(5000)).onRpcUnavailable(ImmutableList.of(testRpcIdentifier));

        testRpcNoInputReg.close();

        verify(listener, timeout(5000)).onRpcUnavailable(ImmutableList.of(testRpcNoInputIdentifier));
        verify(filteredListener, after(200).never()).onRpcUnavailable(ImmutableList.of(testRpcNoInputIdentifier));

        reset(listener, filteredListener);

        listenerReg.close();
        filteredListenerReg.close();

        rpcRouter.registerRpcImplementation(testRpcImpl, testRpcIdentifier);

        verify(listener, after(200).never()).onRpcAvailable(ImmutableList.of(testRpcIdentifier));
        verify(filteredListener, never()).onRpcAvailable(ImmutableList.of(testRpcIdentifier));
    }

    private static DOMRpcIdentifier findRpc(SchemaContext schemaContext, String name) {
        Module testModule = schemaContext.findModule("odl-datastore-test", TestModel.TEST_QNAME.getRevision()).get();
        RpcDefinition rpcDefinition = null;
        for (RpcDefinition def: testModule.getRpcs()) {
            if (def.getQName().getLocalName().equals(name)) {
                rpcDefinition = def;
                break;
            }
        }

        assertNotNull(name + " rpc not found in " + testModule.getRpcs(), rpcDefinition);
        return DOMRpcIdentifier.create(rpcDefinition.getPath());
    }

    private static class TestRpcImplementation implements DOMRpcImplementation {
        Entry<DOMRpcIdentifier, NormalizedNode<?, ?>> rpcInvocation;
        CheckedFuture<DOMRpcResult, DOMRpcException> returnFuture;

        @Override
        public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(
                final DOMRpcIdentifier rpc, final NormalizedNode<?, ?> input) {
            rpcInvocation = new SimpleEntry<>(rpc, input);
            return returnFuture;
        }

        void init(CheckedFuture<DOMRpcResult, DOMRpcException> retFuture) {
            this.returnFuture = retFuture;
            rpcInvocation = null;
        }

        void verify(DOMRpcIdentifier expRpc, NormalizedNode<?, ?> expInput) {
            assertNotNull(rpcInvocation);
            assertEquals(expRpc, rpcInvocation.getKey());
            assertEquals(expInput, rpcInvocation.getValue());
        }
    }

    private static class TestRpcImplementation2 implements DOMRpcImplementation {
        @Override
        public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(
                final DOMRpcIdentifier rpc, final NormalizedNode<?, ?> input) {
            return null;
        }
    }

    private static class TestDOMRpcException extends DOMRpcException {
        private static final long serialVersionUID = 1L;

        TestDOMRpcException() {
            super("test");
        }
    }
}
