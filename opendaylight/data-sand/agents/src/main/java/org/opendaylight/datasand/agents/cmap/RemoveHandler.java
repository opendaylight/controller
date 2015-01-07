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

import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CNodeCommand;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class RemoveHandler<K,V> implements ICNodeCommandHandler<Map<K,V>,CMapEntry<K,V>>{

    @Override
    public void handle(CNodeCommand<CMapEntry<K, V>> cNodeCommand, boolean isUnreachable,
            NetworkID source, NetworkID sourceForUnreachable,
            CPeerEntry<Map<K, V>> peerEntry, CNode<Map<K, V>, CMapEntry<K,V>> node) {
                if(!isUnreachable){
                    CMap<K, V> cmap = (CMap<K, V>)node;
                    Object o = peerEntry.getPeerData().remove(cNodeCommand.getData().getKey());
                    if(o!=null && !cmap.containsKey(cNodeCommand.getData().getKey())){
                        cmap.decreaseSize();
                    }
                    peerEntry.timeStamp();
                    peerEntry.setLastID(cNodeCommand.getID());
                    node.sendAcknowledge(cNodeCommand, source);
                    if(cmap.getListener()!=null){
                        cmap.getListener().peerRemove(cNodeCommand.getData().getKey());
                    }
                }
    }
}
