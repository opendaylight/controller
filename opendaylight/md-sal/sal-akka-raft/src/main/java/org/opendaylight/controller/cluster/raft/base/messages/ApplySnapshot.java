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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Internal message, issued by follower to its actor.
 */
@NonNullByDefault
public record ApplySnapshot(Snapshot snapshot, Callback callback) implements ControlMessage {

    public interface Callback {

        void onSuccess();

        void onFailure();
    }

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

    public ApplySnapshot {
        requireNonNull(snapshot);
        requireNonNull(callback);
    }

    public ApplySnapshot(final Snapshot snapshot) {
        this(snapshot, NOOP_CALLBACK);
    }
}
