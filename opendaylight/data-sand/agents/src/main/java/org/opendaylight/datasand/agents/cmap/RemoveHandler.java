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
public class RemoveHandler<K,V> implements ICNodeCommandHandler<Map<K,V>,CMapEntry<K,V>>{

    @Override
    public void handleMessage(Message cNodeCommand, NetworkID source,NetworkID destination, CPeerEntry<Map<K, V>> peerEntry,CNode<Map<K, V>, CMapEntry<K, V>> node) {
        CMap<K, V> cmap = (CMap<K, V>)node;
        Object o = peerEntry.getPeerData().remove(((CMapEntry<K,V>)cNodeCommand.getMessageData()).getKey());
        if(o!=null && !cmap.containsKey(((CMapEntry<K,V>)cNodeCommand.getMessageData()).getKey())){
            cmap.decreaseSize();
        }
        peerEntry.timeStamp();
        peerEntry.setLastID(cNodeCommand.getMessageID());
        node.sendAcknowledge(cNodeCommand, source);
        if(cmap.getListener()!=null){
            cmap.getListener().peerRemove(((CMapEntry<K,V>)cNodeCommand.getMessageData()).getKey());
        }
    }

    @Override
    public void handleUnreachableMessage(Message cNodeCommand,NetworkID unreachableSource, CPeerEntry<Map<K, V>> peerEntry,CNode<Map<K, V>, CMapEntry<K, V>> node) {
    }
}
