package org.opendaylight.datasand.agents.cnode.handlers;

import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CNodeCommand;
import org.opendaylight.datasand.agents.cnode.CNodeCommandEntry;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;

public class ARPMulticastHandler<DataType, DataTypeElement> implements ICNodeCommandHandler<DataType, DataTypeElement>{
    @Override
    public void handle(CNodeCommand<DataTypeElement> cNodeCommand, boolean isUnreachable,
            NetworkID source, NetworkID sourceForUnreachable,
            CPeerEntry<DataType> peerEntry,
            CNode<DataType, DataTypeElement> node) {
            if(!isUnreachable){
                //update peer data
                peerEntry.timeStamp();
                if(peerEntry.getLastID()>cNodeCommand.getID() && cNodeCommand.getID()!=999){
                    node.log("Peer "+source.getPort()+" Need Synchronize");
                }
                if(cNodeCommand.getID()!=999){
                    if(peerEntry.getLastID()!=cNodeCommand.getID()){
                        node.log("Detected unsync with "+source.getPort());
                        node.setSynchronizing(true);
                        node.send(new CNodeCommand<DataTypeElement>(-1,CNode.REQUEST_JOURNAL_DATA,null),source);
                        for(CNodeCommandEntry<DataTypeElement> e:node.getJournalEntries()){
                            if(e.containSource(source)){
                                node.send(e.getcNodeCommand(), source);
                            }
                        }
                        node.send(new CNodeCommand<DataTypeElement>(node.getNextID()-1,CNode.SET_CURRENT_PEER_ID,null), source);
                    }else{
                        peerEntry.setLastID(cNodeCommand.getID());
                    }
                }
            }
    }
}
