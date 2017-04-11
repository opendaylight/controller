/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.data;

import java.io.Serializable;

/**
 * Created by HanJie on 2017/2/21.
 *
 * @author Han Jie
 */
public class MessageContext<T> implements Serializable{
    private T receiver;
    private T sender;
    private T localProxy;
    private T remoteProxy;
    private Class<?> clazz;

    public MessageContext(T receiver, T remoteProxy, T sender, T localProxy,Class<?> clazz) {
        this.receiver = receiver;
        this.sender = sender;
        this.localProxy = localProxy;
        this.remoteProxy = remoteProxy;
        this.clazz = clazz;
    }

    public T getReceiver() {
        return receiver;
    }

    public T getSender() {
        return sender;
    }

    public T getLocalProxy() {
        return localProxy;
    }

    public T getRemoteProxy() {
        return remoteProxy;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
