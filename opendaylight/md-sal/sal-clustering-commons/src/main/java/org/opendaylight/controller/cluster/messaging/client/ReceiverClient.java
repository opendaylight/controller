/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging.client;

import java.util.ArrayDeque;
import java.util.Queue;

import org.opendaylight.controller.cluster.messaging.SliceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/1/23.
 *
 * @author Han Jie
 */
public class ReceiverClient<T> extends AbstractClient<T, SliceMessage<T>> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClient.class);
    private final Queue<SliceMessage<T>> queuedMessages;

    public ReceiverClient(T receiver) {
        super(receiver);
        this.queuedMessages = new ArrayDeque<>();
    }

    public void enqueueMessage(SliceMessage<T> message) {
        LOG.debug("enqueueMessage message {} ", message);
        queuedMessages.add(message);
    }

    public boolean isQueueEmpty() {
        return queuedMessages.isEmpty();
    }

    public SliceMessage<T> pollMessage() {
        LOG.debug("pollMessage");
        SliceMessage<T> currentMessage = queuedMessages.poll();
        setCurrentMessage(currentMessage);
        return currentMessage;
    }
}
