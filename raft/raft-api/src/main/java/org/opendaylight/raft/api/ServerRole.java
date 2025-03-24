/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.api;

/**
 * The role this server is playing in the RAFT protocol.
 */
public enum ServerRole {
    /**
     * A candidate server.
     */
    CANDIDATE,
    /**
     * A follower server.
     */
    FOLLOWER,
    /**
     * A leader server.
     */
    LEADER
}
