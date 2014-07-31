/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import java.io.Serializable;

public class BucketImpl<T extends Copier<T>> implements Bucket<T>, Serializable {

    private Long version = System.currentTimeMillis();;

    private T data;

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public T getData() {
        if (this.data == null)
            return null;

        return data.copy();
    }

    public void setData(T data){
        this.version = System.currentTimeMillis()+1;
        this.data = data;
    }

    @Override
    public String toString() {
        return "BucketImpl{" +
                "version=" + version +
                ", data=" + data +
                '}';
    }
}
