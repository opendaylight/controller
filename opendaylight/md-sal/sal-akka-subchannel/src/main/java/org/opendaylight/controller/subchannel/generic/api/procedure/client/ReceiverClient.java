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

import org.opendaylight.controller.subchannel.generic.api.data.SliceMessage;
import org.opendaylight.controller.subchannel.generic.api.data.SliceMessageFactory;
import org.opendaylight.controller.subchannel.generic.api.exception.PostTimeoutException;
import org.opendaylight.controller.subchannel.generic.api.messages.AbstractSliceMessageTimeOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/1/23.
 *
 * @author Han Jie
 */
public class ReceiverClient<T,C> extends AbstractClient<T> {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private Map<Long,SliceMessage<T,C>> SendingSliceMessages = new HashMap<>();

    private SliceMessageFactory<T,C> sliceMessageFactory;

    public ReceiverClient(T receiver)
    {
        super(receiver);
        this.sliceMessageFactory = new SliceMessageFactory<T,C>();
    }

    public SliceMessage<T,C> dequeueSliceMessage(long messageId){
        LOG.debug("dequeueSliceMessage {} ", messageId);
        return SendingSliceMessages.remove(messageId);
    }

    public SliceMessage<T,C> enqueueSliceMessage(SliceMessage<T,C> sliceMessage){
        SendingSliceMessages.put(sliceMessage.getMessageId(),sliceMessage);
        return sliceMessage;
    }




    public void finish(long messageId) {
        SliceMessage<T,C>  sliceMessage = dequeueSliceMessage(messageId);
        sliceMessage.scheduleOnceCancel();
        sliceMessage.finish();
    }

    public long createSliceMessageId(){
        return this.sliceMessageFactory.getAndIncrementSliceMessageId();
    }

    public SliceMessage<T,C> getSliceMessage(Long id){

        return SendingSliceMessages.get(id);
    }

    public boolean isSending(){
        return SendingSliceMessages.isEmpty()?false:true;
    }

    @Override
    public void handleSliceMessageTimeOut(AbstractSliceMessageTimeOut<T> message){

        SliceMessage<T,C>  sliceMessage = dequeueSliceMessage(message.getMessageId());
        sliceMessage.finish(new PostTimeoutException("PostTimeoutException"));
    }
}
