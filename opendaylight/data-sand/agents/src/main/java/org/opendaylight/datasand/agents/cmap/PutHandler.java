/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents.cmap;

import java.util.Map;

import org.opendaylight.datasand.agents.Message;
import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class PutHandler<K,V> implements ICNodeCommandHandler<Map<K,V>,CMapEntry<K,V>>{

    @Override
    public void handleMessage(Message cNodeCommand, NetworkID source,NetworkID destination, CPeerEntry<Map<K, V>> peerEntry,CNode<Map<K, V>, CMapEntry<K, V>> node) {
        CMap<K,V> cmap = (CMap<K,V>)node;
        node.log("Putting Key:"+((CMapEntry<K,V>)cNodeCommand.getMessageData()).getKey());
        if(!cmap.containsKey(((CMapEntry<K,V>)cNodeCommand.getMessageData()).getKey())){
            cmap.increaseSize();
        }
        peerEntry.getPeerData().put(((CMapEntry<K,V>)cNodeCommand.getMessageData()).getKey(), ((CMapEntry<K,V>)cNodeCommand.getMessageData()).getValue());
        peerEntry.timeStamp();
        peerEntry.setLastID(cNodeCommand.getMessageID());
        cmap.sendAcknowledge(cNodeCommand, source);
        if(cmap.getListener()!=null){
            cmap.getListener().peerPut(((CMapEntry<K,V>)cNodeCommand.getMessageData()).getKey(), ((CMapEntry<K,V>)cNodeCommand.getMessageData()).getValue());
        }
    }

    @Override
    public void handleUnreachableMessage(Message cNodeCommand,NetworkID unreachableSource, CPeerEntry<Map<K, V>> peerEntry,CNode<Map<K, V>, CMapEntry<K, V>> node) {
    }
}
