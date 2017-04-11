/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.procedure.client;

import java.io.Serializable;

import org.opendaylight.controller.subchannel.generic.api.messages.AbstractSliceMessageTimeOut;

/**
 * Created by HanJie on 2017/1/23.
 *
 * @author Han Jie
 */
public abstract class AbstractClient<T> implements Serializable{
    private ClientIdentify<T> clientIdentify;

    AbstractClient(T entity){
        this.clientIdentify = new ClientIdentify<>(entity);
    }

    public ClientIdentify<T> getClientIdentify()
    {
        return clientIdentify;
    }


    abstract public void handleSliceMessageTimeOut(AbstractSliceMessageTimeOut<T> message);
}
