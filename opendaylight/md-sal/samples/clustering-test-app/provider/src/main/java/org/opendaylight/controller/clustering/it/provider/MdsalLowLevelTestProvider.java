/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider;

import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Future;
import org.opendaylight.controller.clustering.it.provider.impl.GetConstantService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.AddShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.BecomeModuleLeaderInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.BecomePrefixLeaderInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.IsClientAbortedOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.OdlMdsalLowlevelControlService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.PublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterBoundConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterDefaultConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterSingletonConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemoveShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeYnlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDdtlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsInput;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalLowLevelTestProvider implements OdlMdsalLowlevelControlService {

    private static final Logger LOG = LoggerFactory.getLogger(MdsalLowLevelTestProvider.class);

    private final RpcProviderRegistry rpcRegistry;
    private final BindingAwareBroker.RpcRegistration<OdlMdsalLowlevelControlService> registration;
    private final ClusterSingletonServiceProvider singletonService;
    private final DOMRpcProviderService domRpcService;

    private DOMRpcImplementationRegistration<GetConstantService> globalGetConstantRegistration = null;

    public MdsalLowLevelTestProvider(final RpcProviderRegistry rpcRegistry,
                                     final DOMRpcProviderService domRpcService,
                                     final ClusterSingletonServiceProvider singletonService) {
        this.rpcRegistry = rpcRegistry;
        this.domRpcService = domRpcService;
        this.singletonService = singletonService;

        registration = rpcRegistry.addRpcImplementation(OdlMdsalLowlevelControlService.class, this);
    }

    @Override
    public Future<RpcResult<Void>> unregisterSingletonConstant() {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> publishNotifications(PublishNotificationsInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> subscribeDtcl() {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> writeTransactions(WriteTransactionsInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<IsClientAbortedOutput>> isClientAborted() {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> becomeModuleLeader(BecomeModuleLeaderInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> removeShardReplica(RemoveShardReplicaInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> subscribeYnl(SubscribeYnlInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> becomePrefixLeader(BecomePrefixLeaderInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> unregisterBoundConstant(UnregisterBoundConstantInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> registerSingletonConstant(RegisterSingletonConstantInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> registerDefaultConstant(RegisterDefaultConstantInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> unregisterConstant() {

        if (globalGetConstantRegistration == null) {
            final RpcError rpcError = RpcResultBuilder
                    .newError(ErrorType.APPLICATION, "missing-registration", "No get-constant rpc registration present.");
            final RpcResult<Void> result = RpcResultBuilder.<Void>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        globalGetConstantRegistration.close();
        globalGetConstantRegistration = null;

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<UnregisterFlappingSingletonOutput>> unregisterFlappingSingleton() {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> addShardReplica(AddShardReplicaInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> subscribeDdtl() {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> registerBoundConstant(RegisterBoundConstantInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> registerFlappingSingleton() {
        return null;
    }

    @Override
    public Future<RpcResult<UnsubscribeDtclOutput>> unsubscribeDtcl() {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> deconfigureIdIntsShard() {
        return null;
    }

    @Override
    public Future<RpcResult<UnsubscribeYnlOutput>> unsubscribeYnl(UnsubscribeYnlInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> produceTransactions(ProduceTransactionsInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> registerConstant(final RegisterConstantInput input) {

        LOG.debug("Received register-constant rpc, input: {}", input);

        if (input.getConstant() == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Invalid input.", "Constant value is null");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        if (globalGetConstantRegistration != null) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already a get-constant rpc registered.");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        globalGetConstantRegistration = GetConstantService.registerNew(domRpcService, input.getConstant());
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> unregisterDefaultConstant() {
        return null;
    }

    @Override
    public Future<RpcResult<UnsubscribeDdtlOutput>> unsubscribeDdtl() {
        return null;
    }
}
