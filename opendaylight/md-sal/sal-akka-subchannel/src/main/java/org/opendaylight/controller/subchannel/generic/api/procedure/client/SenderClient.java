/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.procedure.client;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.subchannel.generic.api.data.SliceMessageTracker;
import org.opendaylight.controller.subchannel.generic.api.messages.AbstractSliceMessageTimeOut;
import org.opendaylight.controller.subchannel.generic.api.messages.FinishSliceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class SenderClient<T,C> extends AbstractClient<T> {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private Map<Long,SliceMessageTracker<T,C>> queuedSliceMessageTracker = new HashMap<>();

    public SenderClient(T sender){
        super(sender);
    }

    public void enqueueSliceMessageTracker(SliceMessageTracker<T,C> message){
        queuedSliceMessageTracker.put(message.getMessageId(),message);
    }

    private SliceMessageTracker<T,C> dequeueSliceMessageTracker(long id){
        LOG.debug("dequeueSliceMessageTracker id {} ", id);
        return queuedSliceMessageTracker.remove(id);
    }

    public SliceMessageTracker<T,C> getSliceMessageTracker(Long id){

        return queuedSliceMessageTracker.get(id);
    }

    public byte[] finish(FinishSliceMessage<T> finishMessage) {

        SliceMessageTracker<T,C> sliceMessageTracker = dequeueSliceMessageTracker(finishMessage.getMessageId());
        sliceMessageTracker.scheduleOnceCancel();
        return sliceMessageTracker.finish();
    }

    public void handleSliceMessageTimeOut(AbstractSliceMessageTimeOut<T> message){
        dequeueSliceMessageTracker(message.getMessageId());
    }

}
