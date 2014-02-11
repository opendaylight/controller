/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

class NetconfDeviceListener implements NetconfClientSessionListener {
    private static final class Request {
        final UncancellableFuture<RpcResult<CompositeNode>> future;
        final NetconfMessage request;

        private Request(UncancellableFuture<RpcResult<CompositeNode>> future, NetconfMessage request) {
            this.future = future;
            this.request = request;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceListener.class);
    private final Queue<Request> requests = new ArrayDeque<>();
    private final NetconfDevice device;
    private NetconfClientSession session;

    public NetconfDeviceListener(final NetconfDevice device) {
        this.device = Preconditions.checkNotNull(device);
    }

    @Override
    public synchronized void onSessionUp(final NetconfClientSession session) {
        LOG.debug("Session with {} established as address {} session-id {}",
                device.getName(), device.getSocketAddress(), session.getSessionId());

        final Set<QName> caps = device.getCapabilities(session.getServerCapabilities());
        LOG.trace("Server {} advertized capabilities {}", device.getName(), caps);

        // Select the appropriate provider
        final SchemaSourceProvider<String> delegate;
        if (NetconfRemoteSchemaSourceProvider.isSupportedFor(caps)) {
            delegate = new NetconfRemoteSchemaSourceProvider(device);
        } else if(caps.contains(NetconfRemoteSchemaSourceProvider.IETF_NETCONF_MONITORING.getNamespace().toString())) {
            delegate = new NetconfRemoteSchemaSourceProvider(device);
        } else {
            LOG.info("Netconf server {} does not support IETF Netconf Monitoring", device.getName());
            delegate = SchemaSourceProviders.<String>noopProvider();
        }

        device.bringUp(delegate, caps);

        this.session = session;
    }

    private synchronized void tearDown(final Exception e) {
        session = null;

        /*
         * Walk all requests, check if they have been executing
         * or cancelled and remove them from the queue.
         */
        final Iterator<Request> it = requests.iterator();
        while (it.hasNext()) {
            final Request r = it.next();
            if (r.future.isUncancellable()) {
                // FIXME: add a RpcResult instead?
                r.future.setException(e);
                it.remove();
            } else if (r.future.isCancelled()) {
                // This just does some house-cleaning
                it.remove();
            }
        }

        device.bringDown();
    }

    @Override
    public void onSessionDown(final NetconfClientSession session, final Exception e) {
        LOG.debug("Session with {} went down", device.getName(), e);
        tearDown(e);
    }

    @Override
    public void onSessionTerminated(final NetconfClientSession session, final NetconfTerminationReason reason) {
        LOG.debug("Session with {} terminated {}", session, reason);
        tearDown(new RuntimeException(reason.getErrorMessage()));
    }

    @Override
    public void onMessage(final NetconfClientSession session, final NetconfMessage message) {
        /*
         * Dispatch between notifications and messages. Messages need to be processed
         * with lock held, notifications do not.
         */
        if (isNotification(message)) {
            processNotification(message);
        } else {
            processMessage(message);
        }
    }

    private synchronized void processMessage(final NetconfMessage message) {
        final Request r = requests.peek();
        if (r.future.isUncancellable()) {
            requests.poll();
            LOG.debug("Matched {} to {}", r.request, message);

            // FIXME: this can throw exceptions, which should result
            // in the future failing
            NetconfMapping.checkValidReply(r.request, message);
            r.future.set(Rpcs.getRpcResult(true, NetconfMapping.toNotificationNode(message, device.getSchemaContext()),
                    Collections.<RpcError>emptyList()));
        } else {
            LOG.warn("Ignoring unsolicited message", message);
        }
    }

    synchronized ListenableFuture<RpcResult<CompositeNode>> sendRequest(final NetconfMessage message) {
        if (session == null) {
            LOG.debug("Session to {} is disconnected, failing RPC request {}", device.getName(), message);
            return Futures.<RpcResult<CompositeNode>>immediateFuture(new RpcResult<CompositeNode>() {
                @Override
                public boolean isSuccessful() {
                    return false;
                }

                @Override
                public CompositeNode getResult() {
                    return null;
                }

                @Override
                public Collection<RpcError> getErrors() {
                    // FIXME: indicate that the session is down
                    return Collections.emptySet();
                }
            });
        }

        final Request req = new Request(new UncancellableFuture<RpcResult<CompositeNode>>(true), message);
        requests.add(req);

        session.sendMessage(req.request).addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(final Future<Void> future) throws Exception {
                if (!future.isSuccess()) {
                    // We expect that a session down will occur at this point
                    LOG.debug("Failed to send request {}", req.request, future.cause());
                    req.future.setException(future.cause());
                } else {
                    LOG.trace("Finished sending request {}", req.request);
                }
            }
        });

        return req.future;
    }

    /**
     * Process an incoming notification.
     *
     * @param notification Notification message
     */
    private void processNotification(final NetconfMessage notification) {
        this.device.logger.debug("Received NETCONF notification.", notification);
        CompositeNode domNotification = NetconfMapping.toNotificationNode(notification, device.getSchemaContext());
        if (domNotification == null) {
            return;
        }

        MountProvisionInstance mountInstance =  this.device.getMountInstance();
        if (mountInstance != null) {
            mountInstance.publish(domNotification);
        }
    }

    private static boolean isNotification(final NetconfMessage message) {
        final XmlElement xmle = XmlElement.fromDomDocument(message.getDocument());
        return XmlNetconfConstants.NOTIFICATION_ELEMENT_NAME.equals(xmle.getName()) ;
    }
}
