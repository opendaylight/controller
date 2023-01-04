/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.provider;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType.DELETE;
import static org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType.WRITE;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.yangtools.yang.common.ErrorType.APPLICATION;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.CancelToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.CancelToastOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.CancelToastOutputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.DisplayString;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastOutputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.RestockToasterInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.RestockToasterOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.RestockToasterOutputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster.ToasterStatus;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterOutOfBreadBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestocked;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestockedBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.toaster.app.config.rev160503.ToasterAppConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.toaster.app.config.rev160503.ToasterAppConfigBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = ToasterService.class, immediate = true)
@Designate(ocd = OpendaylightToaster.Configuration.class)
public final class OpendaylightToaster extends AbstractMXBean
        implements ToasterService, ToasterProviderRuntimeMXBean, DataTreeChangeListener<Toaster>, AutoCloseable {
    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition(description = "The name of the toaster's manufacturer", max = "255")
        String manufacturer() default "Opendaylight";
        @AttributeDefinition(description = "The name of the toaster's model", max = "255")
        String modelNumber() default "Model 1 - Binding Aware";
        @AttributeDefinition(description = "How many times we attempt to make toast before failing ")
        byte maxMakeToastTries() default 2;
    }

    private static final CancelToastOutput EMPTY_CANCEL_OUTPUT = new CancelToastOutputBuilder().build();
    private static final MakeToastOutput EMPTY_MAKE_OUTPUT = new MakeToastOutputBuilder().build();
    private static final RestockToasterOutput EMPTY_RESTOCK_OUTPUT = new RestockToasterOutputBuilder().build();

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightToaster.class);

    private static final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.builder(Toaster.class).build();
    private static final DisplayString TOASTER_MANUFACTURER = new DisplayString("Opendaylight");
    private static final DisplayString TOASTER_MODEL_NUMBER = new DisplayString("Model 1 - Binding Aware");

    private DataBroker dataBroker;
    private NotificationPublishService notificationProvider;
    private ListenerRegistration<OpendaylightToaster> dataTreeChangeListenerRegistration;
    private Set<ObjectRegistration<?>> regs;

    private final ExecutorService executor;

    // This holds the Future for the current make toast task and is used to cancel the current toast.
    private final AtomicReference<Future<?>> currentMakeToastTask = new AtomicReference<>();

    // Thread safe holders
    private final AtomicLong amountOfBreadInStock = new AtomicLong(100);
    private final AtomicLong toastsMade = new AtomicLong(0);
    private final AtomicLong darknessFactor = new AtomicLong(1000);

    private final ToasterAppConfig toasterAppConfig;

    public OpendaylightToaster() {
        super("OpendaylightToaster", "toaster-provider", null);
        this.executor = Executors.newFixedThreadPool(1);
        this.toasterAppConfig = new ToasterAppConfigBuilder()
                .setManufacturer(TOASTER_MANUFACTURER)
                .setModelNumber(TOASTER_MODEL_NUMBER)
                .setMaxMakeToastTries(Uint16.valueOf(2)).build();
    }

    @Inject
    @Activate
    public OpendaylightToaster(@Reference final DataBroker dataProvider,
            @Reference final NotificationPublishService notificationPublishService,
            @Reference final RpcProviderService rpcProviderService, final @NonNull Configuration configuration) {
        super("OpendaylightToaster", "toaster-provider", null);
        this.executor = Executors.newFixedThreadPool(1);
        this.toasterAppConfig = new ToasterAppConfigBuilder()
                .setManufacturer(new DisplayString(configuration.manufacturer()))
                .setModelNumber(new DisplayString(configuration.modelNumber()))
                .setMaxMakeToastTries(Uint16.valueOf(configuration.maxMakeToastTries())).build();
        this.notificationProvider = notificationPublishService;
        this.dataBroker = dataProvider;
        this.regs = ImmutableSet.of(rpcProviderService.registerRpcImplementation(ToasterService.class, this));
        init();
    }

    public void setNotificationProvider(final NotificationPublishService notificationPublishService) {
        this.notificationProvider = notificationPublishService;
    }

    public void setDataBroker(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        LOG.info("Initializing...");

        this.dataTreeChangeListenerRegistration = requireNonNull(dataBroker, "dataBroker must be set")
            .registerDataTreeChangeListener(DataTreeIdentifier.create(CONFIGURATION, TOASTER_IID), this);
        setToasterStatusUp(null);

        // Register our MXBean.
        register();
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    @PreDestroy
    @Deactivate
    public void close() {
        LOG.info("Closing...");

        // Unregister our MXBean.
        unregister();
        regs.forEach(ObjectRegistration::close);

        // When we close this service we need to shutdown our executor!
        executor.shutdown();

        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }

        if (dataBroker != null) {
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            tx.delete(OPERATIONAL,TOASTER_IID);
            Futures.addCallback(tx.commit(), new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.debug("Successfully deleted the operational Toaster");
                }

                @Override
                public void onFailure(final Throwable failure) {
                    LOG.error("Delete of the operational Toaster failed", failure);
                }
            }, MoreExecutors.directExecutor());
        }
    }

    private Toaster buildToaster(final ToasterStatus status) {
        // note - we are simulating a device whose manufacture and model are
        // fixed (embedded) into the hardware.
        // This is why the manufacture and model number are hardcoded.
        return new ToasterBuilder().setToasterManufacturer(toasterAppConfig.getManufacturer())
                .setToasterModelNumber(toasterAppConfig.getModelNumber()).setToasterStatus(status).build();
    }

    /**
     * Implemented from the DataTreeChangeListener interface.
     */
    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Toaster>> changes) {
        for (DataTreeModification<Toaster> change: changes) {
            DataObjectModification<Toaster> rootNode = change.getRootNode();
            if (rootNode.getModificationType() == WRITE) {
                Toaster oldToaster = rootNode.getDataBefore();
                Toaster newToaster = rootNode.getDataAfter();
                LOG.info("onDataTreeChanged - Toaster config with path {} was added or replaced: "
                        + "old Toaster: {}, new Toaster: {}", change.getRootPath().getRootIdentifier(),
                        oldToaster, newToaster);

                Uint32 darkness = newToaster.getDarknessFactor();
                if (darkness != null) {
                    darknessFactor.set(darkness.toJava());
                }
            } else if (rootNode.getModificationType() == DELETE) {
                LOG.info("onDataTreeChanged - Toaster config with path {} was deleted: old Toaster: {}",
                        change.getRootPath().getRootIdentifier(), rootNode.getDataBefore());
            }
        }
    }

    /**
     * RPC call implemented from the ToasterService interface that cancels the current toast, if any.
     */
    @Override
    public ListenableFuture<RpcResult<CancelToastOutput>> cancelToast(final CancelToastInput input) {
        Future<?> current = currentMakeToastTask.getAndSet(null);
        if (current != null) {
            current.cancel(true);
        }

        // Always return success from the cancel toast call
        return Futures.immediateFuture(RpcResultBuilder.success(EMPTY_CANCEL_OUTPUT).build());
    }

    /**
     * RPC call implemented from the ToasterService interface that attempts to make toast.
     */
    @Override
    public ListenableFuture<RpcResult<MakeToastOutput>> makeToast(final MakeToastInput input) {
        LOG.info("makeToast: {}", input);

        final SettableFuture<RpcResult<MakeToastOutput>> futureResult = SettableFuture.create();

        checkStatusAndMakeToast(input, futureResult, toasterAppConfig.getMaxMakeToastTries().toJava());

        return futureResult;
    }

    private static RpcError makeToasterOutOfBreadError() {
        return RpcResultBuilder.newError(APPLICATION, ErrorTag.RESOURCE_DENIED, "Toaster is out of bread",
            "out-of-stock", null, null);
    }

    private static RpcError makeToasterInUseError() {
        return RpcResultBuilder.newWarning(APPLICATION, ErrorTag.IN_USE, "Toaster is busy", null, null, null);
    }

    private void checkStatusAndMakeToast(final MakeToastInput input,
            final SettableFuture<RpcResult<MakeToastOutput>> futureResult, final int tries) {
        // Read the ToasterStatus and, if currently Up, try to write the status to Down.
        // If that succeeds, then we essentially have an exclusive lock and can proceed
        // to make toast.
        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        FluentFuture<Optional<Toaster>> readFuture = tx.read(OPERATIONAL, TOASTER_IID);

        final ListenableFuture<? extends CommitInfo> commitFuture =
            Futures.transformAsync(readFuture, toasterData -> {
                ToasterStatus toasterStatus = ToasterStatus.Up;
                if (toasterData.isPresent()) {
                    toasterStatus = toasterData.get().getToasterStatus();
                }

                LOG.debug("Read toaster status: {}", toasterStatus);

                if (toasterStatus == ToasterStatus.Up) {

                    if (outOfBread()) {
                        LOG.debug("Toaster is out of bread");
                        tx.cancel();
                        return Futures.immediateFailedFuture(
                                new TransactionCommitFailedException("", makeToasterOutOfBreadError()));
                    }

                    LOG.debug("Setting Toaster status to Down");

                    // We're not currently making toast - try to update the status to Down
                    // to indicate we're going to make toast. This acts as a lock to prevent
                    // concurrent toasting.
                    tx.put(OPERATIONAL, TOASTER_IID, buildToaster(ToasterStatus.Down));
                    return tx.commit();
                }

                LOG.debug("Oops - already making toast!");

                // Return an error since we are already making toast. This will get
                // propagated to the commitFuture below which will interpret the null
                // TransactionStatus in the RpcResult as an error condition.
                tx.cancel();
                return Futures.immediateFailedFuture(
                        new TransactionCommitFailedException("", makeToasterInUseError()));
            }, MoreExecutors.directExecutor());

        Futures.addCallback(commitFuture, new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                // OK to make toast
                currentMakeToastTask.set(executor.submit(new MakeToastTask(input, futureResult)));
            }

            @Override
            public void onFailure(final Throwable ex) {
                if (ex instanceof OptimisticLockFailedException) {

                    // Another thread is likely trying to make toast simultaneously and updated the
                    // status before us. Try reading the status again - if another make toast is
                    // now in progress, we should get ToasterStatus.Down and fail.

                    if (tries - 1 > 0) {
                        LOG.debug("Got OptimisticLockFailedException - trying again");
                        checkStatusAndMakeToast(input, futureResult, tries - 1);
                    } else {
                        futureResult.set(RpcResultBuilder.<MakeToastOutput>failed()
                                .withError(ErrorType.APPLICATION, ex.getMessage()).build());
                    }
                } else if (ex instanceof TransactionCommitFailedException) {
                    LOG.debug("Failed to commit Toaster status", ex);

                    // Probably already making toast.
                    futureResult.set(RpcResultBuilder.<MakeToastOutput>failed()
                            .withRpcErrors(((TransactionCommitFailedException)ex).getErrorList()).build());
                } else {
                    LOG.debug("Unexpected error committing Toaster status", ex);
                    futureResult.set(RpcResultBuilder.<MakeToastOutput>failed().withError(ErrorType.APPLICATION,
                            "Unexpected error committing Toaster status", ex).build());
                }
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * RestConf RPC call implemented from the ToasterService interface.
     * Restocks the bread for the toaster, resets the toastsMade counter to 0, and sends a
     * ToasterRestocked notification.
     */
    @Override
    public ListenableFuture<RpcResult<RestockToasterOutput>> restockToaster(final RestockToasterInput input) {
        LOG.info("restockToaster: {}", input);

        amountOfBreadInStock.set(input.getAmountOfBreadToStock().toJava());

        if (amountOfBreadInStock.get() > 0) {
            ToasterRestocked reStockedNotification = new ToasterRestockedBuilder()
                    .setAmountOfBread(input.getAmountOfBreadToStock()).build();
            notificationProvider.offerNotification(reStockedNotification);
        }

        return Futures.immediateFuture(RpcResultBuilder.success(EMPTY_RESTOCK_OUTPUT).build());
    }

    /**
     * JMX RPC call implemented from the ToasterProviderRuntimeMXBean interface.
     */
    @Override
    public void clearToastsMade() {
        LOG.info("clearToastsMade");
        toastsMade.set(0);
    }

    /**
     * Accesssor method implemented from the ToasterProviderRuntimeMXBean interface.
     */
    @Override
    public Long getToastsMade() {
        return toastsMade.get();
    }

    private void setToasterStatusUp(final Function<Boolean, MakeToastOutput> resultCallback) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(OPERATIONAL,TOASTER_IID, buildToaster(ToasterStatus.Up));

        Futures.addCallback(tx.commit(), new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("Successfully set ToasterStatus to Up");
                notifyCallback(true);
            }

            @Override
            public void onFailure(final Throwable failure) {
                // We shouldn't get an OptimisticLockFailedException (or any ex) as no
                // other component should be updating the operational state.
                LOG.error("Failed to update toaster status", failure);

                notifyCallback(false);
            }

            void notifyCallback(final boolean result) {
                if (resultCallback != null) {
                    resultCallback.apply(result);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private boolean outOfBread() {
        return amountOfBreadInStock.get() == 0;
    }

    private class MakeToastTask implements Callable<Void> {

        final MakeToastInput toastRequest;
        final SettableFuture<RpcResult<MakeToastOutput>> futureResult;

        MakeToastTask(final MakeToastInput toastRequest,
            final SettableFuture<RpcResult<MakeToastOutput>> futureResult) {
            this.toastRequest = toastRequest;
            this.futureResult = futureResult;
        }

        @Override
        public Void call() {
            try {
                // make toast just sleeps for n seconds per doneness level.
                Thread.sleep(darknessFactor.get()
                        * toastRequest.getToasterDoneness().toJava());

            } catch (InterruptedException e) {
                LOG.info("Interrupted while making the toast");
            }

            toastsMade.incrementAndGet();

            amountOfBreadInStock.getAndDecrement();
            if (outOfBread()) {
                LOG.info("Toaster is out of bread!");

                notificationProvider.offerNotification(new ToasterOutOfBreadBuilder().build());
            }

            // Set the Toaster status back to up - this essentially releases the toasting lock.
            // We can't clear the current toast task nor set the Future result until the
            // update has been committed so we pass a callback to be notified on completion.

            setToasterStatusUp(result -> {
                currentMakeToastTask.set(null);
                LOG.debug("Toast done");
                futureResult.set(RpcResultBuilder.success(EMPTY_MAKE_OUTPUT).build());
                return null;
            });

            return null;
        }
    }
}
