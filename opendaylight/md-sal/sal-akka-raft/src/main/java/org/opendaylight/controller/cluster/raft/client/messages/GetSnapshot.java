/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Internal client message to get a snapshot of the current state based on whether or not persistence is enabled.
 * Returns a {@link GetSnapshotReply} instance.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public record GetSnapshot(Duration timeout) {

    public GetSnapshot {
        requireNonNull(timeout);
    }
}
