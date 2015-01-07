/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents.cnode;

import org.opendaylight.datasand.agents.PeerEntry;
import org.opendaylight.datasand.network.NetworkID;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class CPeerEntry<DataType> extends PeerEntry{
    private DataType peerData;

    public CPeerEntry(NetworkID _netNetworkID,DataType _peerData){
        super(_netNetworkID);
        this.peerData = _peerData;
    }

    public DataType getPeerData() {
        return peerData;
    }
}
