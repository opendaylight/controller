/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import akka.actor.ActorContext;
import akka.actor.Cancellable;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.messaging.messages.AbstractSliceMessageTimeOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Created by HanJie on 2017/2/10.
 *
 * @author Han Jie
 */
public abstract class AbstractSliceMessage<K extends AbstractSliceMessageTimeOut<?>> {
    private static final Logger LOG = LoggerFactory.getLogger(SliceMessage.class);
    private long messageId;
    private final ActorContext actorContext;
    static final int INVALID_CHUNK_INDEX = -1;
    static final int FIRST_CHUNK_INDEX = 1;
    static final int INITIAL_LAST_CHUNK_HASH_CODE = -1;
    private int lastChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;
    protected int totalChunks;
    private Cancellable timer;
    private final FiniteDuration duration;



    public AbstractSliceMessage(ActorContext actorContext,long messageId,
                                long timeoutInSenconds) {
        this.actorContext = actorContext;
        this.messageId = messageId;
        this.duration = new FiniteDuration(timeoutInSenconds, TimeUnit.SECONDS);
    }

    public long getMessageId() {
        return messageId;
    }

    public int getLastChunkHashCode() {
        return lastChunkHashCode;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public void setLastChunkHashCode(int lastChunkHashCode) {
        this.lastChunkHashCode = lastChunkHashCode;
    }

    private FiniteDuration getDuration() {
        return duration;
    }

    protected Cancellable newTimer(FiniteDuration timeout, Object message) {
        LOG.debug("newTimer timeout {} message {}", timeout, message);
        return actorContext.system().scheduler().scheduleOnce(
                timeout, actorContext.self(), message,
                actorContext.system().dispatcher(), actorContext.self());
    }

    private void setTimer(Cancellable timer) {
        LOG.debug("setTimer timer {}", timer);
        this.timer = timer;
    }

    private Cancellable getTimer() {
        return this.timer;
    }


    public void scheduleOnceTimeOut(K timeoutMessage) {
        LOG.debug("scheduleOnceTimeOut timeoutMessage {}", timeoutMessage);
        scheduleOnceCancel();
        setTimer(newTimer(getDuration(),timeoutMessage));
    }

    public void scheduleOnceCancel() {
        LOG.debug("scheduleOnceCancel ");
        Cancellable timer = getTimer();
        if (timer != null) {
            if (!timer.isCancelled()) {
                timer.cancel();
            }
            setTimer(null);
        }
    }

}
