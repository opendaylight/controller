/*
 * Copyright (c) 2014, 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.kitchen.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.sample.kitchen.api.EggsType;
import org.opendaylight.controller.sample.kitchen.api.KitchenService;
import org.opendaylight.controller.sample.kitchen.api.KitchenServiceRuntimeMXBean;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastOutputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterListener;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterOutOfBread;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestocked;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.WheatBread;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KitchenServiceImpl extends AbstractMXBean
        implements KitchenService, KitchenServiceRuntimeMXBean, ToasterListener {

    private static final Logger LOG = LoggerFactory.getLogger(KitchenServiceImpl.class);
    private static final MakeToastOutput EMPTY_MAKE_OUTPUT = new MakeToastOutputBuilder().build();

    private final ToasterService toaster;

    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    private volatile boolean toasterOutOfBread;

    public KitchenServiceImpl(final ToasterService toaster) {
        super("KitchenService", "toaster-consumer", null);
        this.toaster = toaster;
    }

    @Override
    public Future<RpcResult<Void>> makeBreakfast(final EggsType eggsType, final Class<? extends ToastType> toastType,
            final int toastDoneness) {
        // Call makeToast, The OpendaylightToaster impl already returns a ListenableFuture so the conversion is
        // actually a no-op.

        ListenableFuture<RpcResult<MakeToastOutput>> makeToastFuture = makeToast(toastType, toastDoneness);

        ListenableFuture<RpcResult<Void>> makeEggsFuture = makeEggs(eggsType);

        // Combine the 2 ListenableFutures into 1 containing a list RpcResults.

        ListenableFuture<List<RpcResult<? extends Object>>> combinedFutures = Futures
                .allAsList(ImmutableList.of(makeToastFuture, makeEggsFuture));

        // Then transform the RpcResults into 1.

        return Futures.transformAsync(combinedFutures, results -> {
            boolean atLeastOneSucceeded = false;
            Builder<RpcError> errorList = ImmutableList.builder();
            for (RpcResult<? extends Object> result : results) {
                if (result.isSuccessful()) {
                    atLeastOneSucceeded = true;
                }

                if (result.getErrors() != null) {
                    errorList.addAll(result.getErrors());
                }
            }

            return Futures.immediateFuture(RpcResultBuilder.<Void>status(atLeastOneSucceeded)
                    .withRpcErrors(errorList.build()).build());
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<RpcResult<Void>> makeEggs(final EggsType eggsType) {
        return executor.submit(() -> RpcResultBuilder.<Void>success().build());
    }

    private ListenableFuture<RpcResult<MakeToastOutput>> makeToast(final Class<? extends ToastType> toastType,
            final int toastDoneness) {

        if (toasterOutOfBread) {
            LOG.info("We're out of toast but we can make eggs");
            return Futures.immediateFuture(RpcResultBuilder.success(EMPTY_MAKE_OUTPUT)
                .withWarning(ErrorType.APPLICATION, "partial-operation",
                    "Toaster is out of bread but we can make you eggs").build());
        }

        // Access the ToasterService to make the toast.

        MakeToastInput toastInput = new MakeToastInputBuilder().setToasterDoneness((long) toastDoneness)
                .setToasterToastType(toastType).build();

        return toaster.makeToast(toastInput);
    }

    @Override
    public Boolean makeScrambledWithWheat() {
        try {
            // This call has to block since we must return a result to the JMX client.
            RpcResult<Void> result = makeBreakfast(EggsType.SCRAMBLED, WheatBread.class, 2).get();
            if (result.isSuccessful()) {
                LOG.info("makeBreakfast succeeded");
            } else {
                LOG.warn("makeBreakfast failed: {}", result.getErrors());
            }

            return result.isSuccessful();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("An error occurred while maing breakfast", e);
        }

        return Boolean.FALSE;
    }

    /**
     * Implemented from the ToasterListener interface.
     */
    @Override
    public void onToasterOutOfBread(final ToasterOutOfBread notification) {
        LOG.info("ToasterOutOfBread notification");
        toasterOutOfBread = true;
    }

    /**
     * Implemented from the ToasterListener interface.
     */
    @Override
    public void onToasterRestocked(final ToasterRestocked notification) {
        LOG.info("ToasterRestocked notification - amountOfBread: {}", notification.getAmountOfBread());
        toasterOutOfBread = false;
    }
}
