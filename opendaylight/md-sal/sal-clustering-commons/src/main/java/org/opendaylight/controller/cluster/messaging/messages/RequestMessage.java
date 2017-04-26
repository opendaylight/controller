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
 * Created by HanJie on 2017/4/18.
 *
 * @author Han Jie
 */
public class RequestMessage<T,K> implements Serializable {
    private static final long serialVersionUID = -5019977718692463601L;
    private T receiver;
    private K message;

    public RequestMessage(T receiver, K message) {
        this.receiver = receiver;
        this.message = message;
    }

    public T getReceiver() {
        return receiver;
    }

    public K getMessage() {
        return message;
    }

}

