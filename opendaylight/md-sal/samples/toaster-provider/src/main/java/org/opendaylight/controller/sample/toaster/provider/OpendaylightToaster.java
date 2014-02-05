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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.config.yang.config.toaster_provider.impl.ToasterProviderRuntimeMXBean;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.common.util.Futures;
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
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpendaylightToaster implements ToasterData, ToasterService, ToasterProviderRuntimeMXBean {

    private static final Logger log = LoggerFactory.getLogger(OpendaylightToaster.class);

    private static final DisplayString toasterManufacturer = new DisplayString("Opendaylight");
    private static final DisplayString toasterModelNumber = new DisplayString("Model 1 - Binding Aware");
    private ToasterStatus toasterStatus;

    private NotificationProviderService notificationProvider;
    private final ExecutorService executor;

    private Future<RpcResult<Void>> currentTask;

    public OpendaylightToaster() {
        toasterStatus = ToasterStatus.Down;
        executor = Executors.newFixedThreadPool(1);
    }

    @Override
    public Toaster getToaster() {
        ToasterBuilder tb = new ToasterBuilder();
        tb //
        .setToasterManufacturer(toasterManufacturer) //
                .setToasterModelNumber(toasterModelNumber) //
                .setToasterStatus(toasterStatus);

        return tb.build();
    }

    @Override
    public Future<RpcResult<Void>> cancelToast() {
        if (currentTask != null) {
            cancelToastImpl();
        }
        return null;
    }

    @Override
    public Future<RpcResult<Void>> makeToast(MakeToastInput input) {
        // TODO Auto-generated method stub
        log.trace("makeToast - Received input for toast");
        logToastInput(input);
        if (currentTask != null) {
            return inProgressError();
        }
        currentTask = executor.submit(new MakeToastTask(input));
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
        notificationProvider.notify(toastDone.build());
    }

    public void setNotificationProvider(NotificationProviderService salService) {
        this.notificationProvider = salService;
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

    private class MakeToastTask implements Callable<RpcResult<Void>> {

        final MakeToastInput toastRequest;

        public MakeToastTask(MakeToastInput toast) {
            toastRequest = toast;
        }

        @Override
        public RpcResult<Void> call() throws Exception {
            Thread.sleep(1000);

            ToastDoneBuilder notifyBuilder = new ToastDoneBuilder();
            notifyBuilder.setToastStatus(ToastStatus.Done);
            notificationProvider.notify(notifyBuilder.build());
            log.trace("Toast Done");
            logToastInput(toastRequest);
            currentTask = null;

            toastsMade.incrementAndGet();

            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }
    }
}
