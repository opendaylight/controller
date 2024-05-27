/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

/**
 * Enumerates voting states for a peer.
 *
 * @author Thomas Pantelis
 */
public enum VotingState {
    VOTING,
    NON_VOTING,
    VOTING_NOT_INITIALIZED
}
