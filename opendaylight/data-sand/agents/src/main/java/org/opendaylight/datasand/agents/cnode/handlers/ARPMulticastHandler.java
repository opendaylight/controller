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
import org.opendaylight.datasand.agents.MessageEntry;
import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class ARPMulticastHandler<DataType, DataTypeElement> implements ICNodeCommandHandler<DataType, DataTypeElement>{

    @Override
    public void handleMessage(Message cNodeCommand, NetworkID source,NetworkID destination, CPeerEntry<DataType> peerEntry,CNode<DataType, DataTypeElement> node) {
        //update peer data
        peerEntry.timeStamp();
        if(peerEntry.getLastID()>cNodeCommand.getMessageID() && cNodeCommand.getMessageID()!=999){
            node.log("Peer "+source.getPort()+" Need Synchronize");
        }
        if(cNodeCommand.getMessageID()!=999){
            if(peerEntry.getLastID()!=cNodeCommand.getMessageID()){
                node.log("Detected unsync with "+source.getPort());
                node.setSynchronizing(true);
                node.send(new Message(-1,CNode.REQUEST_JOURNAL_DATA,null),source);
                for(MessageEntry e:node.getJournalEntries()){
                    if(e.containPeer(source)){
                        node.send(e.getMessage(), source);
                    }
                }
                node.send(new Message(node.getNextID()-1,CNode.SET_CURRENT_PEER_ID,null), source);
            }else{
                peerEntry.setLastID(cNodeCommand.getMessageID());
            }
        }
    }

    @Override
    public void handleUnreachableMessage(Message cNodeCommand,NetworkID unreachableSource, CPeerEntry<DataType> peerEntry,CNode<DataType, DataTypeElement> node) {
    }
}
