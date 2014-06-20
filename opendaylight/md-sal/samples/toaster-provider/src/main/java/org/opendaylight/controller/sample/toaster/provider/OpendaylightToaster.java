/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.config.yang.config.toaster_provider.impl.ToasterProviderRuntimeMXBean;
import org.opendaylight.controller.md.sal.binding.api.BindingDataBroker;
import org.opendaylight.controller.md.sal.binding.api.BindingDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.BindingDataWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.controller.sal.common.util.Rpcs;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

public class OpendaylightToaster implements ToasterService, ToasterProviderRuntimeMXBean,
                                            BindingDataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightToaster.class);

    public static final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.builder(Toaster.class).build();

    private static final DisplayString TOASTER_MANUFACTURER = new DisplayString("Opendaylight");
    private static final DisplayString TOASTER_MODEL_NUMBER = new DisplayString("Model 1 - Binding Aware");

    private NotificationProviderService notificationProvider;
    private BindingDataBroker dataProvider;

    private final ExecutorService executor;

    // As you will see we are using multiple threads here. Therefore we need to be careful about concurrency.
    // In this case we use the taskLock to provide synchronization for the current task.
    private volatile Future<RpcResult<Void>> currentTask;
    private final Object taskLock = new Object();

    private final AtomicLong amountOfBreadInStock = new AtomicLong( 100 );

    private final AtomicLong toastsMade = new AtomicLong(0);

    // Thread safe holder for our darkness multiplier.
    private final AtomicLong darknessFactor = new AtomicLong( 1000 );

    public OpendaylightToaster() {
        executor = Executors.newFixedThreadPool(1);
    }

    public void setNotificationProvider(final NotificationProviderService salService) {
        this.notificationProvider = salService;
    }

    public void setDataProvider(final BindingDataBroker salDataProvider) {
        this.dataProvider = salDataProvider;
        updateStatus();
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
        executor.shutdown();

        if (dataProvider != null) {
            BindingDataWriteTransaction t = dataProvider.newWriteOnlyTransaction();
            t.delete(LogicalDatastoreType.OPERATIONAL,TOASTER_IID);
            t.commit().get(); // FIXME: This call should not be blocking.
        }
    }

    private Toaster buildToaster() {
        // We don't need to synchronize on currentTask here b/c it's declared volatile and
        // we're just doing a read.
        boolean isUp = currentTask == null;

        // note - we are simulating a device whose manufacture and model are
        // fixed (embedded) into the hardware.
        // This is why the manufacture and model number are hardcoded.
        ToasterBuilder tb = new ToasterBuilder();
        tb.setToasterManufacturer(TOASTER_MANUFACTURER).setToasterModelNumber(TOASTER_MODEL_NUMBER)
                .setToasterStatus(isUp ? ToasterStatus.Up : ToasterStatus.Down);
        return tb.build();
    }

    /**
     * Implemented from the DataChangeListener interface.
     */
    @Override
    public void onDataChanged( final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change ) {
        DataObject dataObject = change.getUpdatedSubtree();
        if( dataObject instanceof Toaster )
        {
            Toaster toaster = (Toaster) dataObject;
            Long darkness = toaster.getDarknessFactor();
            if( darkness != null )
            {
                darknessFactor.set( darkness );
            }
        }
    }

    /**
     * RestConf RPC call implemented from the ToasterService interface.
     */
    @Override
    public Future<RpcResult<Void>> cancelToast() {
        synchronized (taskLock) {
            if (currentTask != null) {
                currentTask.cancel(true);
                currentTask = null;
            }
        }
        // Always return success from the cancel toast call.
        return Futures.immediateFuture(Rpcs.<Void> getRpcResult(true, Collections.<RpcError> emptySet()));
    }

    /**
     * RestConf RPC call implemented from the ToasterService interface.
     */
    @Override
    public Future<RpcResult<Void>> makeToast(final MakeToastInput input) {
        LOG.info("makeToast: " + input);

        synchronized (taskLock) {
            if (currentTask != null) {
                // return an error since we are already toasting some toast.
                LOG.info( "Toaster is already making toast" );

                RpcResult<Void> result = Rpcs.<Void> getRpcResult(false, null, Arrays.asList(
                        RpcErrors.getRpcError( "", "in-use", null, ErrorSeverity.WARNING,
                                               "Toaster is busy", ErrorType.APPLICATION, null ) ) );
                return Futures.immediateFuture(result);
            }
            else if( outOfBread() ) {
                RpcResult<Void> result = Rpcs.<Void> getRpcResult(false, null, Arrays.asList(
                        RpcErrors.getRpcError( "out-of-stock", "resource-denied", null, null,
                                               "Toaster is out of bread",
                                               ErrorType.APPLICATION, null ) ) );
                return Futures.immediateFuture(result);
            }
            else {
                // Notice that we are moving the actual call to another thread,
                // allowing this thread to return immediately.
                // The MD-SAL design encourages asynchronus programming. If the
                // caller needs to block until the call is
                // complete then they can leverage the blocking methods on the
                // Future interface.
                currentTask = executor.submit(new MakeToastTask(input));
            }
        }

        updateStatus();
        return currentTask;
    }

    /**
     * RestConf RPC call implemented from the ToasterService interface.
     * Restocks the bread for the toaster, resets the toastsMade counter to 0, and sends a
     * ToasterRestocked notification.
     */
    @Override
    public Future<RpcResult<java.lang.Void>> restockToaster(final RestockToasterInput input) {
        LOG.info( "restockToaster: " + input );

        synchronized( taskLock ) {
            amountOfBreadInStock.set( input.getAmountOfBreadToStock() );

            if( amountOfBreadInStock.get() > 0 ) {
                ToasterRestocked reStockedNotification =
                    new ToasterRestockedBuilder().setAmountOfBread( input.getAmountOfBreadToStock() ).build();
                notificationProvider.publish( reStockedNotification );
            }
        }

        return Futures.immediateFuture(Rpcs.<Void> getRpcResult(true, Collections.<RpcError> emptySet()));
    }

    /**
     * JMX RPC call implemented from the ToasterProviderRuntimeMXBean interface.
     */
    @Override
    public void clearToastsMade() {
        LOG.info( "clearToastsMade" );
        toastsMade.set( 0 );
    }

    /**
     * Accesssor method implemented from the ToasterProviderRuntimeMXBean interface.
     */
    @Override
    public Long getToastsMade() {
        return toastsMade.get();
    }

    private void updateStatus() {
        if (dataProvider != null) {
            BindingDataWriteTransaction tx = dataProvider.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.OPERATIONAL,TOASTER_IID, buildToaster());

            try {
                tx.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Failed to update toaster status, operational otherwise", e);
            }
        } else {
            LOG.trace("No data provider configured, not updating status");
        }
    }

    private boolean outOfBread()
    {
        return amountOfBreadInStock.get() == 0;
    }

    private class MakeToastTask implements Callable<RpcResult<Void>> {

        final MakeToastInput toastRequest;

        public MakeToastTask(final MakeToastInput toast) {
            toastRequest = toast;
        }

        @Override
        public RpcResult<Void> call() {
            try
            {
                // make toast just sleeps for n secondn per doneness level.
                long darknessFactor = OpendaylightToaster.this.darknessFactor.get();
                Thread.sleep(darknessFactor * toastRequest.getToasterDoneness());

            }
            catch( InterruptedException e ) {
                LOG.info( "Interrupted while making the toast" );
            }

            toastsMade.incrementAndGet();

            amountOfBreadInStock.getAndDecrement();
            if( outOfBread() ) {
                LOG.info( "Toaster is out of bread!" );

                notificationProvider.publish( new ToasterOutOfBreadBuilder().build() );
            }

            synchronized (taskLock) {
                currentTask = null;
            }

            updateStatus();

            LOG.debug("Toast done");

            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }
    }
}
