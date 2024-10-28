/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Ticker;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;

@NonNullByDefault
abstract sealed class ShardBehavior permits ActiveShard, InactiveShard {
    final ShardIdentifier id;
    final Ticker ticker;
    // FIXME: we should not be using a plain string here
    final String shardId;

    ShardBehavior(final ShardIdentifier id, final Ticker ticker) {
        this.id = requireNonNull(id);
        this.ticker = requireNonNull(ticker);
        shardId = id.toString();
    }

    ShardBehavior(final ShardBehavior prev) {
        id = prev.id;
        ticker = prev.ticker;
        shardId = prev.shardId;
    }

    final long tickerElapsed(final long since) {
        return ticker.read() - since;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final boolean equals(final @Nullable Object obj) {
        return super.equals(obj);
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("id", shardId);
    }
}
