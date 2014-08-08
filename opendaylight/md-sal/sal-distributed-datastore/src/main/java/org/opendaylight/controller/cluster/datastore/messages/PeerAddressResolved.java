/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;

public class PeerAddressResolved {
    private final ShardIdentifier peerId;
    private final String peerAddress;

    public PeerAddressResolved(ShardIdentifier peerId, String peerAddress) {
        this.peerId = peerId;
        this.peerAddress = peerAddress;
    }

    public ShardIdentifier getPeerId() {
        return peerId;
    }

    public String getPeerAddress() {
        return peerAddress;
    }
}
