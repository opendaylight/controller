/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

// FIXME: nullable?
public class PeerAddressResolved {
    private final String peerId;
    private final String peerAddress;

    public PeerAddressResolved(final String peerId, final String peerAddress) {
        this.peerId = peerId;
        this.peerAddress = peerAddress;
    }

    public String getPeerId() {
        return peerId;
    }

    public String getPeerAddress() {
        return peerAddress;
    }
}
