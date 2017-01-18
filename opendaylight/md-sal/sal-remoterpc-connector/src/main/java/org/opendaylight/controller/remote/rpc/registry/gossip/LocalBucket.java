/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import com.google.common.base.Preconditions;

/**
 * Local bucket implementation. Unlike a full-blown {@link Bucket}, this class is mutable and tracks when it has been
 * changed and when it has been sent anywhere.
 *
 * @author Robert Varga
 */
final class LocalBucket<T extends BucketData<T>> {
    /*
     * Decomposed 64bit signed version number. Always non-negative, hence the most significant bit is always zero.
     * - incarnation number (most-significant 31 bits, forming an unsigned int)
     * - version number (least-significant 32 bits, treated as unsigned int)
     *
     * We are keeping a boxed version here, as we stick it into a map anyway.
     */
    private long version;
    private T data;

    // We bump versions only if we took a snapshot since last data update
    private boolean bumpVersion;

    LocalBucket(final int incarnation, final T data) {
        Preconditions.checkArgument(incarnation >= 0);
        this.version = ((long)incarnation) << Integer.SIZE;
        this.data = Preconditions.checkNotNull(data);
    }

    T getData() {
        return data;
    }

    long getVersion() {
        return version;
    }

    Bucket<T> snapshot() {
        bumpVersion = true;
        return new BucketImpl<>(version, data);
    }

    boolean setData(final T data) {
        this.data = Preconditions.checkNotNull(data);
        if (!bumpVersion) {
            return false;
        }

        bumpVersion = false;
        return (++version & 0xffff_ffffL) == 0;
    }
}
