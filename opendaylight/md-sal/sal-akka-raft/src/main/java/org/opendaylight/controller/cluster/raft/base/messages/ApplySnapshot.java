/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.Snapshot;

/**
 * Internal message, issued by follower to its actor
 */
public class ApplySnapshot {
    private static final Callback NOOP_CALLBACK = new Callback() {
        @Override
        public void onSuccess() {
            // No-op
        }

        @Override
        public void onFailure() {
            // No-op
        }
    };

    private final Snapshot snapshot;
    private final Callback callback;

    public ApplySnapshot(Snapshot snapshot) {
        this(snapshot, NOOP_CALLBACK);
    }

    public ApplySnapshot(@Nonnull Snapshot snapshot, @Nonnull Callback callback) {
        this.snapshot = Preconditions.checkNotNull(snapshot);
        this.callback = Preconditions.checkNotNull(callback);
    }

    @Nonnull
    public Snapshot getSnapshot() {
        return snapshot;
    }

    @Nonnull
    public Callback getCallback() {
        return callback;
    }

    public interface Callback {
        void onSuccess();

        void onFailure();
    }
}
