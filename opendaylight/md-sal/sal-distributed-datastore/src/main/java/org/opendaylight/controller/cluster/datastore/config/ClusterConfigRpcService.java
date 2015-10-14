/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import java.util.concurrent.Future;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.config.rev151013.AddShardReplicaInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.config.rev151013.ClusterConfigService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.config.rev151013.ConvertToNonvotingMembersForAllShardsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.config.rev151013.ConvertToVotingMembersForAllShardsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.config.rev151013.RemoveShardReplicaInput;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the yang RPCs defined in the generated ClusterConfigService interface.
 *
 * @author Thomas Pantelis
 */
public class ClusterConfigRpcService implements ClusterConfigService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterConfigRpcService.class);

    private final DistributedDataStore configDataStore;
    private final DistributedDataStore operDataStore;
    private RpcRegistration<ClusterConfigService> rpcRegistration;

    public ClusterConfigRpcService(DistributedDataStore configDataStore, DistributedDataStore operDataStore) {
        this.configDataStore = configDataStore;
        this.operDataStore = operDataStore;
    }

    public void start(RpcProviderRegistry rpcProviderRegistry) {
        LOG.debug("ClusterConfigRpcService starting");

        rpcRegistration = rpcProviderRegistry.addRpcImplementation(ClusterConfigService.class, this);
    }

    @Override
    public Future<RpcResult<Void>> addShardReplica(AddShardReplicaInput input) {
        // TODO implement
        return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "operation-not-supported",
                "Not implemented yet").buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> removeShardReplica(RemoveShardReplicaInput input) {
        // TODO implement
        return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "operation-not-supported",
                "Not implemented yet").buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> addReplicasForAllShards() {
        // TODO implement
        return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "operation-not-supported",
                "Not implemented yet").buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> removeAllShardReplicas() {
        // TODO implement
        return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "operation-not-supported",
                "Not implemented yet").buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> convertToNonvotingMembersForAllShards(
            ConvertToNonvotingMembersForAllShardsInput input) {
        // TODO implement
        return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "operation-not-supported",
                "Not implemented yet").buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> convertToVotingMembersForAllShards(ConvertToVotingMembersForAllShardsInput input) {
        // TODO implement
        return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "operation-not-supported",
                "Not implemented yet").buildFuture();
    }

    @Override
    public void close() {
        if(rpcRegistration != null) {
            rpcRegistration.close();
        }
    }
}
