package org.opendaylight.datasand.agents.cnode.handlers;

import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CNodeCommand;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;

public class SetCurrentPeerIDHandler <DataType, DataTypeElement> implements ICNodeCommandHandler<DataType, DataTypeElement>{
    @Override
    public void handle(CNodeCommand<DataTypeElement> cNodeCommand, boolean isUnreachable,
            NetworkID source, NetworkID sourceForUnreachable,
            CPeerEntry<DataType> peerEntry,
            CNode<DataType, DataTypeElement> node) {
                if(!isUnreachable){
                    peerEntry.setLastID(cNodeCommand.getID());
                    if(node.getNextID()==1000)
                        node.incrementID();
                    node.send(new CNodeCommand<DataTypeElement>(node.getNextID()-1,CNode.SET_CURRENT_PEER_ID_REPLY,null), source);
                    synchronized(node){
                        node.log("Finished Synchronizing with "+source.getPort());
                        node.setSynchronizing(false);
                        node.notifyAll();
                    }
                }
    }
}
