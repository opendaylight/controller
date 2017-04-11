/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.spi.data;

import java.io.Serializable;

import org.opendaylight.controller.subchannel.generic.api.procedure.ProcedureCallback;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.AbstractClient;

/**
 * Created by HanJie on 2017/2/10.
 *
 * @author Han Jie
 */
public abstract class AbstractSliceMessage<T,C> extends AbstractSliceMessageTimer<T,C> implements Serializable{
    private long messageId;
    private T receiver;
    private T sender;
    static final int INVALID_CHUNK_INDEX = -1;

    public AbstractSliceMessage(long messageId, T receiver, T sender,AbstractClient<T> client,
                                long timeoutInSenconds,ProcedureCallback<T,C> callback){
        super(messageId,client,timeoutInSenconds,callback);
        this.messageId = messageId;
        this.receiver = receiver;
        this.sender = sender;
    }

    public long getMessageId() {
        return messageId;
    }

    public T getReceiver() {
        return this.receiver;
    }

    public T getSender() {
        return this.sender;
    }
}
