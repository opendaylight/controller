/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.sal.core.api.notify.NotificationService;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetconfClientSessionListener implements
        SessionListener<NetconfMessage, NetconfClientSession, NetconfTerminationReason> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClientSessionListener.class);
    private AtomicBoolean up = new AtomicBoolean(false);

    @Override
    public void onSessionUp(NetconfClientSession clientSession) {
        up.set(true);
    }

    @Override
    public void onSessionDown(NetconfClientSession clientSession, Exception e) {
        logger.debug("Client Session {} down, reason: {}", clientSession, e.getMessage());
        up.set(false);
    }

    @Override
    public void onSessionTerminated(NetconfClientSession clientSession,
            NetconfTerminationReason netconfTerminationReason) {
        logger.debug("Client Session {} terminated, reason: {}", clientSession,
                netconfTerminationReason.getErrorMessage());
        up.set(false);
    }

    @Override
    public synchronized void onMessage(NetconfClientSession session, NetconfMessage message) {
        if (isNotification(message)) {
            NotificationService ns ;
            // TODO: Publish to SAL.
        } else {
            synchronized (messages) {
                this.messages.add(message);
            }
        }
    }

    private static final String NOTIFICATION_ELEMENT_NAME = "notification";
    private boolean isNotification(NetconfMessage message) {
        Document doc = message.getDocument();
        String nodeName = doc.getDocumentElement().getNodeName();
        return nodeName.equals(NOTIFICATION_ELEMENT_NAME);
    }

    private int lastReadMessage = -1;
    private List<NetconfMessage> messages = Lists.newArrayList();

    public NetconfMessage getLastMessage(int attempts, int attemptMsDelay) throws InterruptedException {
        Preconditions.checkState(up.get(), "Session was not up yet");

        for (int i = 0; i < attempts; i++) {
            synchronized (messages) {
                if (messages.size() - 1 > lastReadMessage) {
                    lastReadMessage++;
                    return messages.get(lastReadMessage);
                }
            }

            if (up.get() == false)
                throw new IllegalStateException("Session ended while trying to read message");
            Thread.sleep(attemptMsDelay);
        }

        throw new IllegalStateException("No netconf message to read");
    }
}
