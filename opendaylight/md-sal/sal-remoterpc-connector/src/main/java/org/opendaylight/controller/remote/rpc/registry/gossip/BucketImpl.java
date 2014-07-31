/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

public class BucketImpl<T extends Copier<T>> implements Bucket<T> {

    private Long version = System.currentTimeMillis()+1;;

    private T data;

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public T getData() {
        return data;
    }

    public void setData(T data){
        this.version = System.currentTimeMillis()+1;
        this.data = data.copy();
    }

    BucketImpl<T> copy(){
        BucketImpl<T> copy = new BucketImpl<>();
        copy.version = this.version;
        copy.data    = this.data.copy();

        return copy;
    }
}
