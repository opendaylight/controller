/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps.getSourceNode;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_GET_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SalFacade proxy that invokes keepalive RPCs to prevent session shutdown from remote device
 * and to detect incorrect session drops (netconf session is inactive, but TCP/SSH connection is still present).
 * The keepalive RPC is a get-config with empty filter.
 */
public final class KeepaliveSalFacade implements RemoteDeviceHandler<NetconfSessionPreferences> {

    private static final Logger LOG = LoggerFactory.getLogger(KeepaliveSalFacade.class);

    // 2 minutes keepalive delay by default
    private static final long DEFAULT_DELAY = TimeUnit.MINUTES.toSeconds(2);

    // 1 minute transaction timeout by default
    private static final long DEFAULT_TRANSACTION_TIMEOUT_MILLI = TimeUnit.MILLISECONDS.toMillis(60000);

    private final RemoteDeviceId id;
    private final RemoteDeviceHandler<NetconfSessionPreferences> salFacade;
    private final ScheduledExecutorService executor;
    private final long keepaliveDelaySeconds;
    private final ResetKeepalive resetKeepaliveTask;
    private final long defaultRequestTimeoutMillis;

    private volatile NetconfDeviceCommunicator listener;
    private volatile ScheduledFuture<?> currentKeepalive;
    private volatile DOMRpcService currentDeviceRpc;

    public KeepaliveSalFacade(final RemoteDeviceId id, final RemoteDeviceHandler<NetconfSessionPreferences> salFacade,
                              final ScheduledExecutorService executor, final long keepaliveDelaySeconds, final long defaultRequestTimeoutMillis) {
        this.id = id;
        this.salFacade = salFacade;
        this.executor = executor;
        this.keepaliveDelaySeconds = keepaliveDelaySeconds;
        this.defaultRequestTimeoutMillis = defaultRequestTimeoutMillis;
        this.resetKeepaliveTask = new ResetKeepalive();
    }

    public KeepaliveSalFacade(final RemoteDeviceId id, final RemoteDeviceHandler<NetconfSessionPreferences> salFacade,
                              final ScheduledExecutorService executor) {
        this(id, salFacade, executor, DEFAULT_DELAY, DEFAULT_TRANSACTION_TIMEOUT_MILLI);
    }

    /**
     * Set the netconf session listener whenever ready
     *
     * @param listener netconf session listener
     */
    public void setListener(final NetconfDeviceCommunicator listener) {
        this.listener = listener;
    }

    /**
     * Just cancel current keepalive task.
     * If its already started, let it finish ... not such a big deal.
     *
     * Then schedule next keepalive.
     */
    private void resetKeepalive() {
        LOG.trace("{}: Resetting netconf keepalive timer", id);
        if(currentKeepalive != null) {
            currentKeepalive.cancel(false);
        }
        scheduleKeepalive();
    }

    /**
     * Cancel current keepalive and also reset current deviceRpc
     */
    private void stopKeepalives() {
        if(currentKeepalive != null) {
            currentKeepalive.cancel(false);
        }
        currentDeviceRpc = null;
    }

    private void reconnect() {
        Preconditions.checkState(listener != null, "%s: Unable to reconnect, session listener is missing", id);
        stopKeepalives();
        LOG.info("{}: Reconnecting inactive netconf session", id);
        listener.disconnect();
    }

    @Override
    public void onDeviceConnected(final SchemaContext remoteSchemaContext, final NetconfSessionPreferences netconfSessionPreferences, final DOMRpcService deviceRpc) {
        this.currentDeviceRpc = deviceRpc;
        final DOMRpcService deviceRpc1 = new KeepaliveDOMRpcService(deviceRpc, resetKeepaliveTask, defaultRequestTimeoutMillis, executor);
        salFacade.onDeviceConnected(remoteSchemaContext, netconfSessionPreferences, deviceRpc1);

        LOG.debug("{}: Netconf session initiated, starting keepalives", id);
        scheduleKeepalive();
    }

    private void scheduleKeepalive() {
        Preconditions.checkState(currentDeviceRpc != null);
        LOG.trace("{}: Scheduling next keepalive in {} {}", id, keepaliveDelaySeconds, TimeUnit.SECONDS);
        currentKeepalive = executor.schedule(new Keepalive(currentKeepalive), keepaliveDelaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void onDeviceDisconnected() {
        stopKeepalives();
        salFacade.onDeviceDisconnected();
    }

    @Override
    public void onDeviceFailed(final Throwable throwable) {
        stopKeepalives();
        salFacade.onDeviceFailed(throwable);
    }

    @Override
    public void onNotification(final DOMNotification domNotification) {
        resetKeepalive();
        salFacade.onNotification(domNotification);
    }

    @Override
    public void close() {
        stopKeepalives();
        salFacade.close();
    }

    // Keepalive RPC static resources
    private static final SchemaPath PATH = toPath(NETCONF_GET_CONFIG_QNAME);
    private static final ContainerNode KEEPALIVE_PAYLOAD =
            NetconfMessageTransformUtil.wrap(NETCONF_GET_CONFIG_QNAME, getSourceNode(NETCONF_RUNNING_QNAME), NetconfMessageTransformUtil.EMPTY_FILTER);

    /**
     * Invoke keepalive RPC and check the response. In case of any received response the keepalive
     * is considered successful and schedules next keepalive with a fixed delay. If the response is unsuccessful (no
     * response received, or the rcp could not even be sent) immediate reconnect is triggered as netconf session
     * is considered inactive/failed.
     */
    private class Keepalive implements Runnable, FutureCallback<DOMRpcResult> {

        private final ScheduledFuture<?> previousKeepalive;

        public Keepalive(final ScheduledFuture<?> previousKeepalive) {
            this.previousKeepalive = previousKeepalive;
        }

        @Override
        public void run() {
            LOG.trace("{}: Invoking keepalive RPC", id);

            try {
                if(previousKeepalive != null && !previousKeepalive.isDone()) {
                    onFailure(new IllegalStateException("Previous keepalive timed out"));
                } else {
                    Futures.addCallback(currentDeviceRpc.invokeRpc(PATH, KEEPALIVE_PAYLOAD), this);
                }
            } catch (NullPointerException e) {
                LOG.debug("{}: Skipping keepalive while reconnecting", id);
                // Empty catch block intentional
                // Do nothing. The currentDeviceRpc was null and it means we hit the reconnect window and
                // attempted to send keepalive while we were reconnecting. Next keepalive will be scheduled
                // after reconnect so no action necessary here.
            }
        }

        @Override
        public void onSuccess(final DOMRpcResult result) {
            if (result != null && result.getResult() != null) {
                LOG.debug("{}: Keepalive RPC successful with response: {}", id, result.getResult());
                scheduleKeepalive();
            } else {
                LOG.warn("{} Keepalive RPC returned null with response: {}. Reconnecting netconf session", id, result);
                reconnect();
            }
        }

        @Override
        public void onFailure(@Nonnull final Throwable t) {
            LOG.warn("{}: Keepalive RPC failed. Reconnecting netconf session.", id, t);
            reconnect();
        }
    }

    /**
     * Reset keepalive after each RPC response received
     */
    private class ResetKeepalive implements FutureCallback<DOMRpcResult> {
        @Override
        public void onSuccess(@Nullable final DOMRpcResult result) {
            // No matter what response we got, rpc-reply or rpc-error, we got it from device so the netconf session is OK
            resetKeepalive();
        }

        @Override
        public void onFailure(@Nonnull final Throwable t) {
            // User/Application RPC failed (The RPC did not reach the remote device or .. TODO what other reasons could cause this ?)
            // There is no point in keeping this session. Reconnect.
            LOG.warn("{}: Rpc failure detected. Reconnecting netconf session", id, t);
            reconnect();
        }
    }

    /*
     * Request timeout task is called once the defaultRequestTimeoutMillis is
     * reached. At this moment, if the request is not yet finished, we cancel
     * it.
     */
    private static final class RequestTimeoutTask implements Runnable {

        private final CheckedFuture<DOMRpcResult, DOMRpcException> rpcResultFuture;

        public RequestTimeoutTask(final CheckedFuture<DOMRpcResult, DOMRpcException> rpcResultFuture) {
            this.rpcResultFuture = rpcResultFuture;
        }

        @Override
        public void run() {
            if (!rpcResultFuture.isDone()) {
                rpcResultFuture.cancel(true);
            }
        }
    }

    /**
     * DOMRpcService proxy that attaches reset-keepalive-task and schedule
     * request-timeout-task to each RPC invocation.
     */
    private static final class KeepaliveDOMRpcService implements DOMRpcService {

        private final DOMRpcService deviceRpc;
        private ResetKeepalive resetKeepaliveTask;
        private final long defaultRequestTimeoutMillis;
        private final ScheduledExecutorService executor;

        public KeepaliveDOMRpcService(final DOMRpcService deviceRpc, final ResetKeepalive resetKeepaliveTask,
                final long defaultRequestTimeoutMillis, final ScheduledExecutorService executor) {
            this.deviceRpc = deviceRpc;
            this.resetKeepaliveTask = resetKeepaliveTask;
            this.defaultRequestTimeoutMillis = defaultRequestTimeoutMillis;
            this.executor = executor;
        }

        @Nonnull
        @Override
        public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final SchemaPath type, final NormalizedNode<?, ?> input) {
            final CheckedFuture<DOMRpcResult, DOMRpcException> domRpcResultDOMRpcExceptionCheckedFuture = deviceRpc.invokeRpc(type, input);
            Futures.addCallback(domRpcResultDOMRpcExceptionCheckedFuture, resetKeepaliveTask);

            final RequestTimeoutTask timeoutTask = new RequestTimeoutTask(domRpcResultDOMRpcExceptionCheckedFuture);
            executor.schedule(timeoutTask, defaultRequestTimeoutMillis, TimeUnit.MILLISECONDS);

            return domRpcResultDOMRpcExceptionCheckedFuture;
        }

        @Override
        public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull final T listener) {
            // There is no real communication with the device (yet), no reset here
            return deviceRpc.registerRpcListener(listener);
        }
    }
}
