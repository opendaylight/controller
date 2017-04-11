/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.spi.data;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.subchannel.generic.api.data.SliceMessageTimer;
import org.opendaylight.controller.subchannel.generic.api.messages.AbstractSliceMessageTimeOut;
import org.opendaylight.controller.subchannel.generic.api.procedure.ProcedureCallback;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.AbstractClient;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.ClientIdentify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Created by HanJie on 2017/2/10.
 *
 * @author Han Jie
 */
public abstract class AbstractSliceMessageTimer<T,C> implements SliceMessageTimer<C> ,Serializable{
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private AbstractSliceMessageTimeOut<T> timeOutMessage;
    private C timer;
    private ProcedureCallback<T,C> callback;
    private final FiniteDuration duration;

    public AbstractSliceMessageTimer(long messageId, AbstractClient<T> client, long timeoutInSenconds,
                                     ProcedureCallback<T,C> callback){
        this.timeOutMessage = newSliceMessageTimeOut(client.getClientIdentify(),messageId);
        setCallback(callback);
        this.duration = new FiniteDuration(timeoutInSenconds, TimeUnit.SECONDS);
    }

    protected abstract AbstractSliceMessageTimeOut<T> newSliceMessageTimeOut(ClientIdentify<T> id, long messageId);

    private C getTimer() {
        return timer;
    }


    private void setTimer(C timer) {
        this.timer = timer;
    }


    private AbstractSliceMessageTimeOut getTimeOutMessage() {
        return timeOutMessage;
    }

    private FiniteDuration getDuration() {
        return duration;
    }


    private ProcedureCallback<T, C> getCallback() {
        return callback;
    }

    private void setCallback(ProcedureCallback<T, C> callback) {
        this.callback = callback;
    }


    @Override
    public void scheduleOnceTimeOut(Object lastMessage){
        LOG.debug("scheduleOnceTimeOut lastMessage {}", lastMessage);
        Preconditions.checkNotNull(getCallback());
        scheduleOnceCancel();

        getTimeOutMessage().setLastMessage(lastMessage);
        setTimer(getCallback().newTimerCall(getDuration(),
                getTimeOutMessage().setLastMessage(lastMessage)));
    }

    @Override
    public void scheduleOnceCancel(){
        LOG.debug("scheduleOnceCancel ");
        Preconditions.checkNotNull(getCallback());
        C timer = getTimer();
        if(timer != null){
            getCallback().stopTimerCall(timer);
            setTimer(null);
        }
    }

}
