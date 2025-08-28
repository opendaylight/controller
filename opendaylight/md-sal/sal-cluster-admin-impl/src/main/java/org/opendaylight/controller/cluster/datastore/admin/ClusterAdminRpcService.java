/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Status.Success;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.messages.AddShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.ChangeShardMembersVotingStatus;
import org.opendaylight.controller.cluster.datastore.messages.FlipShardMembersVotingStatus;
import org.opendaylight.controller.cluster.datastore.messages.GetKnownClients;
import org.opendaylight.controller.cluster.datastore.messages.GetKnownClientsReply;
import org.opendaylight.controller.cluster.datastore.messages.GetShardRole;
import org.opendaylight.controller.cluster.datastore.messages.GetShardRoleReply;
import org.opendaylight.controller.cluster.datastore.messages.MakeLeaderLocal;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.messages.RemoveShardReplica;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.eos.akka.DataCenterControl;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ActivateEosDatacenter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ActivateEosDatacenterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ActivateEosDatacenterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShards;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddShardReplicaInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddShardReplicaOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddShardReplicaOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.BackupDatastore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.BackupDatastoreInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.BackupDatastoreOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.BackupDatastoreOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForAllShards;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForAllShardsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForAllShardsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForAllShardsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForShard;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForShardInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForShardOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ChangeMemberVotingStatesForShardOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DataStoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DeactivateEosDatacenter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DeactivateEosDatacenterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.DeactivateEosDatacenterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.FlipMemberVotingStatesForAllShards;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.FlipMemberVotingStatesForAllShardsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.FlipMemberVotingStatesForAllShardsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.FlipMemberVotingStatesForAllShardsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.GetKnownClientsForAllShards;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.GetKnownClientsForAllShardsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.GetKnownClientsForAllShardsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.GetKnownClientsForAllShardsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.GetShardRoleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.GetShardRoleOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.GetShardRoleOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.LocateShard;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.LocateShardInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.LocateShardOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.LocateShardOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.MakeLeaderLocalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.MakeLeaderLocalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.MakeLeaderLocalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicas;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveShardReplicaInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveShardReplicaOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveShardReplicaOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.get.known.clients._for.all.shards.output.ShardResult1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.get.known.clients._for.all.shards.output.shard.result.KnownClients;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.get.known.clients._for.all.shards.output.shard.result.KnownClientsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.locate.shard.output.member.node.LeaderActorRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.locate.shard.output.member.node.LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.member.voting.states.input.MemberVotingState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResultBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResultKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Implements the yang RPCs defined in the generated ClusterAdminService interface.
 *
 * @author Thomas Pantelis
 */
public final class ClusterAdminRpcService {
    private static final Timeout SHARD_MGR_TIMEOUT = new Timeout(1, TimeUnit.MINUTES);

    private static final Logger LOG = LoggerFactory.getLogger(ClusterAdminRpcService.class);
    private static final @NonNull RpcResult<LocateShardOutput> LOCAL_SHARD_RESULT =
            RpcResultBuilder.success(new LocateShardOutputBuilder()
                .setMemberNode(new LocalBuilder().setLocal(Empty.value()).build())
                .build())
            .build();

    private final DistributedDataStoreInterface configDataStore;
    private final DistributedDataStoreInterface operDataStore;
    private final Timeout makeLeaderLocalTimeout;
    private final DataCenterControl dataCenterControl;

    public ClusterAdminRpcService(final DistributedDataStoreInterface configDataStore,
                                  final DistributedDataStoreInterface operDataStore,
                                  final DataCenterControl dataCenterControl) {
        this.configDataStore = configDataStore;
        this.operDataStore = operDataStore;

        makeLeaderLocalTimeout =
                new Timeout(configDataStore.getActorUtils().getDatastoreContext()
                        .getShardLeaderElectionTimeout().duration().$times(2));

        this.dataCenterControl = dataCenterControl;
    }

    Registration registerWith(final RpcProviderService rpcProviderService) {
        return rpcProviderService.registerRpcImplementations(
            (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013
                .AddShardReplica) this::addShardReplica,
            (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013
                .RemoveShardReplica) this::removeShardReplica,
            (LocateShard) this::locateShard,
            (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013
                .MakeLeaderLocal) this::makeLeaderLocal,
            (AddReplicasForAllShards) this::addReplicasForAllShards,
            (RemoveAllShardReplicas) this::removeAllShardReplicas,
            (ChangeMemberVotingStatesForShard) this::changeMemberVotingStatesForShard,
            (ChangeMemberVotingStatesForAllShards) this::changeMemberVotingStatesForAllShards,
            (FlipMemberVotingStatesForAllShards) this::flipMemberVotingStatesForAllShards,
            (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013
                .GetShardRole) this::getShardRole,
            (BackupDatastore) this::backupDatastore,
            (GetKnownClientsForAllShards) this::getKnownClientsForAllShards,
            (ActivateEosDatacenter) this::activateEosDatacenter,
            (DeactivateEosDatacenter) this::deactivateEosDatacenter);
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<AddShardReplicaOutput>> addShardReplica(final AddShardReplicaInput input) {
        final String shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultFuture("A valid shard name must be specified");
        }

        DataStoreType dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        LOG.info("Adding replica for shard {}", shardName);

        final var returnFuture = SettableFuture.<RpcResult<AddShardReplicaOutput>>create();
        Futures.addCallback(sendMessageToShardManager(dataStoreType, new AddShardReplica(shardName)),
            new FutureCallback<Success>() {
                @Override
                public void onSuccess(final Success success) {
                    LOG.info("Successfully added replica for shard {}", shardName);
                    returnFuture.set(newSuccessfulResult(new AddShardReplicaOutputBuilder().build()));
                }

                @Override
                public void onFailure(final Throwable failure) {
                    onMessageFailure(String.format("Failed to add replica for shard %s", shardName),
                        returnFuture, failure);
                }
            }, MoreExecutors.directExecutor());

        return returnFuture;
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<RemoveShardReplicaOutput>> removeShardReplica(final RemoveShardReplicaInput input) {
        final String shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultFuture("A valid shard name must be specified");
        }

        DataStoreType dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        final String memberName = input.getMemberName();
        if (Strings.isNullOrEmpty(memberName)) {
            return newFailedRpcResultFuture("A valid member name must be specified");
        }

        LOG.info("Removing replica for shard {} memberName {}, datastoreType {}", shardName, memberName, dataStoreType);

        final SettableFuture<RpcResult<RemoveShardReplicaOutput>> returnFuture = SettableFuture.create();
        ListenableFuture<Success> future = sendMessageToShardManager(dataStoreType,
                new RemoveShardReplica(shardName, MemberName.forName(memberName)));
        Futures.addCallback(future, new FutureCallback<Success>() {
            @Override
            public void onSuccess(final Success success) {
                LOG.info("Successfully removed replica for shard {}", shardName);
                returnFuture.set(newSuccessfulResult(new RemoveShardReplicaOutputBuilder().build()));
            }

            @Override
            public void onFailure(final Throwable failure) {
                onMessageFailure(String.format("Failed to remove replica for shard %s", shardName),
                        returnFuture, failure);
            }
        }, MoreExecutors.directExecutor());

        return returnFuture;
    }

    private ListenableFuture<RpcResult<LocateShardOutput>> locateShard(final LocateShardInput input) {
        final ActorUtils utils;
        switch (input.getDataStoreType()) {
            case Config:
                utils = configDataStore.getActorUtils();
                break;
            case Operational:
                utils = operDataStore.getActorUtils();
                break;
            default:
                return newFailedRpcResultFuture("Unhandled datastore in " + input);
        }

        final SettableFuture<RpcResult<LocateShardOutput>> ret = SettableFuture.create();
        utils.findPrimaryShardAsync(input.getShardName()).onComplete(new OnComplete<PrimaryShardInfo>() {
            @Override
            public void onComplete(final Throwable failure, final PrimaryShardInfo success) {
                if (failure != null) {
                    LOG.debug("Failed to find shard for {}", input, failure);
                    ret.setException(failure);
                    return;
                }

                // Data tree implies local leak
                if (success.getLocalShardDataTree().isPresent()) {
                    ret.set(LOCAL_SHARD_RESULT);
                    return;
                }

                final ActorSelection actorPath = success.getPrimaryShardActor();
                ret.set(newSuccessfulResult(new LocateShardOutputBuilder()
                    .setMemberNode(new LeaderActorRefBuilder()
                        .setLeaderActorRef(actorPath.toSerializationFormat())
                        .build())
                    .build()));
            }
        }, utils.getClientDispatcher());

        return ret;
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<MakeLeaderLocalOutput>> makeLeaderLocal(final MakeLeaderLocalInput input) {
        final String shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultFuture("A valid shard name must be specified");
        }

        DataStoreType dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        ActorUtils actorUtils = dataStoreType == DataStoreType.Config
                ? configDataStore.getActorUtils() : operDataStore.getActorUtils();

        LOG.info("Moving leader to local node {} for shard {}, datastoreType {}",
                actorUtils.getCurrentMemberName().getName(), shardName, dataStoreType);

        final Future<ActorRef> localShardReply = actorUtils.findLocalShardAsync(shardName);

        final scala.concurrent.Promise<Object> makeLeaderLocalAsk = akka.dispatch.Futures.promise();
        localShardReply.onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable failure, final ActorRef actorRef) {
                if (failure != null) {
                    LOG.warn("No local shard found for {} datastoreType {} - Cannot request leadership transfer to"
                            + " local shard.", shardName, dataStoreType, failure);
                    makeLeaderLocalAsk.failure(failure);
                } else {
                    makeLeaderLocalAsk
                            .completeWith(actorUtils
                                    .executeOperationAsync(actorRef, MakeLeaderLocal.INSTANCE, makeLeaderLocalTimeout));
                }
            }
        }, actorUtils.getClientDispatcher());

        final SettableFuture<RpcResult<MakeLeaderLocalOutput>> future = SettableFuture.create();
        makeLeaderLocalAsk.future().onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object success) {
                if (failure != null) {
                    LOG.error("Leadership transfer failed for shard {}.", shardName, failure);
                    future.set(RpcResultBuilder.<MakeLeaderLocalOutput>failed().withError(ErrorType.APPLICATION,
                            "leadership transfer failed", failure).build());
                    return;
                }

                LOG.debug("Leadership transfer complete");
                future.set(RpcResultBuilder.success(new MakeLeaderLocalOutputBuilder().build()).build());
            }
        }, actorUtils.getClientDispatcher());

        return future;
    }

    @VisibleForTesting ListenableFuture<RpcResult<AddReplicasForAllShardsOutput>> addReplicasForAllShards(
            final AddReplicasForAllShardsInput input) {
        LOG.info("Adding replicas for all shards");

        final List<Entry<ListenableFuture<Success>, ShardResultBuilder>> shardResultData = new ArrayList<>();

        sendMessageToManagerForConfiguredShards(DataStoreType.Config, shardResultData, AddShardReplica::new);
        sendMessageToManagerForConfiguredShards(DataStoreType.Operational, shardResultData, AddShardReplica::new);

        return waitForShardResults(shardResultData, shardResults ->
                new AddReplicasForAllShardsOutputBuilder().setShardResult(shardResults).build(),
                "Failed to add replica");
    }

    @VisibleForTesting ListenableFuture<RpcResult<RemoveAllShardReplicasOutput>> removeAllShardReplicas(
            final RemoveAllShardReplicasInput input) {
        LOG.info("Removing replicas for all shards");

        final String memberName = input.getMemberName();
        if (Strings.isNullOrEmpty(memberName)) {
            return newFailedRpcResultFuture("A valid member name must be specified");
        }

        final List<Entry<ListenableFuture<Success>, ShardResultBuilder>> shardResultData = new ArrayList<>();
        Function<String, Object> messageSupplier = shardName ->
                new RemoveShardReplica(shardName, MemberName.forName(memberName));

        sendMessageToManagerForConfiguredShards(DataStoreType.Config, shardResultData, messageSupplier);
        sendMessageToManagerForConfiguredShards(DataStoreType.Operational, shardResultData, messageSupplier);

        return waitForShardResults(shardResultData,
            shardResults -> new RemoveAllShardReplicasOutputBuilder().setShardResult(shardResults).build(),
            "       Failed to remove replica");
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<ChangeMemberVotingStatesForShardOutput>> changeMemberVotingStatesForShard(
            final ChangeMemberVotingStatesForShardInput input) {
        final String shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultFuture("A valid shard name must be specified");
        }

        final var dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        final var memberVotingStates = input.getMemberVotingState();
        if (memberVotingStates == null || memberVotingStates.isEmpty()) {
            return newFailedRpcResultFuture("No member voting state input was specified");
        }

        final var changeVotingStatus = toChangeShardMembersVotingStatus(shardName, memberVotingStates);
        LOG.info("Change member voting states for shard {}: {}", shardName,
                changeVotingStatus.getMeberVotingStatusMap());

        final var returnFuture = SettableFuture.<RpcResult<ChangeMemberVotingStatesForShardOutput>>create();
        Futures.addCallback(sendMessageToShardManager(dataStoreType, changeVotingStatus),
            new FutureCallback<Success>() {
                @Override
                public void onSuccess(final Success success) {
                    LOG.info("Successfully changed member voting states for shard {}", shardName);
                    returnFuture.set(newSuccessfulResult(new ChangeMemberVotingStatesForShardOutputBuilder().build()));
                }

                @Override
                public void onFailure(final Throwable failure) {
                    onMessageFailure(String.format("Failed to change member voting states for shard %s", shardName),
                        returnFuture, failure);
                }
            }, MoreExecutors.directExecutor());

        return returnFuture;
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<ChangeMemberVotingStatesForAllShardsOutput>> changeMemberVotingStatesForAllShards(
            final ChangeMemberVotingStatesForAllShardsInput input) {
        List<MemberVotingState> memberVotingStates = input.getMemberVotingState();
        if (memberVotingStates == null || memberVotingStates.isEmpty()) {
            return newFailedRpcResultFuture("No member voting state input was specified");
        }

        final List<Entry<ListenableFuture<Success>, ShardResultBuilder>> shardResultData = new ArrayList<>();
        Function<String, Object> messageSupplier = shardName ->
                toChangeShardMembersVotingStatus(shardName, memberVotingStates);

        LOG.info("Change member voting states for all shards");

        sendMessageToManagerForConfiguredShards(DataStoreType.Config, shardResultData, messageSupplier);
        sendMessageToManagerForConfiguredShards(DataStoreType.Operational, shardResultData, messageSupplier);

        return waitForShardResults(shardResultData, shardResults ->
                new ChangeMemberVotingStatesForAllShardsOutputBuilder().setShardResult(shardResults).build(),
                "Failed to change member voting states");
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<FlipMemberVotingStatesForAllShardsOutput>> flipMemberVotingStatesForAllShards(
            final FlipMemberVotingStatesForAllShardsInput input) {
        final var shardResultData = new ArrayList<Entry<ListenableFuture<Success>, ShardResultBuilder>>();
        final Function<String, Object> messageSupplier = FlipShardMembersVotingStatus::new;

        LOG.info("Flip member voting states for all shards");

        sendMessageToManagerForConfiguredShards(DataStoreType.Config, shardResultData, messageSupplier);
        sendMessageToManagerForConfiguredShards(DataStoreType.Operational, shardResultData, messageSupplier);

        return waitForShardResults(shardResultData, shardResults ->
                new FlipMemberVotingStatesForAllShardsOutputBuilder().setShardResult(shardResults).build(),
                "Failed to change member voting states");
    }

    private ListenableFuture<RpcResult<GetShardRoleOutput>> getShardRole(final GetShardRoleInput input) {
        final String shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultFuture("A valid shard name must be specified");
        }

        DataStoreType dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        LOG.info("Getting role for shard {}, datastore type {}", shardName, dataStoreType);

        final SettableFuture<RpcResult<GetShardRoleOutput>> returnFuture = SettableFuture.create();
        ListenableFuture<GetShardRoleReply> future = sendMessageToShardManager(dataStoreType,
                new GetShardRole(shardName));
        Futures.addCallback(future, new FutureCallback<GetShardRoleReply>() {
            @Override
            public void onSuccess(final GetShardRoleReply reply) {
                if (reply == null) {
                    returnFuture.set(ClusterAdminRpcService.<GetShardRoleOutput>newFailedRpcResultBuilder(
                            "No Shard role present. Please retry..").build());
                    return;
                }
                LOG.info("Successfully received role:{} for shard {}", reply.getRole(), shardName);
                final GetShardRoleOutputBuilder builder = new GetShardRoleOutputBuilder();
                if (reply.getRole() != null) {
                    builder.setRole(reply.getRole());
                }
                returnFuture.set(newSuccessfulResult(builder.build()));
            }

            @Override
            public void onFailure(final Throwable failure) {
                returnFuture.set(ClusterAdminRpcService.<GetShardRoleOutput>newFailedRpcResultBuilder(
                        "Failed to get shard role.", failure).build());
            }
        }, MoreExecutors.directExecutor());

        return returnFuture;
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<BackupDatastoreOutput>> backupDatastore(final BackupDatastoreInput input) {
        LOG.debug("backupDatastore: {}", input);

        if (Strings.isNullOrEmpty(input.getFilePath())) {
            return newFailedRpcResultFuture("A valid file path must be specified");
        }

        final Uint32 timeout = input.getTimeout();
        final Timeout opTimeout = timeout != null ? Timeout.apply(timeout.longValue(), TimeUnit.SECONDS)
                : SHARD_MGR_TIMEOUT;

        final SettableFuture<RpcResult<BackupDatastoreOutput>> returnFuture = SettableFuture.create();
        ListenableFuture<List<DatastoreSnapshot>> future = sendMessageToShardManagers(new GetSnapshot(opTimeout));
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(final List<DatastoreSnapshot> snapshots) {
                saveSnapshotsToFile(new DatastoreSnapshotList(snapshots), input.getFilePath(), returnFuture);
            }

            @Override
            public void onFailure(final Throwable failure) {
                onDatastoreBackupFailure(input.getFilePath(), returnFuture, failure);
            }
        }, MoreExecutors.directExecutor());

        return returnFuture;
    }

    private ListenableFuture<RpcResult<GetKnownClientsForAllShardsOutput>> getKnownClientsForAllShards(
            final GetKnownClientsForAllShardsInput input) {
        final ImmutableMap<ShardIdentifier, ListenableFuture<GetKnownClientsReply>> allShardReplies =
                getAllShardLeadersClients();
        return Futures.whenAllComplete(allShardReplies.values()).call(() -> processReplies(allShardReplies),
            MoreExecutors.directExecutor());
    }

    private ListenableFuture<RpcResult<ActivateEosDatacenterOutput>> activateEosDatacenter(
            final ActivateEosDatacenterInput input) {
        LOG.debug("Activating EOS Datacenter");
        final SettableFuture<RpcResult<ActivateEosDatacenterOutput>> future = SettableFuture.create();
        Futures.addCallback(dataCenterControl.activateDataCenter(), new FutureCallback<>() {
            @Override
            public void onSuccess(final Empty result) {
                LOG.debug("Successfully activated datacenter.");
                future.set(RpcResultBuilder.<ActivateEosDatacenterOutput>success().build());
            }

            @Override
            public void onFailure(final Throwable failure) {
                future.set(ClusterAdminRpcService.<ActivateEosDatacenterOutput>newFailedRpcResultBuilder(
                        "Failed to activate datacenter.", failure).build());
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    private ListenableFuture<RpcResult<DeactivateEosDatacenterOutput>> deactivateEosDatacenter(
            final DeactivateEosDatacenterInput input) {
        LOG.debug("Deactivating EOS Datacenter");
        final SettableFuture<RpcResult<DeactivateEosDatacenterOutput>> future = SettableFuture.create();
        Futures.addCallback(dataCenterControl.deactivateDataCenter(), new FutureCallback<>() {
            @Override
            public void onSuccess(final Empty result) {
                LOG.debug("Successfully deactivated datacenter.");
                future.set(RpcResultBuilder.<DeactivateEosDatacenterOutput>success().build());
            }

            @Override
            public void onFailure(final Throwable failure) {
                future.set(ClusterAdminRpcService.<DeactivateEosDatacenterOutput>newFailedRpcResultBuilder(
                        "Failed to deactivate datacenter.", failure).build());
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    private static RpcResult<GetKnownClientsForAllShardsOutput> processReplies(
            final ImmutableMap<ShardIdentifier, ListenableFuture<GetKnownClientsReply>> allShardReplies) {
        final Map<ShardResultKey, ShardResult> result = Maps.newHashMapWithExpectedSize(allShardReplies.size());
        for (Entry<ShardIdentifier, ListenableFuture<GetKnownClientsReply>> entry : allShardReplies.entrySet()) {
            final ListenableFuture<GetKnownClientsReply> future = entry.getValue();
            final ShardResultBuilder builder = new ShardResultBuilder()
                    .setDataStoreType(entry.getKey().getDataStoreType())
                    .setShardName(entry.getKey().getShardName());

            final GetKnownClientsReply reply;
            try {
                reply = Futures.getDone(future);
            } catch (ExecutionException e) {
                LOG.debug("Shard {} failed to answer", entry.getKey(), e);
                final ShardResult sr = builder
                        .setSucceeded(Boolean.FALSE)
                        .setErrorMessage(e.getCause().getMessage())
                        .build();
                result.put(sr.key(), sr);
                continue;
            }

            final ShardResult sr = builder
                    .setSucceeded(Boolean.TRUE)
                    .addAugmentation(new ShardResult1Builder()
                        .setKnownClients(reply.getClients().stream()
                            .map(client -> new KnownClientsBuilder()
                                .setMember(client.getFrontendId().getMemberName().toYang())
                                .setType(client.getFrontendId().getClientType().toYang())
                                .setGeneration(client.getYangGeneration())
                                .build())
                            .collect(Collectors.toMap(KnownClients::key, Function.identity())))
                        .build())
                    .build();

            result.put(sr.key(), sr);
        }

        return RpcResultBuilder.success(new GetKnownClientsForAllShardsOutputBuilder().setShardResult(result).build())
                .build();
    }

    private static ChangeShardMembersVotingStatus toChangeShardMembersVotingStatus(final String shardName,
            final List<MemberVotingState> memberVotingStatus) {
        Map<String, Boolean> serverVotingStatusMap = new HashMap<>();
        for (MemberVotingState memberStatus: memberVotingStatus) {
            serverVotingStatusMap.put(memberStatus.getMemberName(), memberStatus.getVoting());
        }
        return new ChangeShardMembersVotingStatus(shardName, serverVotingStatusMap);
    }

    private static <T> SettableFuture<RpcResult<T>> waitForShardResults(
            final List<Entry<ListenableFuture<Success>, ShardResultBuilder>> shardResultData,
            final Function<Map<ShardResultKey, ShardResult>, T> resultDataSupplier,
            final String failureLogMsgPrefix) {
        final SettableFuture<RpcResult<T>> returnFuture = SettableFuture.create();
        final Map<ShardResultKey, ShardResult> shardResults = new HashMap<>();
        for (final Entry<ListenableFuture<Success>, ShardResultBuilder> entry : shardResultData) {
            Futures.addCallback(entry.getKey(), new FutureCallback<Success>() {
                @Override
                public void onSuccess(final Success result) {
                    synchronized (shardResults) {
                        final ShardResultBuilder builder = entry.getValue();
                        LOG.debug("onSuccess for shard {}, type {}", builder.getShardName(),
                            builder.getDataStoreType());
                        final ShardResult sr = builder.setSucceeded(Boolean.TRUE).build();
                        shardResults.put(sr.key(), sr);
                        checkIfComplete();
                    }
                }

                @Override
                public void onFailure(final Throwable failure) {
                    synchronized (shardResults) {
                        ShardResultBuilder builder = entry.getValue();
                        LOG.warn("{} for shard {}, type {}", failureLogMsgPrefix, builder.getShardName(),
                                builder.getDataStoreType(), failure);
                        final ShardResult sr = builder
                                .setSucceeded(Boolean.FALSE)
                                .setErrorMessage(Throwables.getRootCause(failure).getMessage())
                                .build();
                        shardResults.put(sr.key(), sr);
                        checkIfComplete();
                    }
                }

                void checkIfComplete() {
                    LOG.debug("checkIfComplete: expected {}, actual {}", shardResultData.size(), shardResults.size());
                    if (shardResults.size() == shardResultData.size()) {
                        returnFuture.set(newSuccessfulResult(resultDataSupplier.apply(shardResults)));
                    }
                }
            }, MoreExecutors.directExecutor());
        }
        return returnFuture;
    }

    private <T> void sendMessageToManagerForConfiguredShards(final DataStoreType dataStoreType,
            final List<Entry<ListenableFuture<T>, ShardResultBuilder>> shardResultData,
            final Function<String, Object> messageSupplier) {
        ActorUtils actorUtils = dataStoreType == DataStoreType.Config ? configDataStore.getActorUtils()
                : operDataStore.getActorUtils();
        Set<String> allShardNames = actorUtils.getConfiguration().getAllShardNames();

        LOG.debug("Sending message to all shards {} for data store {}", allShardNames, actorUtils.getDataStoreName());

        for (String shardName: allShardNames) {
            ListenableFuture<T> future = this.ask(actorUtils.getShardManager(), messageSupplier.apply(shardName),
                                                  SHARD_MGR_TIMEOUT);
            shardResultData.add(new SimpleEntry<>(future,
                    new ShardResultBuilder().setShardName(shardName).setDataStoreType(dataStoreType)));
        }
    }

    private <T> ListenableFuture<List<T>> sendMessageToShardManagers(final Object message) {
        Timeout timeout = SHARD_MGR_TIMEOUT;
        ListenableFuture<T> configFuture = ask(configDataStore.getActorUtils().getShardManager(), message, timeout);
        ListenableFuture<T> operFuture = ask(operDataStore.getActorUtils().getShardManager(), message, timeout);

        return Futures.allAsList(configFuture, operFuture);
    }

    private <T> ListenableFuture<T> sendMessageToShardManager(final DataStoreType dataStoreType, final Object message) {
        ActorRef shardManager = dataStoreType == DataStoreType.Config
                ? configDataStore.getActorUtils().getShardManager()
                        : operDataStore.getActorUtils().getShardManager();
        return ask(shardManager, message, SHARD_MGR_TIMEOUT);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void saveSnapshotsToFile(final DatastoreSnapshotList snapshots, final String fileName,
            final SettableFuture<RpcResult<BackupDatastoreOutput>> returnFuture) {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            SerializationUtils.serialize(snapshots, fos);

            returnFuture.set(newSuccessfulResult(new BackupDatastoreOutputBuilder().build()));
            LOG.info("Successfully backed up datastore to file {}", fileName);
        } catch (IOException | RuntimeException e) {
            onDatastoreBackupFailure(fileName, returnFuture, e);
        }
    }

    private static <T> void onDatastoreBackupFailure(final String fileName,
            final SettableFuture<RpcResult<T>> returnFuture, final Throwable failure) {
        onMessageFailure(String.format("Failed to back up datastore to file %s", fileName), returnFuture, failure);
    }

    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    private static <T> void onMessageFailure(final String msg, final SettableFuture<RpcResult<T>> returnFuture,
            final Throwable failure) {
        LOG.error("{}", msg, failure);
        returnFuture.set(ClusterAdminRpcService.<T>newFailedRpcResultBuilder(String.format("%s: %s", msg,
                failure.getMessage())).build());
    }

    private <T> ListenableFuture<T> ask(final ActorRef actor, final Object message, final Timeout timeout) {
        final SettableFuture<T> returnFuture = SettableFuture.create();

        @SuppressWarnings("unchecked")
        Future<T> askFuture = (Future<T>) Patterns.ask(actor, message, timeout);
        askFuture.onComplete(new OnComplete<T>() {
            @Override
            public void onComplete(final Throwable failure, final T resp) {
                if (failure != null) {
                    returnFuture.setException(failure);
                } else {
                    returnFuture.set(resp);
                }
            }
        }, configDataStore.getActorUtils().getClientDispatcher());

        return returnFuture;
    }

    private ImmutableMap<ShardIdentifier, ListenableFuture<GetKnownClientsReply>> getAllShardLeadersClients() {
        final ImmutableMap.Builder<ShardIdentifier, ListenableFuture<GetKnownClientsReply>> builder =
                ImmutableMap.builder();

        addAllShardsClients(builder, DataStoreType.Config, configDataStore.getActorUtils());
        addAllShardsClients(builder, DataStoreType.Operational, operDataStore.getActorUtils());

        return builder.build();
    }

    private static void addAllShardsClients(
            final ImmutableMap.Builder<ShardIdentifier, ListenableFuture<GetKnownClientsReply>> builder,
            final DataStoreType type, final ActorUtils utils) {
        for (String shardName : utils.getConfiguration().getAllShardNames()) {
            final SettableFuture<GetKnownClientsReply> future = SettableFuture.create();
            builder.put(new ShardIdentifier(type, shardName), future);

            utils.findPrimaryShardAsync(shardName).flatMap(
                info -> Patterns.ask(info.getPrimaryShardActor(), GetKnownClients.INSTANCE, SHARD_MGR_TIMEOUT),
                utils.getClientDispatcher()).onComplete(new OnComplete<>() {
                    @Override
                    public void onComplete(final Throwable failure, final Object success) {
                        if (failure == null) {
                            future.set((GetKnownClientsReply) success);
                        } else {
                            future.setException(failure);
                        }
                    }
                }, utils.getClientDispatcher());
        }
    }

    private static <T> ListenableFuture<RpcResult<T>> newFailedRpcResultFuture(final String message) {
        return ClusterAdminRpcService.<T>newFailedRpcResultBuilder(message).buildFuture();
    }

    private static <T> RpcResultBuilder<T> newFailedRpcResultBuilder(final String message) {
        return newFailedRpcResultBuilder(message, null);
    }

    private static <T> RpcResultBuilder<T> newFailedRpcResultBuilder(final String message, final Throwable cause) {
        return RpcResultBuilder.<T>failed().withError(ErrorType.RPC, message, cause);
    }

    private static <T> RpcResult<T> newSuccessfulResult(final T data) {
        return RpcResultBuilder.success(data).build();
    }
}
