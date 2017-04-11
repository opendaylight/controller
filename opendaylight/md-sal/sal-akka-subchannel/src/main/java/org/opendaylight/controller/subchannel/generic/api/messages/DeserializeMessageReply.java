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
 * Created by HanJie on 2017/2/20.
 *
 * @author Han Jie
 */
public class DeserializeMessageReply<T> implements Serializable{
    private Object message;
    private T remoteProxy;
    public DeserializeMessageReply(T remoteProxy,Object message){
        this.message = message;
        this.remoteProxy = remoteProxy;
    }

    public Object getMessage() {
        return message;
    }

    public T getRemoteProxy() {
        return remoteProxy;
    }
}
