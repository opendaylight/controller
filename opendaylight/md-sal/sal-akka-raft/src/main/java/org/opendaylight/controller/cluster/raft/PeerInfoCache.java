/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.Collection;

/**
 * Provides read-only access to cached PeerInfo information.
 *
 * @author Thomas Pantelis
 */
public abstract class PeerInfoCache {
    protected abstract Collection<PeerInfo> getPeers();

    protected abstract PeerAddressResolver peerAddressResolver();

    public String[] getPeerAddresses() {
        Collection<PeerInfo> peers = getPeers();
        String[] peerAddresses = new String[peers.size()];
        int i = 0;
        for(PeerInfo peerInfo: peers) {
            String peerAddress = peerInfo.getAddress();
            if(peerAddress == null) {
                peerAddress = peerAddressResolver().resolve(peerInfo.getId());
            }

            peerAddresses[i++] = peerAddress;
        }

        return peerAddresses;
    }
}
