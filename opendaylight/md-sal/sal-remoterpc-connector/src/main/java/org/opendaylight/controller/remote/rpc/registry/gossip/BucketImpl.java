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
    private static final long serialVersionUID = 294779770032719196L;

    private Long version = System.currentTimeMillis();

    private T data;

    public BucketImpl() {
    }

    public BucketImpl(T data) {
        this.data = data;
    }

    public BucketImpl(Bucket<T> other) {
        this.version = other.getVersion();
        this.data = other.getData();
    }

    public void setData(T data) {
        this.data = data;
        this.version = System.currentTimeMillis()+1;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "BucketImpl{" +
                "version=" + version +
                ", data=" + data +
                '}';
    }
}
