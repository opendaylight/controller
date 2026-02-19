/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActor;

/**
 * A simple callback interface. Guaranteed to be invoked in {@link RaftActor} confinement.
 *
 * @param <T> type of successful result
 * @apiNote We choose an abstract class over a functional interface so we can force a {@link #toString()} implementation
 *          friendly to logging -- quite the opposite of what lambdas would do.
 */
public abstract class RaftCallback<T> {
    /**
     * Invoke the callback.
     *
     * @param failure failure cause, {@code null} if successful
     * @param success successful result, only valid if {@code failure == null}
     */
    public abstract void invoke(@Nullable Exception failure, T success);

    /**
     * Enrich a {@link ToStringHelper} with class-specific attributes.
     *
     * @param helper the helper
     * @return the helper
     */
    @NonNullByDefault
    protected abstract ToStringHelper addToStringAttributes(ToStringHelper helper);

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }
}
