/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Internal message, issued by follower to its actor.
 */
public class ApplySnapshot implements ControlMessage {
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

    public ApplySnapshot(@NonNull Snapshot snapshot) {
        this(snapshot, NOOP_CALLBACK);
    }

    public ApplySnapshot(@NonNull Snapshot snapshot, @NonNull Callback callback) {
        this.snapshot = requireNonNull(snapshot);
        this.callback = requireNonNull(callback);
    }

    public @NonNull Snapshot getSnapshot() {
        return snapshot;
    }

    public @NonNull Callback getCallback() {
        return callback;
    }

    public interface Callback {
        void onSuccess();

        void onFailure();
    }
}
