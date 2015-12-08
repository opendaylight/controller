/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import akka.actor.ActorRef;
import akka.actor.Status.Success;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.messages.AddShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshotList;
import org.opendaylight.controller.cluster.datastore.messages.RemoveShardReplica;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddShardReplicaInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.BackupDatastoreInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ConvertMembersToNonvotingForAllShardsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ConvertMembersToVotingForAllShardsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveShardReplicaInput;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the yang RPCs defined in the generated ClusterAdminService interface.
 *
 * @author Thomas Pantelis
 */
public class ClusterAdminRpcService implements ClusterAdminService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAdminRpcService.class);

    private final DistributedDataStore configDataStore;
    private final DistributedDataStore operDataStore;
    private RpcRegistration<ClusterAdminService> rpcRegistration;

    public ClusterAdminRpcService(DistributedDataStore configDataStore, DistributedDataStore operDataStore) {
        this.configDataStore = configDataStore;
        this.operDataStore = operDataStore;
    }

    public void start(RpcProviderRegistry rpcProviderRegistry) {
        LOG.debug("ClusterAdminRpcService starting");

        rpcRegistration = rpcProviderRegistry.addRpcImplementation(ClusterAdminService.class, this);
    }

    @Override
    public void close() {
        if(rpcRegistration != null) {
            rpcRegistration.close();
        }
    }

    @Override
    public Future<RpcResult<Void>> addShardReplica(final AddShardReplicaInput input) {
        final String shardName = input.getShardName();
        if(Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultBuilder("A valid shard name must be specified").buildFuture();
        }

        DataStoreType dataStoreType = input.getDataStoreType();
        if(dataStoreType == null) {
            return newFailedRpcResultBuilder("A valid DataStoreType must be specified").buildFuture();
        }

        LOG.info("Adding replica for shard {}", shardName);

        final SettableFuture<RpcResult<Void>> returnFuture = SettableFuture.create();
        ListenableFuture<Success> future = sendMessageToShardManager(dataStoreType, new AddShardReplica(shardName));
        Futures.addCallback(future, new FutureCallback<Success>() {
            @Override
            public void onSuccess(Success success) {
                LOG.info("Successfully added replica for shard {}", shardName);
                returnFuture.set(newSuccessfulResult());
            }

            @Override
            public void onFailure(Throwable failure) {
                onMessageFailure(String.format("Failed to add replica for shard %s", shardName),
                        returnFuture, failure);
            }
        });

        return returnFuture;
    }

    @Override
    public Future<RpcResult<Void>> removeShardReplica(RemoveShardReplicaInput input) {
        final String shardName = input.getShardName();
        if(Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultBuilder("A valid shard name must be specified").buildFuture();
        }

        DataStoreType dataStoreType = input.getDataStoreType();
        if(dataStoreType == null) {
            return newFailedRpcResultBuilder("A valid DataStoreType must be specified").buildFuture();
        }

        final String memberName = input.getMemberName();
        if(Strings.isNullOrEmpty(memberName)) {
            return newFailedRpcResultBuilder("A valid member name must be specified").buildFuture();
        }

        LOG.info("Removing replica for shard {} memberName {}, datastoreType {}", shardName, memberName, dataStoreType);


        final SettableFuture<RpcResult<Void>> returnFuture = SettableFuture.create();
        ListenableFuture<Success> future = sendMessageToShardManager(dataStoreType,
                new RemoveShardReplica(shardName, memberName, dataStoreType.name()));
        Futures.addCallback(future, new FutureCallback<Success>() {
            @Override
            public void onSuccess(Success success) {
                LOG.info("Successfully removed replica for shard {}", shardName);
                returnFuture.set(newSuccessfulResult());
            }

            @Override
            public void onFailure(Throwable failure) {
                onMessageFailure(String.format("Failed to remove replica for shard %s", shardName),
                        returnFuture, failure);
            }
        });

        return returnFuture;
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
    public Future<RpcResult<Void>> convertMembersToVotingForAllShards(ConvertMembersToVotingForAllShardsInput input) {
        // TODO implement
        return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "operation-not-supported",
                "Not implemented yet").buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> convertMembersToNonvotingForAllShards(
            ConvertMembersToNonvotingForAllShardsInput input) {
        // TODO implement
        return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION, "operation-not-supported",
                "Not implemented yet").buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> backupDatastore(final BackupDatastoreInput input) {
        LOG.debug("backupDatastore: {}", input);

        if(Strings.isNullOrEmpty(input.getFilePath())) {
            return newFailedRpcResultBuilder("A valid file path must be specified").buildFuture();
        }

        final SettableFuture<RpcResult<Void>> returnFuture = SettableFuture.create();
        ListenableFuture<List<DatastoreSnapshot>> future = sendMessageToShardManagers(GetSnapshot.INSTANCE);
        Futures.addCallback(future, new FutureCallback<List<DatastoreSnapshot>>() {
            @Override
            public void onSuccess(List<DatastoreSnapshot> snapshots) {
                saveSnapshotsToFile(new DatastoreSnapshotList(snapshots), input.getFilePath(), returnFuture);
            }

            @Override
            public void onFailure(Throwable failure) {
                onDatastoreBackupFailure(input.getFilePath(), returnFuture, failure);
            }
        });

        return returnFuture;
    }

    @SuppressWarnings("unchecked")
    private <T> ListenableFuture<List<T>> sendMessageToShardManagers(Object message) {
        Timeout timeout = new Timeout(1, TimeUnit.MINUTES);
        ListenableFuture<T> configFuture = ask(configDataStore.getActorContext().getShardManager(), message, timeout);
        ListenableFuture<T> operFuture = ask(operDataStore.getActorContext().getShardManager(), message, timeout);

        return Futures.allAsList(configFuture, operFuture);
    }

    private <T> ListenableFuture<T> sendMessageToShardManager(DataStoreType dataStoreType, Object message) {
        ActorRef shardManager = dataStoreType == DataStoreType.Config ?
                configDataStore.getActorContext().getShardManager() : operDataStore.getActorContext().getShardManager();
        return ask(shardManager, message, new Timeout(1, TimeUnit.MINUTES));
    }

    private static void saveSnapshotsToFile(DatastoreSnapshotList snapshots, String fileName,
            SettableFuture<RpcResult<Void>> returnFuture) {
        try(FileOutputStream fos = new FileOutputStream(fileName)) {
            SerializationUtils.serialize(snapshots, fos);

            returnFuture.set(newSuccessfulResult());
            LOG.info("Successfully backed up datastore to file {}", fileName);
        } catch(Exception e) {
            onDatastoreBackupFailure(fileName, returnFuture, e);
        }
    }

    private static void onDatastoreBackupFailure(String fileName, SettableFuture<RpcResult<Void>> returnFuture,
            Throwable failure) {
        onMessageFailure(String.format("Failed to back up datastore to file %s", fileName), returnFuture, failure);
    }

    private static void onMessageFailure(String msg, final SettableFuture<RpcResult<Void>> returnFuture,
            Throwable failure) {
        LOG.error(msg, failure);
        returnFuture.set(newFailedRpcResultBuilder(String.format("%s: %s", msg, failure.getMessage())).build());
    }

    private <T> ListenableFuture<T> ask(ActorRef actor, Object message, Timeout timeout) {
        final SettableFuture<T> returnFuture = SettableFuture.create();

        @SuppressWarnings("unchecked")
        scala.concurrent.Future<T> askFuture = (scala.concurrent.Future<T>) Patterns.ask(actor, message, timeout);
        askFuture.onComplete(new OnComplete<T>() {
            @Override
            public void onComplete(Throwable failure, T resp) {
                if(failure != null) {
                    returnFuture.setException(failure);
                } else {
                    returnFuture.set(resp);
                }
            }
        }, configDataStore.getActorContext().getClientDispatcher());

        return returnFuture;
    }

    private static RpcResultBuilder<Void> newFailedRpcResultBuilder(String message) {
        return newFailedRpcResultBuilder(message, null);
    }

    private static RpcResultBuilder<Void> newFailedRpcResultBuilder(String message, Throwable cause) {
        return RpcResultBuilder.<Void>failed().withError(ErrorType.RPC, message, cause);
    }

    private static RpcResult<Void> newSuccessfulResult() {
        return RpcResultBuilder.<Void>success().build();
    }
}
