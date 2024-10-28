/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.api;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActorContext;

/**
 * Information about a known RAFT member.
 *
 * @param id leader's {@link RaftActorContext#getId()}
 * @param payloadVersion leader's preferred payload version
 */
@NonNullByDefault
public record MemberInfo(String id, short payloadVersion) {
    public MemberInfo {
        requireNonNull(id);
    }
}
