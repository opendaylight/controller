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
    OK,
    NO_LEADER,
    TIMEOUT,
    ALREADY_EXISTS,
    DOES_NOT_EXIST,  // Server with the specified address does not exist
    NOT_SUPPORTED,   // Some types of RemoveServer for example Removing the current Leader may not be
                     // supported (at least initially)
}
