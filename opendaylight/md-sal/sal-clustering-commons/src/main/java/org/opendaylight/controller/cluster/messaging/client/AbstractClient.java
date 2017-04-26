/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging.client;

import javax.annotation.Nullable;

import org.opendaylight.controller.cluster.messaging.AbstractSliceMessage;
import org.opendaylight.controller.cluster.messaging.messages.AbstractSliceMessageTimeOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by HanJie on 2017/1/23.
 *
 * @author Han Jie
 */
public abstract class AbstractClient<T,
        M extends AbstractSliceMessage<? extends AbstractSliceMessageTimeOut<T>>> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClient.class);
    private M currentMessage;
    private T clientId;

    AbstractClient(T entity) {
        this.clientId = entity;
    }

    public T getClientId() {
        return clientId;
    }

    public M getCurrentMessage() {
        return currentMessage;
    }

    public void setCurrentMessage(@Nullable  M currentMessage) {
        LOG.debug("setCurrentMessage {}",currentMessage);
        this.currentMessage = currentMessage;
    }

    public void done() {
        if (getCurrentMessage() != null) {
            LOG.debug("done message {}", getCurrentMessage().getMessageId());
            getCurrentMessage().scheduleOnceCancel();
            setCurrentMessage(null);
        }
    }
}
