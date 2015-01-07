package org.opendaylight.datasand.agents.cnode.handlers;

import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CNodeCommand;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;

public class PeerSyncDataHandler <DataType, DataTypeElement> implements ICNodeCommandHandler<DataType, DataTypeElement>{
    @Override
    public void handle(CNodeCommand<DataTypeElement> cNodeCommand, boolean isUnreachable,
            NetworkID source, NetworkID sourceForUnreachable,
            CPeerEntry<DataType> peerEntry,
            CNode<DataType, DataTypeElement> node) {
                if(!isUnreachable){
                    node.handlePeerSyncData((DataTypeElement)cNodeCommand.getData(),source);
                    /*
                    if(listener!=null){
                        listener.peerPut((K) cmd.getKey(), (V) cmd.getValue());
                    }*/
                    node.sendAcknowledge(cNodeCommand, source);
                }
    }
}
