/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.messages;

import akka.util.Timeout;

/**
 * Created by HanJie on 2017/2/17.
 *
 * @author Han Jie
 */
public class RequestDeliverMessage<T,K> extends AbstractDeliverMessage<T,K> {
    private Timeout timeout;
    public RequestDeliverMessage(T receiver, K message, Timeout timeout)
    {
        super(receiver,message);
        this.timeout = timeout;
    }

    public Timeout getTimeout() {
        return timeout;
    }
}
