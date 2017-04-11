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
 * Created by HanJie on 2017/1/23.
 *
 * @author Han Jie
 */
public class ChunkMessageReply implements Serializable {
    private long  messageId;
    private int  chunkIndex;
    private int  totalChunks;

    public ChunkMessageReply(long  messageId,int  chunkIndex,int  totalChunks){
        this.chunkIndex = chunkIndex;
        this.messageId = messageId;
        this.totalChunks = totalChunks;

    }

    public int getChunkIndex() {
        return chunkIndex;
    }
    public long getMessageId() {
        return messageId;
    }

    public int getTotalChunks() {
        return totalChunks;
    }
}
