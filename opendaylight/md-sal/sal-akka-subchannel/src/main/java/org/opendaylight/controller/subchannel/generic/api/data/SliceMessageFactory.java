/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.data;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class SliceMessageFactory<T,C> {

    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<SliceMessageFactory> MESSAGE_COUNTER_UPDATER =
            AtomicLongFieldUpdater.newUpdater(SliceMessageFactory.class, "messageId");

    // Used via MESSAGE_COUNTER_UPDATER
    @SuppressWarnings("unused")
    private volatile long messageId;
    public static final int DEFAULT_CHUNK_SIZE = 1024;
    private int chunkSize = DEFAULT_CHUNK_SIZE;


    public SliceMessageFactory(int chunkSize)
    {
        if(chunkSize > 0)
        {
            this.chunkSize = chunkSize;
        }
    }

    public SliceMessageFactory()
    {
        this(DEFAULT_CHUNK_SIZE);
    }


    public long getAndIncrementSliceMessageId(){
        return  MESSAGE_COUNTER_UPDATER.getAndIncrement(this);
    }

}
