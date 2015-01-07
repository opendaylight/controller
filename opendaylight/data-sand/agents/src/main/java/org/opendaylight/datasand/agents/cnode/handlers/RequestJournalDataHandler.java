/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents.cnode.handlers;

import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CNodeCommand;
import org.opendaylight.datasand.agents.cnode.CNodeCommandEntry;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class RequestJournalDataHandler<DataType, DataTypeElement> implements ICNodeCommandHandler<DataType, DataTypeElement>{

    @Override
    public void handle(CNodeCommand<DataTypeElement> cNodeCommand, boolean isUnreachable,
            NetworkID source, NetworkID sourceForUnreachable,
            CPeerEntry<DataType> peerEntry,
            CNode<DataType, DataTypeElement> node) {
                if(!isUnreachable){
                    node.log("Requested Journal Data from "+source.getPort());
                    node.setSynchronizing(true);
                    for(CNodeCommandEntry<DataTypeElement> e:node.getJournalEntries()){
                        if(e.containSource(source)){
                            node.send(e.getcNodeCommand(), source);
                        }
                    }
                    node.send(new CNodeCommand<DataTypeElement>(node.getNextID()-1,CNode.SET_CURRENT_PEER_ID_REPLY,null), source);
                    synchronized(node){
                        node.log("Finish Sync");
                        node.setSynchronizing(false);
                        node.notifyAll();
                    }
                }
    }

}
