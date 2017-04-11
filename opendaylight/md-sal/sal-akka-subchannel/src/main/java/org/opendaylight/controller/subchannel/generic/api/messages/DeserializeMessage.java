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
public class DeserializeMessage<T> implements Serializable {
    private byte[] bytes;
    private T remoteProxy;
    public DeserializeMessage(T remoteProxy,byte[] bytes){
        this.bytes = bytes;
        this.remoteProxy = remoteProxy;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public T getRemoteProxy() {
        return remoteProxy;
    }
}
