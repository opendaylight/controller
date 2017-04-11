/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.messages;

import java.io.Serializable;

import org.opendaylight.controller.subchannel.generic.api.procedure.client.ClientIdentify;

/**
 * Created by HanJie on 2017/2/10.
 *
 * @author Han Jie
 */
public abstract class AbstractSliceMessageTimeOut<T> implements Serializable {
    private final long messageId;
    private ClientIdentify<T> clientId;
    private Object lastMessage;
    public AbstractSliceMessageTimeOut(ClientIdentify<T> clientId, long messageId){
        this.messageId = messageId;
        this.clientId = clientId;
    }

    public AbstractSliceMessageTimeOut<T> setLastMessage(Object lastMessage) {
        this.lastMessage = lastMessage;
        return this;
    }

    public long getMessageId() {
        return messageId;
    }

    public ClientIdentify<T> getClientIdentify() {
        return clientId;
    }
}
