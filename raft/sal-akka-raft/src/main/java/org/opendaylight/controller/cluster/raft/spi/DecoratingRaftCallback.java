/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link RaftCallback} decorating a backing callback.
 */
@NonNullByDefault
public abstract class DecoratingRaftCallback<T> extends RaftCallback<T> {
    protected final RaftCallback<T> delegate;

    /**
     * Default constructor.
     *
     * @param delegate the delegate
     */
    protected DecoratingRaftCallback(final RaftCallback<T> delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("delegate", delegate);
    }
}
