/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging.messages;

import java.io.Serializable;

/**
 * Created by HanJie on 2017/1/23.
 *
 * @author Han Jie
 */
public class ChunkMessageReply implements Serializable {
    private static final long serialVersionUID = -4852895181480662288L;
    private long  messageId;
    private final int  chunkIndex;
    private final boolean success;

    public ChunkMessageReply(long  messageId, int  chunkIndex, boolean success) {
        this.chunkIndex = chunkIndex;
        this.messageId = messageId;
        this.success = success;

    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public long getMessageId() {
        return messageId;
    }

    public boolean isSuccess() {
        return success;
    }
}
