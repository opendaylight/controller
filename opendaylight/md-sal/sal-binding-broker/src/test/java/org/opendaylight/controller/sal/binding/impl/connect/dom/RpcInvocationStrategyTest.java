/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sal.binding.impl.connect.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

public class RpcInvocationStrategyTest {

    @Mock
    private BindingIndependentMappingService mockMappingService;
    @Mock
    private RpcProvisionRegistry mockbiRpcRegistry;

    private RpcInvocationStrategy rpcInvocationStrategy;
    private ListenableFuture<RpcResult<DataObject>> futureDataObj;
    private ListenableFuture<RpcResult<CompositeNode>> futureCompNode;
    private final RpcError rpcError = mock(RpcError.class);
    private final Collection<RpcError> errors = new ArrayList<RpcError>();

    private final CompositeNode inputInvokeOn = mock(CompositeNode.class);
    private final CompositeNode outputInvokeOn = mock(CompositeNode.class);

    private final DataObject toDataDomInput = mock(DataObject.class);
    private final CompositeNode toDataDomReturn = mock(CompositeNode.class);
    private final CompositeNode invokeRpcResult = mock(CompositeNode.class);

    private final DataObject inputForward = mock(DataObject.class);
    private final DataObject outputForward = mock(DataObject.class);

    private QName mockQName;
    private URI urn;

    private final MockRpcService mockRpcService = new MockRpcService();

    public class MockRpcService implements RpcService {

        public Future<?> rpcnameWithInputNoOutput(final DataObject input) {
            return futureDataObj;
        }

        public Future<RpcResult<DataObject>> rpcnameWithInputWithOutput(final DataObject input) {
            return futureDataObj;
        }

        public Future<RpcResult<DataObject>> rpcnameNoInputWithOutput() {
            return futureDataObj;
        }

        public Future<?> rpcnameNoInputNoOutput() {
            return futureDataObj;
        }
    }

    public RpcInvocationStrategyTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void testInit() throws Exception {
        urn = new URI(new String("urn:a:valid:urn"));
    }

    private void setupForForwardToDom(final boolean hasOutput, final boolean hasInput, final int expectedErrorSize) {

        if (expectedErrorSize > 0) {
            errors.add(rpcError);
        }
        RpcResult<CompositeNode> result = RpcResultBuilder.<CompositeNode>success(invokeRpcResult)
                                                            .withRpcErrors( errors ).build();
        futureCompNode = Futures.immediateFuture(result);
        if( hasInput )
        {
            when(mockMappingService.toDataDom(inputForward)).thenReturn(toDataDomReturn);
        }
        when(mockbiRpcRegistry.invokeRpc(eq(mockQName), any(CompositeNode.class))).thenReturn(
                futureCompNode);
        if (hasOutput) {
            when(
                    mockMappingService.dataObjectFromDataDom(eq(rpcInvocationStrategy
                            .getOutputClass().get()), any(CompositeNode.class))).thenReturn(
                    outputForward);
        }

    }

    private void validateForwardToDomBroker(final ListenableFuture<RpcResult<?>> forwardToDomBroker,
            final boolean expectedSuccess, final DataObject expectedResult, final int expectedErrorSize)
            throws InterruptedException, ExecutionException {
        assertNotNull(forwardToDomBroker);
        assertEquals(expectedSuccess, forwardToDomBroker.get().isSuccessful());
        assertEquals(expectedResult, forwardToDomBroker.get().getResult());
        assertEquals(expectedErrorSize, forwardToDomBroker.get().getErrors().size());
    }

    private void setupTestMethod(final String rpcName, final String testMethodName, final boolean hasInput)
            throws NoSuchMethodException {
        mockQName = QName.create(urn, new Date(0L), new String(rpcName));
        java.lang.reflect.Method rpcMethod = hasInput ? MockRpcService.class.getMethod(rpcName,
                DataObject.class) : MockRpcService.class.getMethod(rpcName);
        rpcInvocationStrategy = new RpcInvocationStrategy(mockQName, rpcMethod, mockMappingService,
                mockbiRpcRegistry);
    }

    /*
     * forwardToDomBroker tests
     */
    @Test
    public void testForwardToDomBroker_WithInputNoOutput() throws Exception {
        setupTestMethod("rpcnameWithInputNoOutput", "testForwardToDomBroker_WithInputNoOutput",
                true);
        setupForForwardToDom(false, true, 0);
        ListenableFuture<RpcResult<?>> forwardToDomBroker = rpcInvocationStrategy
                .forwardToDomBroker(inputForward);

        validateForwardToDomBroker(forwardToDomBroker, true, null, 0);
    }

    @Test
    public void testForwardToDomBroker_WithInputNoOutput_error() throws Exception {
        setupTestMethod("rpcnameWithInputNoOutput",
                "testForwardToDomBroker_WithInputNoOutput_error", true);
        setupForForwardToDom(false, true, 1);
        ListenableFuture<RpcResult<?>> forwardToDomBroker = rpcInvocationStrategy
                .forwardToDomBroker(inputForward);

        validateForwardToDomBroker(forwardToDomBroker, true, null, 1);
    }

    @Test
    public void testForwardToDomBroker_WithInputWithOutput() throws Exception {
        setupTestMethod("rpcnameWithInputWithOutput", "testForwardToDomBroker_WithInputWithOutput",
                true);
        setupForForwardToDom(true, true, 0);
        ListenableFuture<RpcResult<?>> forwardToDomBroker = rpcInvocationStrategy
                .forwardToDomBroker(inputForward);
        validateForwardToDomBroker(forwardToDomBroker, true, outputForward, 0);
    }

    @Test
    public void testForwardToDomBroker_NoInputWithOutput() throws Exception {
        setupTestMethod("rpcnameNoInputWithOutput", "testForwardToDomBroker_NoInputWithOutput",
                false);
        setupForForwardToDom(true, false, 0);
        ListenableFuture<RpcResult<?>> forwardToDomBroker = rpcInvocationStrategy
                .forwardToDomBroker(null);
        validateForwardToDomBroker(forwardToDomBroker, true, outputForward, 0);
    }

    @Test
    public void testForwardToDomBroker_NoInputNoOutput() throws Exception {
        setupTestMethod("rpcnameNoInputNoOutput", "testForwardToDomBroker_NoInputNoOutput", false);
        setupForForwardToDom(false, false, 0);
        ListenableFuture<RpcResult<?>> forwardToDomBroker = rpcInvocationStrategy
                .forwardToDomBroker(null);
        validateForwardToDomBroker(forwardToDomBroker, true, null, 0);
    }

    /*
     * invokeOn Tests
     */
    private void setupRpcResultsWithOutput(final int expectedErrorSize) {
        if (expectedErrorSize > 0) {
            errors.add(rpcError);
        }
        RpcResult<CompositeNode> resultCompNode = RpcResultBuilder.<CompositeNode>success(inputInvokeOn)
                                                                        .withRpcErrors(errors).build();
        futureCompNode = Futures.immediateFuture(resultCompNode);
        RpcResult<DataObject> resultDataObj = RpcResultBuilder.<DataObject>success(toDataDomInput)
                                                                           .withRpcErrors(errors).build();
        futureDataObj = Futures.immediateFuture(resultDataObj);

        when(mockMappingService.toDataDom(toDataDomInput)).thenReturn(outputInvokeOn);
    }

    private void setupRpcResultsNoOutput(final int expectedErrorSize) {
        if (expectedErrorSize > 0) {
            errors.add(rpcError);
        }
        RpcResult<CompositeNode> resultCompNode = RpcResultBuilder.<CompositeNode>success(inputInvokeOn)
                                                                          .withRpcErrors(errors).build();
        futureCompNode = Futures.immediateFuture(resultCompNode);
        RpcResult<DataObject> resultDataObj = RpcResultBuilder.<DataObject>success()
                                                                          .withRpcErrors(errors).build();
        futureDataObj = Futures.immediateFuture(resultDataObj);
    }

    private void validateReturnedImmediateFuture(
            final ListenableFuture<RpcResult<CompositeNode>> immediateFuture, final boolean expectedSuccess,
            final CompositeNode expectedReturn, final int expectedErrorSize) throws InterruptedException,
            ExecutionException {
        assertNotNull(immediateFuture);
        assertEquals(expectedSuccess, immediateFuture.get().isSuccessful());
        assertEquals(expectedReturn, immediateFuture.get().getResult());
        assertEquals(expectedErrorSize, immediateFuture.get().getErrors().size());
    }

    @Test
    public void testInvokeOn_NoInputNoOutput() throws Exception {
        setupTestMethod("rpcnameNoInputNoOutput", "testInvokeOn_NoInputNoOutput", false);
        setupRpcResultsNoOutput(0);
        ListenableFuture<RpcResult<CompositeNode>> immediateFuture = Futures
                .immediateFuture(rpcInvocationStrategy.invokeOn(mockRpcService, inputInvokeOn));
        validateReturnedImmediateFuture(immediateFuture, true, null, 0);
    }

    @Test
    public void testInvokeOn_NoInputNoOutput_errors() throws Exception {
        setupTestMethod("rpcnameNoInputNoOutput", "testInvokeOn_NoInputNoOutput", false);
        setupRpcResultsNoOutput(1);
        ListenableFuture<RpcResult<CompositeNode>> immediateFuture = Futures
                .immediateFuture(rpcInvocationStrategy.invokeOn(mockRpcService, inputInvokeOn));
        validateReturnedImmediateFuture(immediateFuture, true, null, 1);
    }

    @Test
    public void testInvokeOn_WithInputNoOutput() throws Exception {
        setupTestMethod("rpcnameWithInputNoOutput", "testInvokeOn_WithInputNoOutput", true);
        setupRpcResultsNoOutput(0);
        ListenableFuture<RpcResult<CompositeNode>> immediateFuture = Futures
                .immediateFuture(rpcInvocationStrategy.invokeOn(mockRpcService, inputInvokeOn));
        validateReturnedImmediateFuture(immediateFuture, true, null, 0);
    }

    @Test
    public void testInvokeOn_WithInputWithOutput() throws Exception {
        setupTestMethod("rpcnameWithInputWithOutput", "testInvokeOn_WithInputWithOutput", true);
        setupRpcResultsWithOutput(0);
        ListenableFuture<RpcResult<CompositeNode>> immediateFuture = Futures
                .immediateFuture(rpcInvocationStrategy.invokeOn(mockRpcService, inputInvokeOn));
        validateReturnedImmediateFuture(immediateFuture, true, outputInvokeOn, 0);
    }

    @Test
    public void testInvokeOn_NoInputWithOutput() throws Exception {
        setupTestMethod("rpcnameNoInputWithOutput", "testInvokeOn_NoInputWithOutput", false);
        setupRpcResultsWithOutput(0);
        ListenableFuture<RpcResult<CompositeNode>> immediateFuture = Futures
                .immediateFuture(rpcInvocationStrategy.invokeOn(mockRpcService, inputInvokeOn));
        validateReturnedImmediateFuture(immediateFuture, true, outputInvokeOn, 0);
    }
}
