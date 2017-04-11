/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.messages;

import java.io.Serializable;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class FinishSliceMessage<T> implements Serializable {

    private long messageId;
    private T sender;
    private T receiver;
    public FinishSliceMessage(long messageId,T receiver,T sender){
        this.messageId = messageId;
        this.sender = sender;
        this.receiver = receiver;
    }

    public T getReceiver() {
        return this.receiver;
    }
    public T getSender() {
        return this.sender;
    }

    public long getMessageId() {
        return this.messageId;
    }
}
