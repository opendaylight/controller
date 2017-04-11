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
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class FinishSliceMessageReply<T> implements Serializable {

    long messageId;

    public FinishSliceMessageReply(long messageId) {
        this.messageId = messageId;
    }

    public long getMessageId() {
        return messageId;
    }


}
