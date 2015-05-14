/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

/**
 * Registration of a {@link ShardInfoListener} instance.
 *
 * @param <T> Type of listener
 */
public class ShardInfoListenerRegistration<T extends ShardInfoListener> extends AbstractObjectRegistration<T> {
    private final PrimaryShardInfoFutureCache parent;

    protected ShardInfoListenerRegistration(final T instance, final PrimaryShardInfoFutureCache parent) {
        super(instance);
        this.parent = Preconditions.checkNotNull(parent);
    }

    @Override
    protected void removeRegistration() {
        parent.removeShardInfoListener(this);
    }
}
