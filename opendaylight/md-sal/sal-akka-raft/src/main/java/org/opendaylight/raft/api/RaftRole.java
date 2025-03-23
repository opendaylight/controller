/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.api;

/**
 * Enumeration of know RAFT roles. Please use {@link #name()} and {@link #valueOf(String)} for conversion to/from
 * canonical string.
 */
public enum RaftRole {
    Candidate,
    Follower,
    Leader,
    IsolatedLeader,
    PreLeader
}
