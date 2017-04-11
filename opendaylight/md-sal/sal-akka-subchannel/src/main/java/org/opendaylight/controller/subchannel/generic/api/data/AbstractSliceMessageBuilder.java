/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.data;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.subchannel.generic.api.procedure.ProcedureCallback;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.AbstractClient;

/**
 * Created by HanJie on 2017/2/10.
 *
 * @author Han Jie
 */
public abstract class AbstractSliceMessageBuilder<T,C,B extends AbstractSliceMessageBuilder<T,C,B>> {
    private long messageId;
    private T receiver;
    private T sender;
    private ProcedureCallback<T, C> callback;
    private AbstractClient<T> client;
    private long timeoutInSenconds;

    public long getMessageId() {
        return messageId;
    }

    @SuppressWarnings("unchecked")
    private B self(){
        return (B) this;
    }

    public B setMessageId(long messageId) {
        this.messageId = messageId;
        return self();
    }

    public T getReceiver() {
        return receiver;
    }

    public B setReceiver(T receiver) {
        this.receiver = receiver;
        return self();
    }

    public T getSender() {
        return sender;
    }

    public B setSender(T sender) {
        this.sender = sender;
        return self();
    }

    public ProcedureCallback<T, C> getCallback() {
        return callback;
    }

    public B setCallback(ProcedureCallback<T, C> callback) {
        this.callback = callback;
        return self();
    }

    public AbstractClient<T> getClient() {
        return client;
    }

    public B setClient(AbstractClient<T> client) {
        this.client = client;
        return self();
    }

    public long getTimeoutInSenconds() {
        return timeoutInSenconds;
    }

    public B setTimeoutInSenconds(long timeoutInSenconds) {
        this.timeoutInSenconds = timeoutInSenconds;
        return self();
    }

    protected void verify() {
        Preconditions.checkNotNull(messageId);
        Preconditions.checkNotNull(receiver);
        Preconditions.checkNotNull(sender);
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(client);
        Preconditions.checkNotNull(timeoutInSenconds);
    }
}
