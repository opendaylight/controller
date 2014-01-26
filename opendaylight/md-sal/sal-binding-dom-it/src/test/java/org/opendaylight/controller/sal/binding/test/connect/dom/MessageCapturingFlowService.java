/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import static junit.framework.Assert.assertNotNull;

import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowOutput;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class MessageCapturingFlowService implements SalFlowService, AutoCloseable {

    private Future<RpcResult<AddFlowOutput>> addFlowResult;
    private Future<RpcResult<RemoveFlowOutput>> removeFlowResult;
    private Future<RpcResult<UpdateFlowOutput>> updateFlowResult;

    private final Multimap<InstanceIdentifier<?>, AddFlowInput> receivedAddFlows = HashMultimap.create();
    private final Multimap<InstanceIdentifier<?>, RemoveFlowInput> receivedRemoveFlows = HashMultimap.create();
    private final Multimap<InstanceIdentifier<?>, UpdateFlowInput> receivedUpdateFlows = HashMultimap.create();
    private RoutedRpcRegistration<SalFlowService> registration;

    @Override
    public Future<RpcResult<AddFlowOutput>> addFlow(AddFlowInput arg0) {
        receivedAddFlows.put(arg0.getNode().getValue(), arg0);
        return addFlowResult;
    }

    @Override
    public Future<RpcResult<RemoveFlowOutput>> removeFlow(RemoveFlowInput arg0) {
        receivedRemoveFlows.put(arg0.getNode().getValue(), arg0);
        return removeFlowResult;
    }

    @Override
    public Future<RpcResult<UpdateFlowOutput>> updateFlow(UpdateFlowInput arg0) {
        receivedUpdateFlows.put(arg0.getNode().getValue(), arg0);
        return updateFlowResult;
    }

    public Future<RpcResult<AddFlowOutput>> getAddFlowResult() {
        return addFlowResult;
    }

    public MessageCapturingFlowService setAddFlowResult(Future<RpcResult<AddFlowOutput>> addFlowResult) {
        this.addFlowResult = addFlowResult;
        return this;
    }

    public Future<RpcResult<RemoveFlowOutput>> getRemoveFlowResult() {
        return removeFlowResult;
    }

    public MessageCapturingFlowService setRemoveFlowResult(Future<RpcResult<RemoveFlowOutput>> removeFlowResult) {
        this.removeFlowResult = removeFlowResult;
        return this;
    }

    public Future<RpcResult<UpdateFlowOutput>> getUpdateFlowResult() {
        return updateFlowResult;
    }

    public MessageCapturingFlowService setUpdateFlowResult(Future<RpcResult<UpdateFlowOutput>> updateFlowResult) {
        this.updateFlowResult = updateFlowResult;
        return this;
    }

    public Multimap<InstanceIdentifier<?>, AddFlowInput> getReceivedAddFlows() {
        return receivedAddFlows;
    }

    public Multimap<InstanceIdentifier<?>, RemoveFlowInput> getReceivedRemoveFlows() {
        return receivedRemoveFlows;
    }

    public Multimap<InstanceIdentifier<?>, UpdateFlowInput> getReceivedUpdateFlows() {
        return receivedUpdateFlows;
    }

    public MessageCapturingFlowService registerTo(RpcProviderRegistry registry) {
        registration = registry.addRoutedRpcImplementation(SalFlowService.class, this);
        assertNotNull(registration);
        return this;
    }

    public void close() throws Exception {
        registration.close();
    }

    public MessageCapturingFlowService registerPath(Class<? extends BaseIdentity> context, InstanceIdentifier<?> path) {
        registration.registerPath(context, path);
        return this;
    }

    public MessageCapturingFlowService unregisterPath(Class<? extends BaseIdentity> context, InstanceIdentifier<?> path) {
        registration.unregisterPath(context, path);
        return this;
    }
    
    public static MessageCapturingFlowService create() {
        return new MessageCapturingFlowService();
    }
    
    public static MessageCapturingFlowService create(RpcProviderRegistry registry) {
        MessageCapturingFlowService ret = new MessageCapturingFlowService();
        ret.registerTo(registry);
        return ret;
    }
    
    
}
