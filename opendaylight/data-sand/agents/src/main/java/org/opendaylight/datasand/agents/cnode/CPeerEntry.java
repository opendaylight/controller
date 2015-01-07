/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents.cnode;

import org.opendaylight.datasand.network.NetworkID;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class CPeerEntry<DataType> {
    private NetworkID networkID = null;
    private long lastReceivedPing = -1;
    private long lastID = 999;
    private boolean unreachable = false;
    private DataType peerData;

    public CPeerEntry(NetworkID _netNetworkID,DataType _peerData){
        this.networkID = _netNetworkID;
        this.peerData = _peerData;
        this.lastReceivedPing = System.currentTimeMillis();
    }

    public NetworkID getNetworkID() {
        return networkID;
    }
    public long getLastReceivedPing() {
        return lastReceivedPing;
    }
    public long getLastID() {
        return lastID;
    }
    public void setLastID(long id){
        this.lastID = id;
    }
    public void setUnreachable(boolean u){
        this.unreachable = u;
    }
    public boolean isUnreachable() {
        return unreachable;
    }
    public DataType getPeerData() {
        return peerData;
    }
    public void timeStamp(){
        this.lastReceivedPing = System.currentTimeMillis();
    }
}
