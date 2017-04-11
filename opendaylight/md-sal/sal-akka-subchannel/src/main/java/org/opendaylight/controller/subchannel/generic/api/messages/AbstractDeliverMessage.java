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
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public abstract class AbstractDeliverMessage<T,K>  implements Serializable {
    private T receiver;
    private K message;
    private MessageContext<T> messageContext;
    public AbstractDeliverMessage(T receiver, K message){
        this.receiver = receiver;
        this.message = message;
    }

    public T getReceiver()
    {
        return  receiver;
    }


    public K getMessage()
    {
        return  message;
    }

    public MessageContext<T> getMessageContext() {
        return messageContext;
    }

    public void setMessageContext(MessageContext<T> messageContext) {
        this.messageContext = messageContext;
    }
}
