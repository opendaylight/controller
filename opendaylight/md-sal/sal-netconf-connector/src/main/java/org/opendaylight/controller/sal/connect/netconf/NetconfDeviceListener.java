/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import com.google.common.base.Objects;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.NetconfMapping;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.w3c.dom.Document;

@SuppressWarnings("all")
class NetconfDeviceListener extends NetconfClientSessionListener {
    private final NetconfDevice device;
    private final EventExecutor eventExecutor;

    public NetconfDeviceListener(final NetconfDevice device, final EventExecutor eventExecutor) {
        this.device = device;
        this.eventExecutor = eventExecutor;
    }

    private Promise<NetconfMessage> messagePromise;
    private ConcurrentMap<String, Promise<NetconfMessage>> promisedMessages;

    private final ReentrantLock promiseLock = new ReentrantLock();

    public void onMessage(final NetconfClientSession session, final NetconfMessage message) {
        if (isNotification(message)) {
            this.onNotification(session, message);
        } else {
            try {
                this.promiseLock.lock();
                boolean _notEquals = (!Objects.equal(this.messagePromise, null));
                if (_notEquals) {
                    this.device.logger.debug("Setting promised reply {} with message {}", this.messagePromise, message);
                    this.messagePromise.setSuccess(message);
                    this.messagePromise = null;
                }
            } finally {
                this.promiseLock.unlock();
            }
        }
    }

    /**
     * Method intended to customize notification processing.
     * 
     * @param session
     *            {@see
     *            NetconfClientSessionListener#onMessage(NetconfClientSession,
     *            NetconfMessage)}
     * @param message
     *            {@see
     *            NetconfClientSessionListener#onMessage(NetconfClientSession,
     *            NetconfMessage)}
     */
    public void onNotification(final NetconfClientSession session, final NetconfMessage message) {
        this.device.logger.debug("Received NETCONF notification.", message);
        CompositeNode _notificationBody = null;
        CompositeNode _compositeNode = null;
        if (message != null) {
            _compositeNode = NetconfMapping.toCompositeNode(message,device.getSchemaContext());
        }
        if (_compositeNode != null) {
            _notificationBody = NetconfDeviceListener.getNotificationBody(_compositeNode);
        }
        final CompositeNode domNotification = _notificationBody;
        boolean _notEquals = (!Objects.equal(domNotification, null));
        if (_notEquals) {
            MountProvisionInstance _mountInstance = null;
            if (this.device != null) {
                _mountInstance = this.device.getMountInstance();
            }
            if (_mountInstance != null) {
                _mountInstance.publish(domNotification);
            }
        }
    }

    private static CompositeNode getNotificationBody(final CompositeNode node) {
        List<Node<? extends Object>> _children = node.getChildren();
        for (final Node<? extends Object> child : _children) {
            if ((child instanceof CompositeNode)) {
                return ((CompositeNode) child);
            }
        }
        return null;
    }

    public NetconfMessage getLastMessage(final int attempts, final int attemptMsDelay) throws InterruptedException {
        final Promise<NetconfMessage> promise = this.promiseReply();
        this.device.logger.debug("Waiting for reply {}", promise);
        int _plus = (attempts * attemptMsDelay);
        final boolean messageAvailable = promise.await(_plus);
        if (messageAvailable) {
            try {
                try {
                    return promise.get();
                } catch (Throwable _e) {
                    throw Exceptions.sneakyThrow(_e);
                }
            } catch (final Throwable _t) {
                if (_t instanceof ExecutionException) {
                    final ExecutionException e = (ExecutionException) _t;
                    IllegalStateException _illegalStateException = new IllegalStateException(e);
                    throw _illegalStateException;
                } else {
                    throw Exceptions.sneakyThrow(_t);
                }
            }
        }
        String _plus_1 = ("Unsuccessful after " + Integer.valueOf(attempts));
        String _plus_2 = (_plus_1 + " attempts.");
        IllegalStateException _illegalStateException_1 = new IllegalStateException(_plus_2);
        throw _illegalStateException_1;
    }

    public synchronized Promise<NetconfMessage> promiseReply() {
        this.device.logger.debug("Promising reply.");
        this.promiseLock.lock();
        try {
            boolean _equals = Objects.equal(this.messagePromise, null);
            if (_equals) {
                Promise<NetconfMessage> _newPromise = this.eventExecutor.<NetconfMessage> newPromise();
                this.messagePromise = _newPromise;
                return this.messagePromise;
            }
            return this.messagePromise;
        } finally {
            this.promiseLock.unlock();
        }
    }

    public boolean isNotification(final NetconfMessage message) {
        Document _document = message.getDocument();
        final XmlElement xmle = XmlElement.fromDomDocument(_document);
        String _name = xmle.getName();
        return XmlNetconfConstants.NOTIFICATION_ELEMENT_NAME.equals(_name);
    }
}
