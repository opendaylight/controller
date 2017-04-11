/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.messages;

import java.io.Serializable;

import org.opendaylight.controller.subchannel.generic.api.data.MessageContext;

/**
 * Created by HanJie on 2017/2/20.
 *
 * @author Han Jie
 */
public class SerializeMessageReply<T> implements Serializable{
    private byte[] bytes;
    private MessageContext<T> messageContext;

    public SerializeMessageReply(MessageContext<T> messageContext, byte[] bytes) {
        this.bytes = bytes;
        this.messageContext = messageContext;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public MessageContext<T> getMessageContext() {
        return messageContext;
    }
}
