/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import java.io.Serializable;

/**
 * Interface implemented by all requests exchanged in the Raft protocol.
 */
public interface RaftRPC extends Serializable {
    /**
     * Return the term in which this call is being made.
     *
     * @return The term ID
     */
    public long getTerm();
}
