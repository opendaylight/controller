/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.messages;

/**
 * Created by HanJie on 2017/2/17.
 *
 * @author Han Jie
 */
public class PostDeliverMessage<T,K> extends AbstractDeliverMessage<T,K> {
    private T replyTo;
    public PostDeliverMessage(T receiver,K message, T replyTo)
    {
        super(receiver, message);
        this.replyTo = replyTo;
    }

    public T getReplyTo() {
        return replyTo;
    }
}
