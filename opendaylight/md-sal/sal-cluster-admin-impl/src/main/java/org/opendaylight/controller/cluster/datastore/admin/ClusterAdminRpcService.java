/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Status.Success;
import org.apache.pekko.dispatch.OnComplete;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.get.known.clients._for.all.shards.output.shard.result.KnownClientsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.locate.shard.output.member.node.LeaderActorRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.locate.shard.output.member.node.LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.member.voting.states.input.MemberVotingState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResultBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.shard.result.output.ShardResultKey;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
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

    private ActorUtils actorUtils(final DataStoreType dataStoreType) {
        return switch (dataStoreType) {
            case Config -> configDataStore.getActorUtils();
            case Operational -> operDataStore.getActorUtils();
        };
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<AddShardReplicaOutput>> addShardReplica(final AddShardReplicaInput input) {
        final var shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultFuture("A valid shard name must be specified");
        }

        final var dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        LOG.info("Adding replica for shard {}", shardName);

        final var ret = SettableFuture.<RpcResult<AddShardReplicaOutput>>create();
        Futures.addCallback(
            this.<Success>sendMessageToShardManager(dataStoreType, new AddShardReplica(shardName)),
            new FutureCallback<>() {
                @Override
                public void onSuccess(final Success success) {
                    LOG.info("Successfully added replica for shard {}", shardName);
                    ret.set(newSuccessfulResult(new AddShardReplicaOutputBuilder().build()));
                }

                @Override
                public void onFailure(final Throwable failure) {
                    onMessageFailure(String.format("Failed to add replica for shard %s", shardName), ret, failure);
                }
            }, MoreExecutors.directExecutor());
        return ret;
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<RemoveShardReplicaOutput>> removeShardReplica(final RemoveShardReplicaInput input) {
        final var shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultFuture("A valid shard name must be specified");
        }

        final var dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        final var memberName = input.getMemberName();
        if (Strings.isNullOrEmpty(memberName)) {
            return newFailedRpcResultFuture("A valid member name must be specified");
        }

        LOG.info("Removing replica for shard {} memberName {}, datastoreType {}", shardName, memberName, dataStoreType);

        final var ret = SettableFuture.<RpcResult<RemoveShardReplicaOutput>>create();

        Futures.addCallback(
            this.<Success>sendMessageToShardManager(dataStoreType,
                new RemoveShardReplica(shardName, MemberName.forName(memberName))),
            new FutureCallback<>() {
                @Override
                public void onSuccess(final Success success) {
                    LOG.info("Successfully removed replica for shard {}", shardName);
                    ret.set(newSuccessfulResult(new RemoveShardReplicaOutputBuilder().build()));
                }

                @Override
                public void onFailure(final Throwable failure) {
                    onMessageFailure("Failed to remove replica for shard " + shardName, ret, failure);
                }
            }, MoreExecutors.directExecutor());

        return ret;
    }

    private ListenableFuture<RpcResult<LocateShardOutput>> locateShard(final LocateShardInput input) {
        final var dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        final var actorUtils = actorUtils(dataStoreType);
        final var ret = SettableFuture.<RpcResult<LocateShardOutput>>create();
        actorUtils.findPrimaryShardAsync(input.getShardName()).onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final PrimaryShardInfo success) throws Throwable {
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

                ret.set(newSuccessfulResult(new LocateShardOutputBuilder()
                    .setMemberNode(new LeaderActorRefBuilder()
                        .setLeaderActorRef(success.getPrimaryShardActor().toSerializationFormat())
                        .build())
                    .build()));
            }
        }, actorUtils.getClientDispatcher());

        return ret;
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<MakeLeaderLocalOutput>> makeLeaderLocal(final MakeLeaderLocalInput input) {
        final var shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultFuture("A valid shard name must be specified");
        }

        final var dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        final var actorUtils = actorUtils(dataStoreType);

        LOG.info("Moving leader to local node {} for shard {}, datastoreType {}",
                actorUtils.getCurrentMemberName().getName(), shardName, dataStoreType);

        final var makeLeaderLocalAsk = org.apache.pekko.dispatch.Futures.<Object>promise();
        actorUtils.findLocalShardAsync(shardName).onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable failure, final ActorRef actorRef) {
                if (failure != null) {
                    LOG.warn("No local shard found for {} datastoreType {} - Cannot request leadership transfer to"
                            + " local shard.", shardName, dataStoreType, failure);
                    makeLeaderLocalAsk.failure(failure);
                } else {
                    makeLeaderLocalAsk.completeWith(
                        actorUtils.executeOperationAsync(actorRef, MakeLeaderLocal.INSTANCE, makeLeaderLocalTimeout));
                }
            }
        }, actorUtils.getClientDispatcher());

        final var ret = SettableFuture.<RpcResult<MakeLeaderLocalOutput>>create();
        makeLeaderLocalAsk.future().onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object success) {
                if (failure != null) {
                    LOG.error("Leadership transfer failed for shard {}.", shardName, failure);
                    ret.set(RpcResultBuilder.<MakeLeaderLocalOutput>failed()
                        .withError(ErrorType.APPLICATION, "leadership transfer failed", failure)
                        .build());
                } else {
                    LOG.debug("Leadership transfer complete");
                    ret.set(RpcResultBuilder.success(new MakeLeaderLocalOutputBuilder().build()).build());
                }
            }
        }, actorUtils.getClientDispatcher());

        return ret;
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
        final var shardName = input.getShardName();
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

        final var ret = SettableFuture.<RpcResult<ChangeMemberVotingStatesForShardOutput>>create();
        Futures.addCallback(
            this.<Success>sendMessageToShardManager(dataStoreType, changeVotingStatus),
            new FutureCallback<>() {
                @Override
                public void onSuccess(final Success success) {
                    LOG.info("Successfully changed member voting states for shard {}", shardName);
                    ret.set(newSuccessfulResult(new ChangeMemberVotingStatesForShardOutputBuilder().build()));
                }

                @Override
                public void onFailure(final Throwable failure) {
                    onMessageFailure(String.format("Failed to change member voting states for shard %s", shardName),
                        ret, failure);
                }
            }, MoreExecutors.directExecutor());
        return ret;
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
        final var shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return newFailedRpcResultFuture("A valid shard name must be specified");
        }

        final var dataStoreType = input.getDataStoreType();
        if (dataStoreType == null) {
            return newFailedRpcResultFuture("A valid DataStoreType must be specified");
        }

        LOG.info("Getting role for shard {}, datastore type {}", shardName, dataStoreType);

        final var ret = SettableFuture.<RpcResult<GetShardRoleOutput>>create();
        Futures.addCallback(
            this.<GetShardRoleReply>sendMessageToShardManager(dataStoreType, new GetShardRole(shardName)),
            new FutureCallback<>() {
                @Override
                public void onSuccess(final GetShardRoleReply reply) {
                    if (reply == null) {
                        ret.set(ClusterAdminRpcService.<GetShardRoleOutput>newFailedRpcResultBuilder(
                            "No Shard role present. Please retry..").build());
                        return;
                    }

                    final var role = reply.getRole();
                    LOG.info("Successfully received role:{} for shard {}", role, shardName);
                    ret.set(newSuccessfulResult(new GetShardRoleOutputBuilder().setRole(role).build()));
                }

                @Override
                public void onFailure(final Throwable failure) {
                    ret.set(ClusterAdminRpcService.<GetShardRoleOutput>newFailedRpcResultBuilder(
                        "Failed to get shard role.", failure).build());
                }
            }, MoreExecutors.directExecutor());

        return ret;
    }

    @VisibleForTesting
    ListenableFuture<RpcResult<BackupDatastoreOutput>> backupDatastore(final BackupDatastoreInput input) {
        LOG.debug("backupDatastore: {}", input);

        if (Strings.isNullOrEmpty(input.getFilePath())) {
            return newFailedRpcResultFuture("A valid file path must be specified");
        }

        final var timeout = input.getTimeout();
        final var opTimeout = timeout != null ? Timeout.apply(timeout.longValue(), TimeUnit.SECONDS)
                : SHARD_MGR_TIMEOUT;

        final var message = new GetSnapshot(opTimeout);

        final var ret = SettableFuture.<RpcResult<BackupDatastoreOutput>>create();
        Futures.addCallback(Futures.<DatastoreSnapshot>allAsList(
            sendMessageToShardManager(DataStoreType.Config, message),
            sendMessageToShardManager(DataStoreType.Operational, message)),
            new FutureCallback<>() {
                @Override
                public void onSuccess(final List<DatastoreSnapshot> snapshots) {
                    saveSnapshotsToFile(new DatastoreSnapshotList(snapshots), input.getFilePath(), ret);
                }

                @Override
                public void onFailure(final Throwable failure) {
                    onDatastoreBackupFailure(input.getFilePath(), ret, failure);
                }
            }, MoreExecutors.directExecutor());
        return ret;
    }

    private ListenableFuture<RpcResult<GetKnownClientsForAllShardsOutput>> getKnownClientsForAllShards(
            final GetKnownClientsForAllShardsInput input) {
        final var builder = ImmutableMap.<ShardIdentifier, ListenableFuture<GetKnownClientsReply>>builder();

        for (var type : DataStoreType.values()) {
            final var utils = actorUtils(type);

            for (var shardName : utils.getConfiguration().getAllShardNames()) {
                final var future = SettableFuture.<GetKnownClientsReply>create();
                builder.put(new ShardIdentifier(type, shardName), future);

                final var disp = utils.getClientDispatcher();
                utils.findPrimaryShardAsync(shardName)
                    .flatMap(info ->
                        Patterns.ask(info.getPrimaryShardActor(), GetKnownClients.INSTANCE, SHARD_MGR_TIMEOUT), disp)
                    .onComplete(new OnComplete<>() {
                        @Override
                        public void onComplete(final Throwable failure, final Object success) {
                            if (failure == null) {
                                future.set((GetKnownClientsReply) success);
                            } else {
                                future.setException(failure);
                            }
                        }
                    }, disp);
            }
        }

        final var allShardReplies = builder.build();
        return Futures.whenAllComplete(allShardReplies.values()).call(() -> processReplies(allShardReplies),
            MoreExecutors.directExecutor());
    }

    private ListenableFuture<RpcResult<ActivateEosDatacenterOutput>> activateEosDatacenter(
            final ActivateEosDatacenterInput input) {
        LOG.debug("Activating EOS Datacenter");
        final var ret = SettableFuture.<RpcResult<ActivateEosDatacenterOutput>>create();
        Futures.addCallback(dataCenterControl.activateDataCenter(), new FutureCallback<>() {
            @Override
            public void onSuccess(final Empty result) {
                LOG.debug("Successfully activated datacenter.");
                ret.set(RpcResultBuilder.<ActivateEosDatacenterOutput>success().build());
            }

            @Override
            public void onFailure(final Throwable failure) {
                ret.set(ClusterAdminRpcService.<ActivateEosDatacenterOutput>newFailedRpcResultBuilder(
                        "Failed to activate datacenter.", failure).build());
            }
        }, MoreExecutors.directExecutor());
        return ret;
    }

    private ListenableFuture<RpcResult<DeactivateEosDatacenterOutput>> deactivateEosDatacenter(
            final DeactivateEosDatacenterInput input) {
        LOG.debug("Deactivating EOS Datacenter");
        final var ret = SettableFuture.<RpcResult<DeactivateEosDatacenterOutput>>create();
        Futures.addCallback(dataCenterControl.deactivateDataCenter(), new FutureCallback<>() {
            @Override
            public void onSuccess(final Empty result) {
                LOG.debug("Successfully deactivated datacenter.");
                ret.set(RpcResultBuilder.<DeactivateEosDatacenterOutput>success().build());
            }

            @Override
            public void onFailure(final Throwable failure) {
                ret.set(ClusterAdminRpcService.<DeactivateEosDatacenterOutput>newFailedRpcResultBuilder(
                        "Failed to deactivate datacenter.", failure).build());
            }
        }, MoreExecutors.directExecutor());

        return ret;
    }

    private static RpcResult<GetKnownClientsForAllShardsOutput> processReplies(
            final ImmutableMap<ShardIdentifier, ListenableFuture<GetKnownClientsReply>> allShardReplies) {
        final var result = allShardReplies.entrySet().stream()
            .map(entry -> {
                final var shardId = entry.getKey();
                final var future = entry.getValue();
                final var builder = new ShardResultBuilder()
                        .setDataStoreType(shardId.getDataStoreType())
                        .setShardName(shardId.getShardName());

                final GetKnownClientsReply reply;
                try {
                    reply = Futures.getDone(future);
                } catch (ExecutionException e) {
                    LOG.debug("Shard {} failed to answer", entry.getKey(), e);
                    return builder
                        .setSucceeded(Boolean.FALSE)
                        .setErrorMessage(e.getCause().getMessage())
                        .build();
                }

                return builder
                    .setSucceeded(Boolean.TRUE)
                    .addAugmentation(new ShardResult1Builder()
                        .setKnownClients(reply.getClients().stream()
                            .map(client -> new KnownClientsBuilder()
                                .setMember(client.getFrontendId().getMemberName().toYang())
                                .setType(client.getFrontendId().getClientType().toYang())
                                .setGeneration(client.getYangGeneration())
                                .build())
                            .collect(BindingMap.toMap()))
                        .build())
                    .build();
            })
            .collect(BindingMap.toMap());

        return RpcResultBuilder.success(new GetKnownClientsForAllShardsOutputBuilder().setShardResult(result).build())
                .build();
    }

    private static ChangeShardMembersVotingStatus toChangeShardMembersVotingStatus(final String shardName,
            final List<MemberVotingState> memberVotingStatus) {
        return new ChangeShardMembersVotingStatus(shardName, memberVotingStatus.stream()
            .collect(Collectors.toMap(MemberVotingState::getMemberName, MemberVotingState::getVoting)));
    }

    private static <T> SettableFuture<RpcResult<T>> waitForShardResults(
            final List<Entry<ListenableFuture<Success>, ShardResultBuilder>> shardResultData,
            final Function<Map<ShardResultKey, ShardResult>, T> resultDataSupplier,
            final String failureLogMsgPrefix) {
        final var ret = SettableFuture.<RpcResult<T>>create();
        final var shardResults = new HashMap<ShardResultKey, ShardResult>();

        for (var entry : shardResultData) {
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
                        ret.set(newSuccessfulResult(resultDataSupplier.apply(shardResults)));
                    }
                }
            }, MoreExecutors.directExecutor());
        }
        return ret;
    }

    private <T> void sendMessageToManagerForConfiguredShards(final DataStoreType dataStoreType,
            final List<Entry<ListenableFuture<T>, ShardResultBuilder>> shardResultData,
            final Function<String, Object> messageSupplier) {
        final var actorUtils = actorUtils(dataStoreType);
        final var allShardNames = actorUtils.getConfiguration().getAllShardNames();

        LOG.debug("Sending message to all shards {} for data store {}", allShardNames, actorUtils.getDataStoreName());

        for (var shardName: allShardNames) {
            shardResultData.add(Map.entry(
                this.<T>ask(actorUtils.getShardManager(), messageSupplier.apply(shardName), SHARD_MGR_TIMEOUT),
                new ShardResultBuilder().setShardName(shardName).setDataStoreType(dataStoreType)));
        }
    }

    private <T> ListenableFuture<T> sendMessageToShardManager(final DataStoreType dataStoreType, final Object message) {
        return ask(actorUtils(dataStoreType).getShardManager(), message, SHARD_MGR_TIMEOUT);
    }

    private static void saveSnapshotsToFile(final DatastoreSnapshotList snapshots, final String fileName,
            final SettableFuture<RpcResult<BackupDatastoreOutput>> returnFuture) {
        try (var oos = new ObjectOutputStream(Files.newOutputStream(Path.of(fileName)))) {
            oos.writeObject(snapshots);
        } catch (IOException e) {
            onDatastoreBackupFailure(fileName, returnFuture, e);
            return;
        }

        returnFuture.set(newSuccessfulResult(new BackupDatastoreOutputBuilder().build()));
        LOG.info("Successfully backed up datastore to file {}", fileName);
    }

    private static <T> void onDatastoreBackupFailure(final String fileName,
            final SettableFuture<RpcResult<T>> returnFuture, final Throwable failure) {
        onMessageFailure("Failed to back up datastore to file " + fileName, returnFuture, failure);
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
