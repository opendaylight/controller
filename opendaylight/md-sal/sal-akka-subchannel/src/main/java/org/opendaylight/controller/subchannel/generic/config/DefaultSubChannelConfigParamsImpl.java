/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.config;

/**
 * Created by HanJie on 2017/3/22.
 *
 * @author Han Jie
 */
public class DefaultSubChannelConfigParamsImpl implements SubChannelConfigParams {

    public static final int DEFAULT_CHUNK_SIZE = 2048 * 1000; //2MB
    public static final long DEFAULT_MESSAGE_TIMEOUT = 10; //seconds
    public static final long DEFAULT_SERIALIZER_IDLE_TIMEOUT = 10; //seconds


    private long messageTimeoutInSeconds = DEFAULT_MESSAGE_TIMEOUT;//30 seconds
    private long serializerTimeoutInSeconds = DEFAULT_SERIALIZER_IDLE_TIMEOUT;//10 seconds
    private int chunkSize = DEFAULT_CHUNK_SIZE;

    @Override
    public int getChunkSize() {
        return chunkSize;
    }


    @Override
    public long getMessageTimeoutInSeconds() {
        return messageTimeoutInSeconds;
    }



    @Override
    public long getSerializerTimeoutInSeconds() {
        return serializerTimeoutInSeconds;
    }




    @Override
    public SubChannelConfigParams setMessageTimeoutInSeconds(long messageTimeoutInSeconds) {
        this.messageTimeoutInSeconds = messageTimeoutInSeconds;
        return this;
    }

    @Override
    public SubChannelConfigParams setSerializerTimeoutInSeconds(long serializerTimeoutInSeconds) {
        this.serializerTimeoutInSeconds = serializerTimeoutInSeconds;
        return this;
    }

    @Override
    public SubChannelConfigParams setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }
}
