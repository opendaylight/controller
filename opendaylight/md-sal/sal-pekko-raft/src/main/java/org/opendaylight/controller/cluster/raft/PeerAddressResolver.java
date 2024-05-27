/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface to resolve raft actor peer addresses.
 *
 * @author Thomas Pantelis
 */
@FunctionalInterface
public interface PeerAddressResolver {
    /**
     * Resolves a raft actor peer id to its remote actor address.
     *
     * @param peerId the id of the peer to resolve
     * @return the peer's actor path string or null if not found
     */
    @Nullable String resolve(String peerId);

    /**
     * Sets the actor address for a raft peer.
     *
     * @param peerId the id of the peer
     * @param address the peer's actor's address
     */
    default void setResolved(String peerId, String address) {

    }
}
