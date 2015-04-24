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

public class ShardInfoListenerRegistration<T extends ShardInfoListener> extends AbstractObjectRegistration<T> {
    private final ActorContext parent;

    protected ShardInfoListenerRegistration(final T instance, final ActorContext parent) {
        super(instance);
        this.parent = Preconditions.checkNotNull(parent);
    }

    @Override
    protected void removeRegistration() {
        parent.removeShardInfoListener(this);
    }
}
