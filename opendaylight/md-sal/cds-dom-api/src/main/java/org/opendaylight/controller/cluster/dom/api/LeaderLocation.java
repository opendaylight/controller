/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.dom.api;

import com.google.common.annotations.Beta;

/**
 * Enumeration of possible shard leader locations relative to the local node.
 *
 * @author Robert Varga
 */
@Beta
public enum LeaderLocation {
    /**
     * The leader is co-located on this node.
     */
    LOCAL,
    /**
     * The leader is resident on a different node.
     */
    REMOTE,
    /**
     * The leader is residence is currently unknown. This is a transition state during a leader failure, which can be
     * caused by a network partition. This state is not observed during a leadership transfer vote initiated by
     * the leader shutting down.
     */
    UNKNOWN,
}
