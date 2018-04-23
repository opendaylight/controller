/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider;

import static akka.actor.ActorRef.noSender;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.SimpleDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.clustering.it.provider.impl.FlappingSingletonService;
import org.opendaylight.controller.clustering.it.provider.impl.GetConstantService;
import org.opendaylight.controller.clustering.it.provider.impl.IdIntsDOMDataTreeLIstener;
import org.opendaylight.controller.clustering.it.provider.impl.IdIntsListener;
import org.opendaylight.controller.clustering.it.provider.impl.PrefixLeaderHandler;
import org.opendaylight.controller.clustering.it.provider.impl.PrefixShardHandler;
import org.opendaylight.controller.clustering.it.provider.impl.ProduceTransactionsHandler;
import org.opendaylight.controller.clustering.it.provider.impl.PublishNotificationsTask;
import org.opendaylight.controller.clustering.it.provider.impl.RoutedGetConstantService;
import org.opendaylight.controller.clustering.it.provider.impl.SingletonGetConstantService;
import org.opendaylight.controller.clustering.it.provider.impl.WriteTransactionsHandler;
import org.opendaylight.controller.clustering.it.provider.impl.YnlListener;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeLoopException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.AddShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.AddShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.BecomePrefixLeaderInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.BecomePrefixLeaderOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CreatePrefixShardInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CreatePrefixShardOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.DeconfigureIdIntsShardInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.DeconfigureIdIntsShardOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.IsClientAbortedInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.IsClientAbortedOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.OdlMdsalLowlevelControlService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterBoundConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterBoundConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterBoundConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterDefaultConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterDefaultConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterFlappingSingletonInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterFlappingSingletonOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterFlappingSingletonOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterSingletonConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterSingletonConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterSingletonConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemovePrefixShardInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemovePrefixShardOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemoveShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemoveShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownPrefixShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownPrefixShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownPrefixShardReplicaOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplicaOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtlOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDtclInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDtclOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDtclOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeYnlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeYnlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeYnlOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterDefaultConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterDefaultConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterSingletonConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterSingletonConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterSingletonConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDdtlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDdtlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDdtlOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsOutput;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

public class MdsalLowLevelTestProvider implements OdlMdsalLowlevelControlService {

    private static final Logger LOG = LoggerFactory.getLogger(MdsalLowLevelTestProvider.class);
    private static final org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType CONTROLLER_CONFIG =
            org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

    private final RpcProviderRegistry rpcRegistry;
    private final BindingAwareBroker.RpcRegistration<OdlMdsalLowlevelControlService> registration;
    private final DistributedShardFactory distributedShardFactory;
    private final DistributedDataStoreInterface configDataStore;
    private final DOMDataTreeService domDataTreeService;
    private final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;
    private final DOMDataBroker domDataBroker;
    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;
    private final SchemaService schemaService;
    private final ClusterSingletonServiceProvider singletonService;
    private final DOMRpcProviderService domRpcService;
    private final PrefixLeaderHandler prefixLeaderHandler;
    private final PrefixShardHandler prefixShardHandler;
    private final DOMDataTreeChangeService domDataTreeChangeService;
    private final ActorSystem actorSystem;

    private final Map<InstanceIdentifier<?>, DOMRpcImplementationRegistration<RoutedGetConstantService>>
            routedRegistrations = new HashMap<>();

    private final Map<String, ListenerRegistration<YnlListener>> ynlRegistrations = new HashMap<>();

    private DOMRpcImplementationRegistration<GetConstantService> globalGetConstantRegistration = null;
    private ClusterSingletonServiceRegistration getSingletonConstantRegistration;
    private FlappingSingletonService flappingSingletonService;
    private ListenerRegistration<DOMDataTreeChangeListener> dtclReg;
    private IdIntsListener idIntsListener;
    private final Map<String, PublishNotificationsTask> publishNotificationsTasks = new HashMap<>();
    private ListenerRegistration<IdIntsDOMDataTreeLIstener> ddtlReg;
    private IdIntsDOMDataTreeLIstener idIntsDdtl;



    public MdsalLowLevelTestProvider(final RpcProviderRegistry rpcRegistry,
                                     final DOMRpcProviderService domRpcService,
                                     final ClusterSingletonServiceProvider singletonService,
                                     final SchemaService schemaService,
                                     final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer,
                                     final NotificationPublishService notificationPublishService,
                                     final NotificationService notificationService,
                                     final DOMDataBroker domDataBroker,
                                     final DOMDataTreeService domDataTreeService,
                                     final DistributedShardFactory distributedShardFactory,
                                     final DistributedDataStoreInterface configDataStore,
                                     final ActorSystemProvider actorSystemProvider) {
        this.rpcRegistry = rpcRegistry;
        this.domRpcService = domRpcService;
        this.singletonService = singletonService;
        this.schemaService = schemaService;
        this.bindingNormalizedNodeSerializer = bindingNormalizedNodeSerializer;
        this.notificationPublishService = notificationPublishService;
        this.notificationService = notificationService;
        this.domDataBroker = domDataBroker;
        this.domDataTreeService = domDataTreeService;
        this.distributedShardFactory = distributedShardFactory;
        this.configDataStore = configDataStore;
        this.actorSystem = actorSystemProvider.getActorSystem();

        this.prefixLeaderHandler = new PrefixLeaderHandler(domDataTreeService, bindingNormalizedNodeSerializer);

        domDataTreeChangeService =
                (DOMDataTreeChangeService) domDataBroker.getSupportedExtensions().get(DOMDataTreeChangeService.class);

        registration = rpcRegistry.addRpcImplementation(OdlMdsalLowlevelControlService.class, this);

        prefixShardHandler = new PrefixShardHandler(distributedShardFactory, domDataTreeService,
                bindingNormalizedNodeSerializer);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<RpcResult<UnregisterSingletonConstantOutput>> unregisterSingletonConstant(
            final UnregisterSingletonConstantInput input) {
        LOG.debug("unregister-singleton-constant");

        if (getSingletonConstantRegistration == null) {
            LOG.debug("No get-singleton-constant registration present.");
            final RpcError rpcError = RpcResultBuilder.newError(ErrorType.APPLICATION, "missing-registration",
                    "No get-singleton-constant rpc registration present.");
            final RpcResult<UnregisterSingletonConstantOutput> result =
                    RpcResultBuilder.<UnregisterSingletonConstantOutput>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        try {
            getSingletonConstantRegistration.close();
            getSingletonConstantRegistration = null;

            return Futures.immediateFuture(RpcResultBuilder.success(
                new UnregisterSingletonConstantOutputBuilder().build()).build());
        } catch (Exception e) {
            LOG.debug("There was a problem closing the singleton constant service", e);
            final RpcError rpcError = RpcResultBuilder.newError(ErrorType.APPLICATION, "error-closing",
                    "There was a problem closing get-singleton-constant");
            final RpcResult<UnregisterSingletonConstantOutput> result =
                    RpcResultBuilder.<UnregisterSingletonConstantOutput>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }
    }

    @Override
    public ListenableFuture<RpcResult<StartPublishNotificationsOutput>> startPublishNotifications(
            final StartPublishNotificationsInput input) {
        LOG.debug("publish-notifications, input: {}", input);

        final PublishNotificationsTask task = new PublishNotificationsTask(notificationPublishService, input.getId(),
                input.getSeconds(), input.getNotificationsPerSecond());

        publishNotificationsTasks.put(input.getId(), task);

        task.start();

        return Futures.immediateFuture(RpcResultBuilder.success(new StartPublishNotificationsOutputBuilder().build())
            .build());
    }

    @Override
    public ListenableFuture<RpcResult<SubscribeDtclOutput>> subscribeDtcl(final SubscribeDtclInput input) {

        if (dtclReg != null) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already dataTreeChangeListener registered on id-ints list.");
            return Futures.immediateFuture(RpcResultBuilder.<SubscribeDtclOutput>failed().withRpcError(error).build());
        }

        idIntsListener = new IdIntsListener();

        dtclReg = domDataTreeChangeService
                .registerDataTreeChangeListener(
                        new org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier(
                                CONTROLLER_CONFIG, WriteTransactionsHandler.ID_INT_YID),
                        idIntsListener);

        return Futures.immediateFuture(RpcResultBuilder.success(new SubscribeDtclOutputBuilder().build()).build());
    }

    @Override
    public ListenableFuture<RpcResult<WriteTransactionsOutput>> writeTransactions(final WriteTransactionsInput input) {
        LOG.debug("write-transactions, input: {}", input);
        return WriteTransactionsHandler.start(domDataBroker, input);
    }

    @Override
    public ListenableFuture<RpcResult<IsClientAbortedOutput>> isClientAborted(final IsClientAbortedInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<RemoveShardReplicaOutput>> removeShardReplica(
            final RemoveShardReplicaInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<SubscribeYnlOutput>> subscribeYnl(final SubscribeYnlInput input) {

        LOG.debug("subscribe-ynl, input: {}", input);

        if (ynlRegistrations.containsKey(input.getId())) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already ynl listener registered for this id: " + input.getId());
            return Futures.immediateFuture(RpcResultBuilder.<SubscribeYnlOutput>failed().withRpcError(error).build());
        }

        ynlRegistrations.put(input.getId(),
                notificationService.registerNotificationListener(new YnlListener(input.getId())));

        return Futures.immediateFuture(RpcResultBuilder.success(new SubscribeYnlOutputBuilder().build()).build());
    }

    @Override
    public ListenableFuture<RpcResult<RemovePrefixShardOutput>> removePrefixShard(final RemovePrefixShardInput input) {
        LOG.debug("remove-prefix-shard, input: {}", input);

        return prefixShardHandler.onRemovePrefixShard(input);
    }

    @Override
    public ListenableFuture<RpcResult<BecomePrefixLeaderOutput>> becomePrefixLeader(
            final BecomePrefixLeaderInput input) {
        LOG.debug("become-prefix-leader, input: {}", input);

        return prefixLeaderHandler.makeLeaderLocal(input);
    }

    @Override
    public ListenableFuture<RpcResult<UnregisterBoundConstantOutput>> unregisterBoundConstant(
            final UnregisterBoundConstantInput input) {
        LOG.debug("unregister-bound-constant, {}", input);

        final DOMRpcImplementationRegistration<RoutedGetConstantService> rpcRegistration =
                routedRegistrations.remove(input.getContext());

        if (rpcRegistration == null) {
            LOG.debug("No get-contexted-constant registration for context: {}", input.getContext());
            final RpcError rpcError = RpcResultBuilder.newError(ErrorType.APPLICATION, "missing-registration",
                    "No get-constant rpc registration present.");
            final RpcResult<UnregisterBoundConstantOutput> result =
                    RpcResultBuilder.<UnregisterBoundConstantOutput>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        rpcRegistration.close();
        return Futures.immediateFuture(RpcResultBuilder.success(new UnregisterBoundConstantOutputBuilder().build())
            .build());
    }

    @Override
    public ListenableFuture<RpcResult<RegisterSingletonConstantOutput>> registerSingletonConstant(
            final RegisterSingletonConstantInput input) {

        LOG.debug("Received register-singleton-constant rpc, input: {}", input);

        if (input.getConstant() == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Invalid input.", "Constant value is null");
            return Futures.immediateFuture(RpcResultBuilder.<RegisterSingletonConstantOutput>failed()
                .withRpcError(error).build());
        }

        getSingletonConstantRegistration =
                SingletonGetConstantService.registerNew(singletonService, domRpcService, input.getConstant());

        return Futures.immediateFuture(RpcResultBuilder.success(new RegisterSingletonConstantOutputBuilder().build())
            .build());
    }

    @Override
    public ListenableFuture<RpcResult<RegisterDefaultConstantOutput>> registerDefaultConstant(
            final RegisterDefaultConstantInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<UnregisterConstantOutput>> unregisterConstant(
            final UnregisterConstantInput input) {

        if (globalGetConstantRegistration == null) {
            final RpcError rpcError = RpcResultBuilder.newError(ErrorType.APPLICATION, "missing-registration",
                    "No get-constant rpc registration present.");
            return Futures.immediateFuture(RpcResultBuilder.<UnregisterConstantOutput>failed().withRpcError(rpcError)
                .build());
        }

        globalGetConstantRegistration.close();
        globalGetConstantRegistration = null;

        return Futures.immediateFuture(RpcResultBuilder.success(new UnregisterConstantOutputBuilder().build()).build());
    }

    @Override
    public ListenableFuture<RpcResult<UnregisterFlappingSingletonOutput>> unregisterFlappingSingleton(
            final UnregisterFlappingSingletonInput input) {
        LOG.debug("unregister-flapping-singleton received.");

        if (flappingSingletonService == null) {
            final RpcError rpcError = RpcResultBuilder.newError(ErrorType.APPLICATION, "missing-registration",
                    "No flapping-singleton registration present.");
            final RpcResult<UnregisterFlappingSingletonOutput> result =
                    RpcResultBuilder.<UnregisterFlappingSingletonOutput>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        final long flapCount = flappingSingletonService.setInactive();
        flappingSingletonService = null;

        final UnregisterFlappingSingletonOutput output =
                new UnregisterFlappingSingletonOutputBuilder().setFlapCount(flapCount).build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public ListenableFuture<RpcResult<AddShardReplicaOutput>> addShardReplica(final AddShardReplicaInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<SubscribeDdtlOutput>> subscribeDdtl(final SubscribeDdtlInput input) {

        if (ddtlReg != null) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already dataTreeChangeListener registered on id-ints list.");
            return Futures.immediateFuture(RpcResultBuilder.<SubscribeDdtlOutput>failed().withRpcError(error).build());
        }

        idIntsDdtl = new IdIntsDOMDataTreeLIstener();

        try {
            ddtlReg = domDataTreeService.registerListener(idIntsDdtl,
                    Collections.singleton(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                            ProduceTransactionsHandler.ID_INT_YID)),
                    true, Collections.emptyList());
        } catch (DOMDataTreeLoopException e) {
            LOG.error("Failed to register DOMDataTreeListener.", e);
        }

        return Futures.immediateFuture(RpcResultBuilder.success(new SubscribeDdtlOutputBuilder().build()).build());
    }

    @Override
    public ListenableFuture<RpcResult<RegisterBoundConstantOutput>> registerBoundConstant(
            final RegisterBoundConstantInput input) {
        LOG.debug("register-bound-constant: {}", input);

        if (input.getContext() == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Invalid input.", "Context value is null");
            return Futures.immediateFuture(RpcResultBuilder.<RegisterBoundConstantOutput>failed().withRpcError(error)
                .build());
        }

        if (input.getConstant() == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Invalid input.", "Constant value is null");
            return Futures.immediateFuture(RpcResultBuilder.<RegisterBoundConstantOutput>failed().withRpcError(error)
                .build());
        }

        if (routedRegistrations.containsKey(input.getContext())) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already a rpc registered for context: " + input.getContext());
            return Futures.immediateFuture(RpcResultBuilder.<RegisterBoundConstantOutput>failed().withRpcError(error)
                .build());
        }

        final DOMRpcImplementationRegistration<RoutedGetConstantService> rpcRegistration =
                RoutedGetConstantService.registerNew(bindingNormalizedNodeSerializer, domRpcService,
                        input.getConstant(), input.getContext());

        routedRegistrations.put(input.getContext(), rpcRegistration);
        return Futures.immediateFuture(RpcResultBuilder.success(new RegisterBoundConstantOutputBuilder().build())
            .build());
    }

    @Override
    public ListenableFuture<RpcResult<RegisterFlappingSingletonOutput>> registerFlappingSingleton(
            final RegisterFlappingSingletonInput input) {
        LOG.debug("Received register-flapping-singleton.");

        if (flappingSingletonService != null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Registration present.", "flapping-singleton already registered");
            return Futures.immediateFuture(RpcResultBuilder.<RegisterFlappingSingletonOutput>failed()
                .withRpcError(error).build());
        }

        flappingSingletonService = new FlappingSingletonService(singletonService);

        return Futures.immediateFuture(RpcResultBuilder.success(new RegisterFlappingSingletonOutputBuilder().build())
            .build());
    }

    @Override
    public ListenableFuture<RpcResult<UnsubscribeDtclOutput>> unsubscribeDtcl(final UnsubscribeDtclInput input) {
        LOG.debug("Received unsubscribe-dtcl");

        if (idIntsListener == null || dtclReg == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Dtcl missing.", "No DataTreeChangeListener registered.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                    .withRpcError(error).build());
        }

        try {
            idIntsListener.tryFinishProcessing().get(120, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "resource-denied-transport",
                    "Unable to finish notification processing in 120 seconds.", "clustering-it", "clustering-it", e);
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                    .withRpcError(error).build());
        }

        dtclReg.close();
        dtclReg = null;

        if (!idIntsListener.hasTriggered()) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.APPLICATION, "No notification received.", "id-ints listener has not received"
                            + "any notifications.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                    .withRpcError(error).build());
        }

        final DOMDataReadOnlyTransaction rTx = domDataBroker.newReadOnlyTransaction();
        try {
            final Optional<NormalizedNode<?, ?>> readResult =
                    rTx.read(CONTROLLER_CONFIG, WriteTransactionsHandler.ID_INT_YID).checkedGet();

            if (!readResult.isPresent()) {
                final RpcError error = RpcResultBuilder.newError(
                        ErrorType.APPLICATION, "Final read empty.", "No data read from id-ints list.");
                return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                        .withRpcError(error).build());
            }

            return Futures.immediateFuture(
                    RpcResultBuilder.success(new UnsubscribeDtclOutputBuilder()
                            .setCopyMatches(idIntsListener.checkEqual(readResult.get()))).build());

        } catch (final ReadFailedException e) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.APPLICATION, "Read failed.", "Final read from id-ints failed.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                    .withRpcError(error).build());

        }
    }

    @Override
    public ListenableFuture<RpcResult<CreatePrefixShardOutput>> createPrefixShard(final CreatePrefixShardInput input) {
        LOG.debug("create-prefix-shard, input: {}", input);

        return prefixShardHandler.onCreatePrefixShard(input);
    }

    @Override
    public ListenableFuture<RpcResult<DeconfigureIdIntsShardOutput>> deconfigureIdIntsShard(
            final DeconfigureIdIntsShardInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<UnsubscribeYnlOutput>> unsubscribeYnl(final UnsubscribeYnlInput input) {
        LOG.debug("Received unsubscribe-ynl, input: {}", input);

        if (!ynlRegistrations.containsKey(input.getId())) {
            final RpcError rpcError = RpcResultBuilder.newError(ErrorType.APPLICATION, "missing-registration",
                    "No ynl listener with this id registered.");
            final RpcResult<UnsubscribeYnlOutput> result =
                    RpcResultBuilder.<UnsubscribeYnlOutput>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        final ListenerRegistration<YnlListener> reg = ynlRegistrations.remove(input.getId());
        final UnsubscribeYnlOutput output = reg.getInstance().getOutput();

        reg.close();

        return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeYnlOutput>success().withResult(output).build());
    }

    @Override
    public ListenableFuture<RpcResult<CheckPublishNotificationsOutput>> checkPublishNotifications(
            final CheckPublishNotificationsInput input) {

        final PublishNotificationsTask task = publishNotificationsTasks.get(input.getId());

        if (task == null) {
            return Futures.immediateFuture(RpcResultBuilder.success(
                    new CheckPublishNotificationsOutputBuilder().setActive(false)).build());
        }

        final CheckPublishNotificationsOutputBuilder checkPublishNotificationsOutputBuilder =
                new CheckPublishNotificationsOutputBuilder().setActive(!task.isFinished());

        if (task.getLastError() != null) {
            LOG.error("Last error for {}", task, task.getLastError());
            checkPublishNotificationsOutputBuilder.setLastError(task.getLastError().toString());
        }

        final CheckPublishNotificationsOutput output =
                checkPublishNotificationsOutputBuilder.setPublishCount(task.getCurrentNotif()).build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public ListenableFuture<RpcResult<ProduceTransactionsOutput>> produceTransactions(
            final ProduceTransactionsInput input) {
        LOG.debug("producer-transactions, input: {}", input);
        return ProduceTransactionsHandler.start(domDataTreeService, input);
    }

    @Override
    public ListenableFuture<RpcResult<ShutdownShardReplicaOutput>> shutdownShardReplica(
            final ShutdownShardReplicaInput input) {
        LOG.debug("Received shutdown-shard-replica rpc, input: {}", input);

        final String shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            final RpcError rpcError = RpcResultBuilder.newError(ErrorType.APPLICATION, "bad-element",
                    "A valid shard name must be specified");
            return Futures.immediateFuture(RpcResultBuilder.<ShutdownShardReplicaOutput>failed().withRpcError(rpcError)
                .build());
        }

        return shutdownShardGracefully(shardName, new ShutdownShardReplicaOutputBuilder().build());
    }

    @Override
    public ListenableFuture<RpcResult<ShutdownPrefixShardReplicaOutput>> shutdownPrefixShardReplica(
            final ShutdownPrefixShardReplicaInput input) {
        LOG.debug("Received shutdown-prefix-shard-replica rpc, input: {}", input);

        final InstanceIdentifier<?> shardPrefix = input.getPrefix();

        if (shardPrefix == null) {
            final RpcError rpcError = RpcResultBuilder.newError(ErrorType.APPLICATION, "bad-element",
                    "A valid shard prefix must be specified");
            return Futures.immediateFuture(RpcResultBuilder.<ShutdownPrefixShardReplicaOutput>failed()
                .withRpcError(rpcError).build());
        }

        final YangInstanceIdentifier shardPath = bindingNormalizedNodeSerializer.toYangInstanceIdentifier(shardPrefix);
        final String cleanPrefixShardName = ClusterUtils.getCleanShardName(shardPath);

        return shutdownShardGracefully(cleanPrefixShardName, new ShutdownPrefixShardReplicaOutputBuilder().build());
    }

    private <T> SettableFuture<RpcResult<T>> shutdownShardGracefully(final String shardName, final T success) {
        final SettableFuture<RpcResult<T>> rpcResult = SettableFuture.create();
        final ActorContext context = configDataStore.getActorContext();

        long timeoutInMS = Math.max(context.getDatastoreContext().getShardRaftConfig()
                .getElectionTimeOutInterval().$times(3).toMillis(), 10000);
        final FiniteDuration duration = FiniteDuration.apply(timeoutInMS, TimeUnit.MILLISECONDS);
        final scala.concurrent.Promise<Boolean> shutdownShardAsk = akka.dispatch.Futures.promise();

        context.findLocalShardAsync(shardName).onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable throwable, final ActorRef actorRef) throws Throwable {
                if (throwable != null) {
                    shutdownShardAsk.failure(throwable);
                } else {
                    shutdownShardAsk.completeWith(Patterns.gracefulStop(actorRef, duration, Shutdown.INSTANCE));
                }
            }
        }, context.getClientDispatcher());

        shutdownShardAsk.future().onComplete(new OnComplete<Boolean>() {
            @Override
            public void onComplete(final Throwable throwable, final Boolean gracefulStopResult) throws Throwable {
                if (throwable != null) {
                    final RpcResult<T> failedResult = RpcResultBuilder.<T>failed()
                            .withError(ErrorType.APPLICATION, "Failed to gracefully shutdown shard", throwable).build();
                    rpcResult.set(failedResult);
                } else {
                    // according to Patterns.gracefulStop API, we don't have to
                    // check value of gracefulStopResult
                    rpcResult.set(RpcResultBuilder.success(success).build());
                }
            }
        }, context.getClientDispatcher());
        return rpcResult;
    }

    @Override
    public ListenableFuture<RpcResult<RegisterConstantOutput>> registerConstant(final RegisterConstantInput input) {

        LOG.debug("Received register-constant rpc, input: {}", input);

        if (input.getConstant() == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Invalid input.", "Constant value is null");
            return Futures.immediateFuture(RpcResultBuilder.<RegisterConstantOutput>failed().withRpcError(error)
                .build());
        }

        if (globalGetConstantRegistration != null) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already a get-constant rpc registered.");
            return Futures.immediateFuture(RpcResultBuilder.<RegisterConstantOutput>failed().withRpcError(error)
                .build());
        }

        globalGetConstantRegistration = GetConstantService.registerNew(domRpcService, input.getConstant());
        return Futures.immediateFuture(RpcResultBuilder.success(new RegisterConstantOutputBuilder().build()).build());
    }

    @Override
    public ListenableFuture<RpcResult<UnregisterDefaultConstantOutput>> unregisterDefaultConstant(
            final UnregisterDefaultConstantInput input) {
        return null;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<RpcResult<UnsubscribeDdtlOutput>> unsubscribeDdtl(final UnsubscribeDdtlInput input) {
        LOG.debug("Received unsubscribe-ddtl.");

        if (idIntsDdtl == null || ddtlReg == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Ddtl missing.", "No DOMDataTreeListener registered.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDdtlOutput>failed()
                    .withRpcError(error).build());
        }

        try {
            idIntsDdtl.tryFinishProcessing().get(120, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "resource-denied-transport",
                    "Unable to finish notification processing in 120 seconds.", "clustering-it", "clustering-it", e);
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDdtlOutput>failed()
                    .withRpcError(error).build());
        }

        ddtlReg.close();
        ddtlReg = null;

        if (!idIntsDdtl.hasTriggered()) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.APPLICATION, "No notification received.", "id-ints listener has not received"
                            + "any notifications.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDdtlOutput>failed()
                    .withRpcError(error).build());
        }

        final String shardName = ClusterUtils.getCleanShardName(ProduceTransactionsHandler.ID_INTS_YID);
        LOG.debug("Creating distributed datastore client for shard {}", shardName);

        final ActorContext actorContext = configDataStore.getActorContext();
        final Props distributedDataStoreClientProps =
                SimpleDataStoreClientActor.props(actorContext.getCurrentMemberName(),
                        "Shard-" + shardName, actorContext, shardName);

        final ActorRef clientActor = actorSystem.actorOf(distributedDataStoreClientProps);
        final DataStoreClient distributedDataStoreClient;
        try {
            distributedDataStoreClient = SimpleDataStoreClientActor
                    .getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            LOG.error("Failed to get actor for {}", distributedDataStoreClientProps, e);
            clientActor.tell(PoisonPill.getInstance(), noSender());
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.APPLICATION, "Unable to create ds client for read.",
                    "Unable to create ds client for read.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDdtlOutput>failed()
                    .withRpcError(error).build());
        }

        final ClientLocalHistory localHistory = distributedDataStoreClient.createLocalHistory();
        final ClientTransaction tx = localHistory.createTransaction();
        final CheckedFuture<Optional<NormalizedNode<?, ?>>,
                org.opendaylight.mdsal.common.api.ReadFailedException> read =
                tx.read(YangInstanceIdentifier.of(ProduceTransactionsHandler.ID_INT));

        tx.abort();
        localHistory.close();
        try {
            final Optional<NormalizedNode<?, ?>> optional = read.checkedGet();
            if (!optional.isPresent()) {
                LOG.warn("Final read from client is empty.");
                final RpcError error = RpcResultBuilder.newError(
                        ErrorType.APPLICATION, "Read failed.", "Final read from id-ints is empty.");
                return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDdtlOutput>failed()
                        .withRpcError(error).build());
            }

            return Futures.immediateFuture(
                    RpcResultBuilder.success(new UnsubscribeDdtlOutputBuilder()
                            .setCopyMatches(idIntsDdtl.checkEqual(optional.get()))).build());

        } catch (org.opendaylight.mdsal.common.api.ReadFailedException e) {
            LOG.error("Unable to read data to verify ddtl data.", e);
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.APPLICATION, "Read failed.", "Final read from id-ints failed.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDdtlOutput>failed()
                    .withRpcError(error).build());
        } finally {
            distributedDataStoreClient.close();
            clientActor.tell(PoisonPill.getInstance(), noSender());
        }
    }
}
