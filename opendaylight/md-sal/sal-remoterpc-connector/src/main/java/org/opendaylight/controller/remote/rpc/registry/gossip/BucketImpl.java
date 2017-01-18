/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.io.Serializable;

final class BucketImpl<T extends BucketData<T>> implements Bucket<T>, Serializable {
    private static final long serialVersionUID = 1L;

    private final long version;

    // Guaranteed to be non-null
    private final T data;

    BucketImpl(final long version, final T data) {
        this.version = version;
        this.data = Preconditions.checkNotNull(data);
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "BucketImpl{" + "version=" + version + ", data=" + data + '}';
    }

    private Object readResolve() {
        Verify.verifyNotNull(data);
        return this;
    }
}
