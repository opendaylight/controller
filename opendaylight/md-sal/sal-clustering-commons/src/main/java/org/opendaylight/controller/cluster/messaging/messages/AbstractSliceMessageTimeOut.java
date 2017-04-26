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
 * Created by HanJie on 2017/2/10.
 *
 * @author Han Jie
 */
public abstract class AbstractSliceMessageTimeOut<T>
        implements Serializable {
    private static final long serialVersionUID = -1146411504021832870L;
    private final long messageId;
    private final int chunkIndex;
    private T clientId;

    public AbstractSliceMessageTimeOut(T clientId, long messageId,int chunkIndex) {
        this.messageId = messageId;
        this.clientId = clientId;
        this.chunkIndex = chunkIndex;
    }

    public long getMessageId() {
        return messageId;
    }

    public T getClientId() {
        return clientId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }
}
