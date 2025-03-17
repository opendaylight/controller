/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.OnComplete;
import org.apache.pekko.pattern.Patterns;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.clustering.it.provider.impl.FlappingSingletonService;
import org.opendaylight.controller.clustering.it.provider.impl.GetConstantService;
import org.opendaylight.controller.clustering.it.provider.impl.IdIntsListener;
import org.opendaylight.controller.clustering.it.provider.impl.PublishNotificationsTask;
import org.opendaylight.controller.clustering.it.provider.impl.RoutedGetConstantService;
import org.opendaylight.controller.clustering.it.provider.impl.SingletonGetConstantService;
import org.opendaylight.controller.clustering.it.provider.impl.WriteTransactionsHandler;
import org.opendaylight.controller.clustering.it.provider.impl.YnlListener;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.AddShardReplica;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.AddShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.AddShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotifications;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.IsClientAborted;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.IsClientAbortedInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.IsClientAbortedOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterBoundConstant;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterBoundConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterBoundConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterBoundConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterConstant;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterDefaultConstant;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterDefaultConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterDefaultConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterFlappingSingleton;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterFlappingSingletonInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterFlappingSingletonOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterFlappingSingletonOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterSingletonConstant;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterSingletonConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterSingletonConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterSingletonConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemoveShardReplica;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemoveShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemoveShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplica;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplicaOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotifications;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtl;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDtcl;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDtclInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDtclOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDtclOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeYnl;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeYnlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeYnlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeYnlOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstant;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterConstant;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterDefaultConstant;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterDefaultConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterDefaultConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingleton;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterSingletonConstant;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterSingletonConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterSingletonConstantOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterSingletonConstantOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDdtl;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDdtlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDdtlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtcl;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnl;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactions;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.IdSequence;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.PropertyIdentifier;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

@Singleton
@Component(service = {})
public final class MdsalLowLevelTestProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalLowLevelTestProvider.class);

    private final Registration registration;
    private final DistributedDataStoreInterface configDataStore;
    private final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;
    private final DOMDataBroker domDataBroker;
    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;
    private final ClusterSingletonServiceProvider singletonService;
    private final DOMRpcProviderService domRpcService;
    private final DataTreeChangeExtension dataTreeChangeExtension;

    private final Map<InstanceIdentifier<?>, Registration> routedRegistrations = new HashMap<>();
    private final Map<String, ObjectRegistration<YnlListener>> ynlRegistrations = new HashMap<>();
    private final Map<String, PublishNotificationsTask> publishNotificationsTasks = new HashMap<>();

    private Registration globalGetConstantRegistration = null;
    private Registration getSingletonConstantRegistration;
    private FlappingSingletonService flappingSingletonService;
    private Registration dtclReg;
    private IdIntsListener idIntsListener;

    @Inject
    @Activate
    public MdsalLowLevelTestProvider(
            @Reference final RpcProviderService rpcRegistry,
            @Reference final DOMRpcProviderService domRpcService,
            @Reference final ClusterSingletonServiceProvider singletonService,
            @Reference final DOMSchemaService schemaService,
            @Reference final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer,
            @Reference final NotificationPublishService notificationPublishService,
            @Reference final NotificationService notificationService,
            @Reference final DOMDataBroker domDataBroker,
            @Reference final DistributedDataStoreInterface configDataStore) {
        this.domRpcService = domRpcService;
        this.singletonService = singletonService;
        this.bindingNormalizedNodeSerializer = bindingNormalizedNodeSerializer;
        this.notificationPublishService = notificationPublishService;
        this.notificationService = notificationService;
        this.domDataBroker = domDataBroker;
        this.configDataStore = configDataStore;

        dataTreeChangeExtension = domDataBroker.extension(DataTreeChangeExtension.class);

        registration = rpcRegistry.registerRpcImplementations(
            (UnregisterSingletonConstant) this::unregisterSingletonConstant,
            (StartPublishNotifications) this::startPublishNotifications,
            (SubscribeDdtl) this::subscribeDdtl,
            (WriteTransactions) this::writeTransactions,
            (IsClientAborted) this::isClientAborted,
            (RemoveShardReplica) this::removeShardReplica,
            (SubscribeYnl) this::subscribeYnl,
            (UnregisterBoundConstant) this::unregisterBoundConstant,
            (RegisterSingletonConstant) this::registerSingletonConstant,
            (RegisterDefaultConstant) this::registerDefaultConstant,
            (UnregisterConstant) this::unregisterConstant,
            (UnregisterFlappingSingleton) this::unregisterFlappingSingleton,
            (AddShardReplica) this::addShardReplica,
            (RegisterBoundConstant) this::registerBoundConstant,
            (RegisterFlappingSingleton) this::registerFlappingSingleton,
            (UnsubscribeDdtl) this::unsubscribeDdtl,
            (UnsubscribeYnl) this::unsubscribeYnl,
            (CheckPublishNotifications) this::checkPublishNotifications,
            (ShutdownShardReplica) this::shutdownShardReplica,
            (RegisterConstant) this::registerConstant,
            (UnregisterDefaultConstant) this::unregisterDefaultConstant,
            (SubscribeDtcl) this::subscribeDtcl,
            (UnsubscribeDtcl) this::unsubscribeDtcl);
    }

    @PreDestroy
    @Deactivate
    public void close() {
        registration.close();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private ListenableFuture<RpcResult<UnregisterSingletonConstantOutput>> unregisterSingletonConstant(
            final UnregisterSingletonConstantInput input) {
        LOG.info("In unregisterSingletonConstant");

        if (getSingletonConstantRegistration == null) {
            return RpcResultBuilder.<UnregisterSingletonConstantOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_MISSING, "No prior RPC was registered")
                .buildFuture();
        }

        try {
            getSingletonConstantRegistration.close();
            getSingletonConstantRegistration = null;

            return RpcResultBuilder.success(new UnregisterSingletonConstantOutputBuilder().build()).buildFuture();
        } catch (Exception e) {
            String msg = "Error closing the singleton constant service";
            LOG.error(msg, e);
            return RpcResultBuilder.<UnregisterSingletonConstantOutput>failed().withError(
                    ErrorType.APPLICATION, msg, e).buildFuture();
        }
    }

    private ListenableFuture<RpcResult<StartPublishNotificationsOutput>> startPublishNotifications(
            final StartPublishNotificationsInput input) {
        LOG.info("In startPublishNotifications - input: {}", input);

        final PublishNotificationsTask task = new PublishNotificationsTask(notificationPublishService, input.getId(),
                input.getSeconds().toJava(), input.getNotificationsPerSecond().toJava());

        publishNotificationsTasks.put(input.getId(), task);

        task.start();

        return RpcResultBuilder.success(new StartPublishNotificationsOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<SubscribeDtclOutput>> subscribeDtcl(final SubscribeDtclInput input) {
        LOG.info("In subscribeDtcl - input: {}", input);

        if (dtclReg != null) {
            return RpcResultBuilder.<SubscribeDtclOutput>failed().withError(ErrorType.RPC, ErrorTag.DATA_EXISTS,
                "There is already a DataTreeChangeListener registered for id-ints")
                .buildFuture();
        }

        idIntsListener = new IdIntsListener();

        dtclReg = dataTreeChangeExtension.registerTreeChangeListener(
            DOMDataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION, WriteTransactionsHandler.ID_INT_YID),
            idIntsListener);

        return RpcResultBuilder.success(new SubscribeDtclOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<WriteTransactionsOutput>> writeTransactions(final WriteTransactionsInput input) {
        return WriteTransactionsHandler.start(domDataBroker, input);
    }

    private ListenableFuture<RpcResult<IsClientAbortedOutput>> isClientAborted(final IsClientAbortedInput input) {
        return null;
    }

    private ListenableFuture<RpcResult<RemoveShardReplicaOutput>> removeShardReplica(
            final RemoveShardReplicaInput input) {
        return null;
    }

    private ListenableFuture<RpcResult<SubscribeYnlOutput>> subscribeYnl(final SubscribeYnlInput input) {
        LOG.info("In subscribeYnl - input: {}", input);

        if (ynlRegistrations.containsKey(input.getId())) {
            return RpcResultBuilder.<SubscribeYnlOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_EXISTS,
                    "There is already a listener registered for id: " + input.getId())
                .buildFuture();
        }

        final var id = input.getId();
        final var listener = new YnlListener(id);
        final var reg = notificationService.registerListener(IdSequence.class, listener);
        ynlRegistrations.put(id, new AbstractObjectRegistration<>(listener) {
            @Override
            protected void removeRegistration() {
                reg.close();
            }
        });

        return RpcResultBuilder.success(new SubscribeYnlOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<UnregisterBoundConstantOutput>> unregisterBoundConstant(
            final UnregisterBoundConstantInput input) {
        LOG.info("In unregisterBoundConstant - {}", input);

        final DataObjectIdentifier<?> context;
        switch (input.getContext()) {
            case null -> {
                return RpcResultBuilder.<UnregisterBoundConstantOutput>failed()
                    .withError(ErrorType.RPC, ErrorTag.INVALID_VALUE, "Context value is null")
                    .buildFuture();
            }
            case PropertyIdentifier<?, ?> pid -> {
                return RpcResultBuilder.<UnregisterBoundConstantOutput>failed().withError(
                    ErrorType.RPC, ErrorTag.INVALID_VALUE, "Context value points to a non-container").buildFuture();
            }
            case DataObjectIdentifier<?> doi -> context = doi;
        }

        final var rpcRegistration = routedRegistrations.remove(context.toLegacy());
        if (rpcRegistration == null) {
            return RpcResultBuilder.<UnregisterBoundConstantOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_MISSING,
                    "No prior RPC was registered for " + context)
                .buildFuture();
        }

        rpcRegistration.close();
        return RpcResultBuilder.success(new UnregisterBoundConstantOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<RegisterSingletonConstantOutput>> registerSingletonConstant(
            final RegisterSingletonConstantInput input) {
        LOG.info("In registerSingletonConstant - input: {}", input);

        final var constant = input.getConstant();
        if (constant == null) {
            return RpcResultBuilder.<RegisterSingletonConstantOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.INVALID_VALUE, "Constant value is null")
                .buildFuture();
        }

        getSingletonConstantRegistration = SingletonGetConstantService.registerNew(singletonService, domRpcService,
            constant);

        return RpcResultBuilder.success(new RegisterSingletonConstantOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<RegisterDefaultConstantOutput>> registerDefaultConstant(
            final RegisterDefaultConstantInput input) {
        return null;
    }

    private ListenableFuture<RpcResult<UnregisterConstantOutput>> unregisterConstant(
            final UnregisterConstantInput input) {
        LOG.info("In unregisterConstant");

        if (globalGetConstantRegistration == null) {
            return RpcResultBuilder.<UnregisterConstantOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_MISSING, "No prior RPC was registered")
                .buildFuture();
        }

        globalGetConstantRegistration.close();
        globalGetConstantRegistration = null;

        return RpcResultBuilder.success(new UnregisterConstantOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<UnregisterFlappingSingletonOutput>> unregisterFlappingSingleton(
            final UnregisterFlappingSingletonInput input) {
        LOG.info("In unregisterFlappingSingleton");

        if (flappingSingletonService == null) {
            return RpcResultBuilder.<UnregisterFlappingSingletonOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_MISSING, "No prior RPC was registered")
                .buildFuture();
        }

        final long flapCount = flappingSingletonService.setInactive();
        flappingSingletonService = null;

        return RpcResultBuilder.success(new UnregisterFlappingSingletonOutputBuilder().setFlapCount(flapCount).build())
                .buildFuture();
    }

    private ListenableFuture<RpcResult<AddShardReplicaOutput>> addShardReplica(final AddShardReplicaInput input) {
        throw new UnsupportedOperationException();
    }

    private ListenableFuture<RpcResult<SubscribeDdtlOutput>> subscribeDdtl(final SubscribeDdtlInput input) {
        throw new UnsupportedOperationException();
    }

    private ListenableFuture<RpcResult<RegisterBoundConstantOutput>> registerBoundConstant(
            final RegisterBoundConstantInput input) {
        LOG.info("In registerBoundConstant - input: {}", input);

        final DataObjectIdentifier<?> context;
        switch (input.getContext()) {
            case null -> {
                return RpcResultBuilder.<RegisterBoundConstantOutput>failed().withError(
                    ErrorType.RPC, ErrorTag.INVALID_VALUE, "Context value is null").buildFuture();
            }
            case PropertyIdentifier<?, ?> pid -> {
                return RpcResultBuilder.<RegisterBoundConstantOutput>failed().withError(
                    ErrorType.RPC, ErrorTag.INVALID_VALUE, "Context value points to a non-container").buildFuture();
            }
            case DataObjectIdentifier<?> bid -> context = bid;
        }

        final var constant = input.getConstant();
        if (constant == null) {
            return RpcResultBuilder.<RegisterBoundConstantOutput>failed().withError(
                    ErrorType.RPC, ErrorTag.INVALID_VALUE, "Constant value is null").buildFuture();
        }

        final var iid = context.toLegacy();
        if (routedRegistrations.containsKey(iid)) {
            return RpcResultBuilder.<RegisterBoundConstantOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_EXISTS,
                    "There is already an rpc registered for context: " + context)
                .buildFuture();
        }

        final var rpcRegistration = RoutedGetConstantService.registerNew(bindingNormalizedNodeSerializer, domRpcService,
            constant, iid);

        routedRegistrations.put(iid, rpcRegistration);
        return RpcResultBuilder.success(new RegisterBoundConstantOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<RegisterFlappingSingletonOutput>> registerFlappingSingleton(
            final RegisterFlappingSingletonInput input) {
        LOG.info("In registerFlappingSingleton");

        if (flappingSingletonService != null) {
            return RpcResultBuilder.<RegisterFlappingSingletonOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_EXISTS, "There is already an rpc registered")
                .buildFuture();
        }

        flappingSingletonService = new FlappingSingletonService(singletonService);

        return RpcResultBuilder.success(new RegisterFlappingSingletonOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<UnsubscribeDtclOutput>> unsubscribeDtcl(final UnsubscribeDtclInput input) {
        LOG.info("In unsubscribeDtcl");

        if (idIntsListener == null || dtclReg == null) {
            return RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_MISSING, "No prior listener was registered")
                .buildFuture();
        }

        long timeout = 120L;
        try {
            idIntsListener.tryFinishProcessing().get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to finish notification processing", e);
            return RpcResultBuilder.<UnsubscribeDtclOutput>failed().withError(ErrorType.APPLICATION,
                    "Unable to finish notification processing in " + timeout + " seconds", e).buildFuture();
        }

        dtclReg.close();
        dtclReg = null;

        if (!idIntsListener.hasTriggered()) {
            return RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                .withError(ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED,
                    "id-ints listener has not received any notifications.")
                .buildFuture();
        }

        try (DOMDataTreeReadTransaction rTx = domDataBroker.newReadOnlyTransaction()) {
            final Optional<NormalizedNode> readResult = rTx.read(LogicalDatastoreType.CONFIGURATION,
                WriteTransactionsHandler.ID_INT_YID).get();

            if (!readResult.isPresent()) {
                return RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                    .withError(ErrorType.APPLICATION, ErrorTag.DATA_MISSING, "No data read from id-ints list")
                    .buildFuture();
            }

            final boolean nodesEqual = idIntsListener.checkEqual(readResult.orElseThrow());
            if (!nodesEqual) {
                LOG.error("Final read of id-int does not match IdIntsListener's copy. {}",
                        idIntsListener.diffWithLocalCopy(readResult.orElseThrow()));
            }

            return RpcResultBuilder.success(new UnsubscribeDtclOutputBuilder().setCopyMatches(nodesEqual).build())
                    .buildFuture();

        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Final read of id-ints failed", e);
            return RpcResultBuilder.<UnsubscribeDtclOutput>failed().withError(ErrorType.APPLICATION,
                    "Final read of id-ints failed", e).buildFuture();
        }
    }

    private ListenableFuture<RpcResult<UnsubscribeYnlOutput>> unsubscribeYnl(final UnsubscribeYnlInput input) {
        LOG.info("In unsubscribeYnl - input: {}", input);

        if (!ynlRegistrations.containsKey(input.getId())) {
            return RpcResultBuilder.<UnsubscribeYnlOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_MISSING,
                    "No prior listener was registered for " + input.getId())
                .buildFuture();
        }

        try (var reg = ynlRegistrations.remove(input.getId())) {
            return RpcResultBuilder.<UnsubscribeYnlOutput>success()
                .withResult(reg.getInstance().getOutput())
                .buildFuture();
        }
    }

    private ListenableFuture<RpcResult<CheckPublishNotificationsOutput>> checkPublishNotifications(
            final CheckPublishNotificationsInput input) {
        LOG.info("In checkPublishNotifications - input: {}", input);

        final PublishNotificationsTask task = publishNotificationsTasks.get(input.getId());

        if (task == null) {
            return RpcResultBuilder.success(new CheckPublishNotificationsOutputBuilder().setActive(false).build())
                .buildFuture();
        }

        final CheckPublishNotificationsOutputBuilder checkPublishNotificationsOutputBuilder =
                new CheckPublishNotificationsOutputBuilder().setActive(!task.isFinished());

        if (task.getLastError() != null) {
            LOG.error("Last error for {}", task, task.getLastError());
            checkPublishNotificationsOutputBuilder.setLastError(task.getLastError().toString());
        }

        final CheckPublishNotificationsOutput output =
                checkPublishNotificationsOutputBuilder.setPublishCount(task.getCurrentNotif()).build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    private ListenableFuture<RpcResult<ShutdownShardReplicaOutput>> shutdownShardReplica(
            final ShutdownShardReplicaInput input) {
        LOG.info("In shutdownShardReplica - input: {}", input);

        final String shardName = input.getShardName();
        if (Strings.isNullOrEmpty(shardName)) {
            return RpcResultBuilder.<ShutdownShardReplicaOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.BAD_ELEMENT, shardName + "is not a valid shard name")
                .buildFuture();
        }

        return shutdownShardGracefully(shardName, new ShutdownShardReplicaOutputBuilder().build());
    }

    private <T> SettableFuture<RpcResult<T>> shutdownShardGracefully(final String shardName, final T success) {
        final SettableFuture<RpcResult<T>> rpcResult = SettableFuture.create();
        final ActorUtils context = configDataStore.getActorUtils();

        long timeoutInMS = Math.max(context.getDatastoreContext().getShardRaftConfig()
                .getElectionTimeOutInterval().multipliedBy(3).toMillis(), 10000);
        final FiniteDuration duration = FiniteDuration.apply(timeoutInMS, TimeUnit.MILLISECONDS);
        final scala.concurrent.Promise<Boolean> shutdownShardAsk = Futures.promise();

        context.findLocalShardAsync(shardName).onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable throwable, final ActorRef actorRef) {
                if (throwable != null) {
                    shutdownShardAsk.failure(throwable);
                } else {
                    shutdownShardAsk.completeWith(Patterns.gracefulStop(actorRef, duration, Shutdown.INSTANCE));
                }
            }
        }, context.getClientDispatcher());

        shutdownShardAsk.future().onComplete(new OnComplete<Boolean>() {
            @Override
            public void onComplete(final Throwable throwable, final Boolean gracefulStopResult) {
                if (throwable != null) {
                    final var failedResult = RpcResultBuilder.<T>failed()
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

    private ListenableFuture<RpcResult<RegisterConstantOutput>> registerConstant(final RegisterConstantInput input) {
        LOG.info("In registerConstant - input: {}", input);

        if (input.getConstant() == null) {
            return RpcResultBuilder.<RegisterConstantOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.INVALID_VALUE, "Constant value is null")
                .buildFuture();
        }

        if (globalGetConstantRegistration != null) {
            return RpcResultBuilder.<RegisterConstantOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_EXISTS, "There is already an rpc registered")
                .buildFuture();
        }

        globalGetConstantRegistration = GetConstantService.registerNew(domRpcService, input.getConstant());
        return RpcResultBuilder.success(new RegisterConstantOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<UnregisterDefaultConstantOutput>> unregisterDefaultConstant(
            final UnregisterDefaultConstantInput input) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private ListenableFuture<RpcResult<UnsubscribeDdtlOutput>> unsubscribeDdtl(final UnsubscribeDdtlInput input) {
        throw new UnsupportedOperationException();
    }
}
