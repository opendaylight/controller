/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.AddShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.AddShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.IsClientAbortedInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.IsClientAbortedOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.OdlMdsalLowlevelControlService;
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
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemoveShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemoveShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplicaOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ShutdownShardReplicaOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeDdtlOutput;
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
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsOutput;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
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
public final class MdsalLowLevelTestProvider implements OdlMdsalLowlevelControlService {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalLowLevelTestProvider.class);

    private final Registration registration;
    private final DistributedDataStoreInterface configDataStore;
    private final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;
    private final DOMDataBroker domDataBroker;
    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;
    private final ClusterSingletonServiceProvider singletonService;
    private final DOMRpcProviderService domRpcService;
    private final DOMDataTreeChangeService domDataTreeChangeService;

    private final Map<InstanceIdentifier<?>, DOMRpcImplementationRegistration<RoutedGetConstantService>>
            routedRegistrations = new HashMap<>();

    private final Map<String, ListenerRegistration<YnlListener>> ynlRegistrations = new HashMap<>();

    private DOMRpcImplementationRegistration<GetConstantService> globalGetConstantRegistration = null;
    private ClusterSingletonServiceRegistration getSingletonConstantRegistration;
    private FlappingSingletonService flappingSingletonService;
    private ListenerRegistration<DOMDataTreeChangeListener> dtclReg;
    private IdIntsListener idIntsListener;
    private final Map<String, PublishNotificationsTask> publishNotificationsTasks = new HashMap<>();

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

        domDataTreeChangeService = domDataBroker.getExtensions().getInstance(DOMDataTreeChangeService.class);

        registration = rpcRegistry.registerRpcImplementation(OdlMdsalLowlevelControlService.class, this);
    }

    @PreDestroy
    @Deactivate
    public void close() {
        registration.close();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<RpcResult<UnregisterSingletonConstantOutput>> unregisterSingletonConstant(
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

    @Override
    public ListenableFuture<RpcResult<StartPublishNotificationsOutput>> startPublishNotifications(
            final StartPublishNotificationsInput input) {
        LOG.info("In startPublishNotifications - input: {}", input);

        final PublishNotificationsTask task = new PublishNotificationsTask(notificationPublishService, input.getId(),
                input.getSeconds().toJava(), input.getNotificationsPerSecond().toJava());

        publishNotificationsTasks.put(input.getId(), task);

        task.start();

        return RpcResultBuilder.success(new StartPublishNotificationsOutputBuilder().build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<SubscribeDtclOutput>> subscribeDtcl(final SubscribeDtclInput input) {
        LOG.info("In subscribeDtcl - input: {}", input);

        if (dtclReg != null) {
            return RpcResultBuilder.<SubscribeDtclOutput>failed().withError(ErrorType.RPC, ErrorTag.DATA_EXISTS,
                "There is already a DataTreeChangeListener registered for id-ints")
                .buildFuture();
        }

        idIntsListener = new IdIntsListener();

        dtclReg = domDataTreeChangeService.registerDataTreeChangeListener(
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, WriteTransactionsHandler.ID_INT_YID),
            idIntsListener);

        return RpcResultBuilder.success(new SubscribeDtclOutputBuilder().build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<WriteTransactionsOutput>> writeTransactions(final WriteTransactionsInput input) {
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
        LOG.info("In subscribeYnl - input: {}", input);

        if (ynlRegistrations.containsKey(input.getId())) {
            return RpcResultBuilder.<SubscribeYnlOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_EXISTS,
                    "There is already a listener registered for id: " + input.getId())
                .buildFuture();
        }

        ynlRegistrations.put(input.getId(),
                notificationService.registerNotificationListener(new YnlListener(input.getId())));

        return RpcResultBuilder.success(new SubscribeYnlOutputBuilder().build()).buildFuture();
    }


    @Override
    public ListenableFuture<RpcResult<UnregisterBoundConstantOutput>> unregisterBoundConstant(
            final UnregisterBoundConstantInput input) {
        LOG.info("In unregisterBoundConstant - {}", input);

        final DOMRpcImplementationRegistration<RoutedGetConstantService> rpcRegistration =
                routedRegistrations.remove(input.getContext());

        if (rpcRegistration == null) {
            return RpcResultBuilder.<UnregisterBoundConstantOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_MISSING,
                    "No prior RPC was registered for " + input.getContext())
                .buildFuture();
        }

        rpcRegistration.close();
        return RpcResultBuilder.success(new UnregisterBoundConstantOutputBuilder().build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<RegisterSingletonConstantOutput>> registerSingletonConstant(
            final RegisterSingletonConstantInput input) {
        LOG.info("In registerSingletonConstant - input: {}", input);

        if (input.getConstant() == null) {
            return RpcResultBuilder.<RegisterSingletonConstantOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.INVALID_VALUE, "Constant value is null")
                .buildFuture();
        }

        getSingletonConstantRegistration =
                SingletonGetConstantService.registerNew(singletonService, domRpcService, input.getConstant());

        return RpcResultBuilder.success(new RegisterSingletonConstantOutputBuilder().build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<RegisterDefaultConstantOutput>> registerDefaultConstant(
            final RegisterDefaultConstantInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<UnregisterConstantOutput>> unregisterConstant(
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

    @Override
    public ListenableFuture<RpcResult<UnregisterFlappingSingletonOutput>> unregisterFlappingSingleton(
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

    @Override
    public ListenableFuture<RpcResult<AddShardReplicaOutput>> addShardReplica(final AddShardReplicaInput input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<RpcResult<SubscribeDdtlOutput>> subscribeDdtl(final SubscribeDdtlInput input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<RpcResult<RegisterBoundConstantOutput>> registerBoundConstant(
            final RegisterBoundConstantInput input) {
        LOG.info("In registerBoundConstant - input: {}", input);

        if (input.getContext() == null) {
            return RpcResultBuilder.<RegisterBoundConstantOutput>failed().withError(
                    ErrorType.RPC, ErrorTag.INVALID_VALUE, "Context value is null").buildFuture();
        }

        if (input.getConstant() == null) {
            return RpcResultBuilder.<RegisterBoundConstantOutput>failed().withError(
                    ErrorType.RPC, ErrorTag.INVALID_VALUE, "Constant value is null").buildFuture();
        }

        if (routedRegistrations.containsKey(input.getContext())) {
            return RpcResultBuilder.<RegisterBoundConstantOutput>failed().withError(ErrorType.RPC,
                ErrorTag.DATA_EXISTS, "There is already an rpc registered for context: " + input.getContext())
                .buildFuture();
        }

        final DOMRpcImplementationRegistration<RoutedGetConstantService> rpcRegistration =
                RoutedGetConstantService.registerNew(bindingNormalizedNodeSerializer, domRpcService,
                        input.getConstant(), input.getContext());

        routedRegistrations.put(input.getContext(), rpcRegistration);
        return RpcResultBuilder.success(new RegisterBoundConstantOutputBuilder().build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<RegisterFlappingSingletonOutput>> registerFlappingSingleton(
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

    @Override
    public ListenableFuture<RpcResult<UnsubscribeDtclOutput>> unsubscribeDtcl(final UnsubscribeDtclInput input) {
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

            final boolean nodesEqual = idIntsListener.checkEqual(readResult.get());
            if (!nodesEqual) {
                LOG.error("Final read of id-int does not match IdIntsListener's copy. {}",
                        idIntsListener.diffWithLocalCopy(readResult.get()));
            }

            return RpcResultBuilder.success(new UnsubscribeDtclOutputBuilder().setCopyMatches(nodesEqual).build())
                    .buildFuture();

        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Final read of id-ints failed", e);
            return RpcResultBuilder.<UnsubscribeDtclOutput>failed().withError(ErrorType.APPLICATION,
                    "Final read of id-ints failed", e).buildFuture();
        }
    }

    @Override
    public ListenableFuture<RpcResult<UnsubscribeYnlOutput>> unsubscribeYnl(final UnsubscribeYnlInput input) {
        LOG.info("In unsubscribeYnl - input: {}", input);

        if (!ynlRegistrations.containsKey(input.getId())) {
            return RpcResultBuilder.<UnsubscribeYnlOutput>failed()
                .withError(ErrorType.RPC, ErrorTag.DATA_MISSING,
                    "No prior listener was registered for " + input.getId())
                .buildFuture();
        }

        final ListenerRegistration<YnlListener> reg = ynlRegistrations.remove(input.getId());
        final UnsubscribeYnlOutput output = reg.getInstance().getOutput();

        reg.close();

        return RpcResultBuilder.<UnsubscribeYnlOutput>success().withResult(output).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<CheckPublishNotificationsOutput>> checkPublishNotifications(
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

    @Override
    public ListenableFuture<RpcResult<ShutdownShardReplicaOutput>> shutdownShardReplica(
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
                .getElectionTimeOutInterval().$times(3).toMillis(), 10000);
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

    @Override
    public ListenableFuture<RpcResult<UnregisterDefaultConstantOutput>> unregisterDefaultConstant(
            final UnregisterDefaultConstantInput input) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<RpcResult<UnsubscribeDdtlOutput>> unsubscribeDdtl(final UnsubscribeDdtlInput input) {
        throw new UnsupportedOperationException();
    }
}
