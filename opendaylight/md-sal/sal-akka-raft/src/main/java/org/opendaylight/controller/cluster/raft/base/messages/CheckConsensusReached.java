/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

/**
 * Internal message sent to the leader after persistence is complete to check if replication consensus has been reached.
 *
 * @author Thomas Pantelis
 */
public final class CheckConsensusReached {
    public static final CheckConsensusReached INSTANCE = new CheckConsensusReached();

    private CheckConsensusReached() {
        // Hidden on purpose
    }
}
