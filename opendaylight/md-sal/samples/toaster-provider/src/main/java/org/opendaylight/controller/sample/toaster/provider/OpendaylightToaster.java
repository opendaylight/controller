/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.provider;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.config.yang.config.toaster_provider.impl.ToasterProviderRuntimeMXBean;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.DisplayString;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastDone.ToastStatus;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastDoneBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster.ToasterStatus;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterData;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

public class OpendaylightToaster implements ToasterData, ToasterService, ToasterProviderRuntimeMXBean, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OpendaylightToaster.class);
    private static final InstanceIdentifier<Toaster>  toasterIID = InstanceIdentifier.builder(Toaster.class).build();

    private static final DisplayString toasterManufacturer = new DisplayString("Opendaylight");
    private static final DisplayString toasterModelNumber = new DisplayString("Model 1 - Binding Aware");

    private NotificationProviderService notificationProvider;
    private DataBrokerService dataProvider;
    private final ExecutorService executor;

    private Future<RpcResult<Void>> currentTask;

    public OpendaylightToaster() {
        executor = Executors.newFixedThreadPool(1);
    }

    @Override
    public synchronized Toaster getToaster() {
        ToasterBuilder tb = new ToasterBuilder();
        tb //
        .setToasterManufacturer(toasterManufacturer) //
        .setToasterModelNumber(toasterModelNumber) //
        .setToasterStatus(currentTask == null ? ToasterStatus.Up : ToasterStatus.Down);

        return tb.build();
    }

    @Override
    public synchronized Future<RpcResult<Void>> cancelToast() {
        if (currentTask != null) {
            cancelToastImpl();
        }
        return null;
    }

    @Override
    public synchronized Future<RpcResult<Void>> makeToast(MakeToastInput input) {
        log.debug("makeToast - Received input for toast");
        logToastInput(input);
        if (currentTask != null) {
            return inProgressError();
        }
        currentTask = executor.submit(new MakeToastTask(input));
        updateStatus();
        return currentTask;
    }

    private Future<RpcResult<Void>> inProgressError() {
        RpcResult<Void> result = Rpcs.<Void> getRpcResult(false, null, Collections.<RpcError> emptySet());
        return Futures.immediateFuture(result);
    }

    private void cancelToastImpl() {
        currentTask.cancel(true);
        ToastDoneBuilder toastDone = new ToastDoneBuilder();
        toastDone.setToastStatus(ToastStatus.Cancelled);
        notificationProvider.publish(toastDone.build());
    }

    public void setNotificationProvider(NotificationProviderService salService) {
        this.notificationProvider = salService;
    }

    public void setDataProvider(DataBrokerService salDataProvider) {
        this.dataProvider = salDataProvider;
        updateStatus();
    }

    private void logToastInput(MakeToastInput input) {
        String toastType = input.getToasterToastType().getName();
        String toastDoneness = input.getToasterDoneness().toString();
        log.trace("Toast: {} doneness: {}", toastType, toastDoneness);
    }

    private final AtomicLong toastsMade = new AtomicLong(0);

    @Override
    public Long getToastsMade() {
        return toastsMade.get();
    }

    private void updateStatus() {
        if (dataProvider != null) {
            final DataModificationTransaction t = dataProvider.beginTransaction();
            t.removeOperationalData(toasterIID);
            t.putOperationalData(toasterIID, getToaster());

            try {
                t.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to update toaster status, operational otherwise", e);
            }
        } else {
            log.trace("No data provider configured, not updating status");
        }
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        if (dataProvider != null) {
            final DataModificationTransaction t = dataProvider.beginTransaction();
            t.removeOperationalData(toasterIID);
            t.commit().get();
        }
    }

    private class MakeToastTask implements Callable<RpcResult<Void>> {

        final MakeToastInput toastRequest;

        public MakeToastTask(MakeToastInput toast) {
            toastRequest = toast;
        }

        @Override
        public RpcResult<Void> call() throws InterruptedException {
            Thread.sleep(1000 * toastRequest.getToasterDoneness());

            ToastDoneBuilder notifyBuilder = new ToastDoneBuilder();
            notifyBuilder.setToastStatus(ToastStatus.Done);
            notificationProvider.publish(notifyBuilder.build());
            log.debug("Toast Done");
            logToastInput(toastRequest);

            currentTask = null;
            toastsMade.incrementAndGet();
            updateStatus();

            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }
    }
}
