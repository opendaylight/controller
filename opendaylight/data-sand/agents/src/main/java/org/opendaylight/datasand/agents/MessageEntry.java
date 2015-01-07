/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.datasand.network.NetworkID;

/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class MessageEntry {
    public static long DEFAULT_TIMEOUT = 10000;

    private Message message = null;
    private long timeout = -1;
    private long timeStamp = -1;
    private Set<NetworkID> peers = new HashSet<NetworkID>();

    public MessageEntry(Message _message,NetworkID peer,long _timeout){
        this.message = _message;
        if(peer!=null)
            this.peers.add(peer);
        this.timeout = _timeout;
        this.timeStamp = System.currentTimeMillis();
    }

    public MessageEntry(Message _message,long _timeout){
        this(_message,null,_timeout);
    }

    public MessageEntry(Message _message){
        this(_message,null,DEFAULT_TIMEOUT);
    }

    public Message getMessage() {
        return message;
    }

    public boolean hasTimedOut(){
        if(System.currentTimeMillis()-this.timeStamp>this.timeout)
            return true;
        else
            return false;
    }

    public void addAllPeers(Collection<NetworkID> _peers){
        this.peers.addAll(_peers);
    }

    public void addPeer(NetworkID peer){
        this.peers.add(peer);
    }

    public boolean removePeer(NetworkID peer){
        return this.peers.remove(peer);
    }

    public boolean isFinished(){
        return this.peers.isEmpty();
    }

    public Set<NetworkID> getPeers(){
        return this.peers;
    }

    public boolean containPeer(NetworkID peer){
        return this.peers.contains(peer);
    }
}
