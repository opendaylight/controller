/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.procedure.client;

import java.io.Serializable;

/**
 * Created by HanJie on 2017/1/23.
 *
 * @author Han Jie
 */
public class ClientIdentify<T> implements Serializable{
    private T entity;

    public ClientIdentify(T entity) {
        this.entity = entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientIdentify<?> that = (ClientIdentify<?>) o;

        return entity.equals(that.entity);

    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }
}
