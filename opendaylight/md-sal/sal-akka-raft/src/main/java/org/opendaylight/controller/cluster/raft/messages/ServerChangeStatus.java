/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

/**
 * Enumerates server configuration change status reply codes.
 *
 * @author Thomas Pantelis
 */
public enum ServerChangeStatus {
    /**
     * Request successfully completed.
     */
    OK,

    /**
     * No leader exists to process the request.
     */
    NO_LEADER,

    /**
     * For an AddServer request, the leader timed out trying to install a snapshot on the new server.
     */
    TIMEOUT,

    /**
     * For an AddServer request, the server to add already exists.
     */
    ALREADY_EXISTS,

    /**
     * For a RemoveServer request, the server to remove does not exist.
     */
    DOES_NOT_EXIST,

    /**
     * The leader could not process the request due to a prior request that timed out while trying to
     * achieve replication consensus.
     */
    PRIOR_REQUEST_CONSENSUS_TIMEOUT,

    /**
     * An unsupported request, for example removing the leader in a single node cluster.
     */
    NOT_SUPPORTED,

    /**
     * Some part of the request is invalid.
     */
    INVALID_REQUEST,
}
