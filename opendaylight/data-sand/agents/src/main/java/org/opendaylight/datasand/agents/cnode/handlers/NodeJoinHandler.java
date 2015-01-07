/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents.cnode.handlers;

import org.opendaylight.datasand.agents.Message;
import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;

/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class NodeJoinHandler<DataType,DataTypeElement> implements ICNodeCommandHandler<DataType,DataTypeElement>{

    @Override
    public void handleMessage(Message cNodeCommand, NetworkID source,NetworkID destination, CPeerEntry<DataType> peerEntry,CNode<DataType, DataTypeElement> node) {
        node.log("Node Join, entering sync mode to sync "+source.getPort());
        node.setSynchronizing(true);
        node.send(new Message(-1,CNode.ENTER_SYNC_MODE,null),source);
        int decCommandID=-1;
        //if this node container data from the source node, it means that the source node was down
        //and now it is up again so send it its original data
        if(node.isLocalPeerCopyContainData(peerEntry.getPeerData())){
            node.log("Found data for "+source.getPort());
            peerEntry.setUnreachable(false);
            for(DataTypeElement e:node.getDataTypeElementCollection(peerEntry.getPeerData())){
                Message command = new Message(decCommandID,CNode.NODE_ORIGINAL_DATA, e);
                node.addUnicastJournal(command, source);
                node.send(command, source);
                decCommandID--;
            }
        }
        //Send it this node map data
        peerEntry.setLastID(1000);
        node.log("Sending my local data to "+source.getPort());
        for(DataTypeElement e:node.getDataTypeElementCollection(node.getLocalData())){
            Message command = new Message(decCommandID,CNode.PEER_SYNC_DATA, e);
            node.addUnicastJournal(command, source);
            node.send(command, source);
            decCommandID--;
        }
        node.cleanJournalHistoryForSource(source);
    }

    @Override
    public void handleUnreachableMessage(Message cNodeCommand,NetworkID unreachableSource, CPeerEntry<DataType> peerEntry,CNode<DataType, DataTypeElement> node) {
    }
}
