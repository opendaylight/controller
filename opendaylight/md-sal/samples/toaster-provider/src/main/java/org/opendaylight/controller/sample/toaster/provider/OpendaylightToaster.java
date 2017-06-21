/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.provider;

import static org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType.DELETE;
import static org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType.WRITE;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.yangtools.yang.common.RpcError.ErrorType.APPLICATION;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.DisplayString;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.RestockToasterInput;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpendaylightToaster extends AbstractMXBean
        implements ToasterService, ToasterProviderRuntimeMXBean, DataTreeChangeListener<Toaster>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightToaster.class);

    private static final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.builder(Toaster.class).build();
    private static final DisplayString TOASTER_MANUFACTURER = new DisplayString("Opendaylight");
    private static final DisplayString TOASTER_MODEL_NUMBER = new DisplayString("Model 1 - Binding Aware");

    private DataBroker dataBroker;
    private NotificationPublishService notificationProvider;
    private ListenerRegistration<OpendaylightToaster> dataTreeChangeListenerRegistration;

    private final ExecutorService executor;

    // This holds the Future for the current make toast task and is used to cancel the current toast.
    private final AtomicReference<Future<?>> currentMakeToastTask = new AtomicReference<>();

    // Thread safe holders
    private final AtomicLong amountOfBreadInStock = new AtomicLong(100);
    private final AtomicLong toastsMade = new AtomicLong(0);
    private final AtomicLong darknessFactor = new AtomicLong(1000);

    private final ToasterAppConfig toasterAppConfig;

    public OpendaylightToaster() {
        this(new ToasterAppConfigBuilder().setManufacturer(TOASTER_MANUFACTURER).setModelNumber(TOASTER_MODEL_NUMBER)
                .setMaxMakeToastTries(2).build());
    }

    public OpendaylightToaster(ToasterAppConfig toasterAppConfig) {
        super("OpendaylightToaster", "toaster-provider", null);
        executor = Executors.newFixedThreadPool(1);
        this.toasterAppConfig = toasterAppConfig;
    }

    public void setNotificationProvider(final NotificationPublishService notificationPublishService) {
        this.notificationProvider = notificationPublishService;
    }

    public void setDataBroker(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        LOG.info("Initializing...");

        Preconditions.checkNotNull(dataBroker, "dataBroker must be set");
        dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(CONFIGURATION, TOASTER_IID), this);
        setToasterStatusUp(null);

        // Register our MXBean.
        register();
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() {
        LOG.info("Closing...");

        // Unregister our MXBean.
        unregister();

        // When we close this service we need to shutdown our executor!
        executor.shutdown();

        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }

        if (dataBroker != null) {
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            tx.delete(OPERATIONAL,TOASTER_IID);
            Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.debug("Successfully deleted the operational Toaster");
                }

                @Override
                public void onFailure(final Throwable failure) {
                    LOG.error("Delete of the operational Toaster failed", failure);
                }
            });
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
    public void onDataTreeChanged(Collection<DataTreeModification<Toaster>> changes) {
        for (DataTreeModification<Toaster> change: changes) {
            DataObjectModification<Toaster> rootNode = change.getRootNode();
            if (rootNode.getModificationType() == WRITE) {
                Toaster oldToaster = rootNode.getDataBefore();
                Toaster newToaster = rootNode.getDataAfter();
                LOG.info("onDataTreeChanged - Toaster config with path {} was added or replaced: "
                        + "old Toaster: {}, new Toaster: {}", change.getRootPath().getRootIdentifier(),
                        oldToaster, newToaster);

                Long darkness = newToaster.getDarknessFactor();
                if (darkness != null) {
                    darknessFactor.set(darkness);
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
    public Future<RpcResult<Void>> cancelToast() {
        Future<?> current = currentMakeToastTask.getAndSet(null);
        if (current != null) {
            current.cancel(true);
        }

        // Always return success from the cancel toast call
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    /**
     * RPC call implemented from the ToasterService interface that attempts to make toast.
     */
    @Override
    public Future<RpcResult<Void>> makeToast(final MakeToastInput input) {
        LOG.info("makeToast: " + input);

        final SettableFuture<RpcResult<Void>> futureResult = SettableFuture.create();

        checkStatusAndMakeToast(input, futureResult, toasterAppConfig.getMaxMakeToastTries());

        return futureResult;
    }

    private RpcError makeToasterOutOfBreadError() {
        return RpcResultBuilder.newError(APPLICATION, "resource-denied", "Toaster is out of bread", "out-of-stock",
                null, null);
    }

    private RpcError makeToasterInUseError() {
        return RpcResultBuilder.newWarning(APPLICATION, "in-use", "Toaster is busy", null, null, null);
    }

    private void checkStatusAndMakeToast(final MakeToastInput input, final SettableFuture<RpcResult<Void>> futureResult,
            final int tries) {
        // Read the ToasterStatus and, if currently Up, try to write the status to Down.
        // If that succeeds, then we essentially have an exclusive lock and can proceed
        // to make toast.
        final ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        ListenableFuture<Optional<Toaster>> readFuture = tx.read(OPERATIONAL, TOASTER_IID);

        final ListenableFuture<Void> commitFuture =
            Futures.transformAsync(readFuture, toasterData -> {
                ToasterStatus toasterStatus = ToasterStatus.Up;
                if (toasterData.isPresent()) {
                    toasterStatus = toasterData.get().getToasterStatus();
                }

                LOG.debug("Read toaster status: {}", toasterStatus);

                if (toasterStatus == ToasterStatus.Up) {

                    if (outOfBread()) {
                        LOG.debug("Toaster is out of bread");
                        return Futures.immediateFailedCheckedFuture(
                                new TransactionCommitFailedException("", makeToasterOutOfBreadError()));
                    }

                    LOG.debug("Setting Toaster status to Down");

                    // We're not currently making toast - try to update the status to Down
                    // to indicate we're going to make toast. This acts as a lock to prevent
                    // concurrent toasting.
                    tx.put(OPERATIONAL, TOASTER_IID, buildToaster(ToasterStatus.Down));
                    return tx.submit();
                }

                LOG.debug("Oops - already making toast!");

                // Return an error since we are already making toast. This will get
                // propagated to the commitFuture below which will interpret the null
                // TransactionStatus in the RpcResult as an error condition.
                return Futures.immediateFailedCheckedFuture(
                        new TransactionCommitFailedException("", makeToasterInUseError()));
            });

        Futures.addCallback(commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
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
                        futureResult.set(RpcResultBuilder.<Void>failed()
                                .withError(ErrorType.APPLICATION, ex.getMessage()).build());
                    }
                } else if (ex instanceof TransactionCommitFailedException) {
                    LOG.debug("Failed to commit Toaster status", ex);

                    // Probably already making toast.
                    futureResult.set(RpcResultBuilder.<Void>failed()
                            .withRpcErrors(((TransactionCommitFailedException)ex).getErrorList()).build());
                } else {
                    LOG.debug("Unexpected error committing Toaster status", ex);
                    futureResult.set(RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION,
                            "Unexpected error committing Toaster status", ex).build());
                }
            }
        });
    }

    /**
     * RestConf RPC call implemented from the ToasterService interface.
     * Restocks the bread for the toaster, resets the toastsMade counter to 0, and sends a
     * ToasterRestocked notification.
     */
    @Override
    public Future<RpcResult<java.lang.Void>> restockToaster(final RestockToasterInput input) {
        LOG.info("restockToaster: " + input);

        amountOfBreadInStock.set(input.getAmountOfBreadToStock());

        if (amountOfBreadInStock.get() > 0) {
            ToasterRestocked reStockedNotification = new ToasterRestockedBuilder()
                    .setAmountOfBread(input.getAmountOfBreadToStock()).build();
            notificationProvider.offerNotification(reStockedNotification);
        }

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
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

    private void setToasterStatusUp(final Function<Boolean,Void> resultCallback) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(OPERATIONAL,TOASTER_IID, buildToaster(ToasterStatus.Up));

        Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
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
        });
    }

    private boolean outOfBread() {
        return amountOfBreadInStock.get() == 0;
    }

    private class MakeToastTask implements Callable<Void> {

        final MakeToastInput toastRequest;
        final SettableFuture<RpcResult<Void>> futureResult;

        MakeToastTask(final MakeToastInput toastRequest, final SettableFuture<RpcResult<Void>> futureResult) {
            this.toastRequest = toastRequest;
            this.futureResult = futureResult;
        }

        @Override
        public Void call() {
            try {
                // make toast just sleeps for n seconds per doneness level.
                Thread.sleep(OpendaylightToaster.this.darknessFactor.get() * toastRequest.getToasterDoneness());

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
                futureResult.set(RpcResultBuilder.<Void>success().build());
                return null;
            });

            return null;
        }
    }
}
