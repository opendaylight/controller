package org.opendaylight.datasand.agents.cnode.handlers;

import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CNodeCommand;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;


public class NodeJoinHandler<DataType,DataTypeElement> implements ICNodeCommandHandler<DataType,DataTypeElement>{
    @Override
    public void handle(CNodeCommand<DataTypeElement> cNodeCommand, boolean isUnreachable,NetworkID source, NetworkID sourceForUnreachable,CPeerEntry<DataType> peerEntry, CNode<DataType, DataTypeElement> node) {
        if(!isUnreachable){
            node.log("Node Join, entering sync mode to sync "+source.getPort());
            node.setSynchronizing(true);
            node.send(new CNodeCommand<DataTypeElement>(-1,CNode.ENTER_SYNC_MODE,null),source);
            int decCommandID=-1;
            //if this node container data from the source node, it means that the source node was down
            //and now it is up again so send it its original data
            if(node.isLocalPeerCopyContainData(peerEntry.getPeerData())){
                node.log("Found data for "+source.getPort());
                peerEntry.setUnreachable(false);
                for(DataTypeElement e:node.getDataTypeElementCollection(peerEntry.getPeerData())){
                    CNodeCommand<DataTypeElement> command = new CNodeCommand<DataTypeElement>(decCommandID,CNode.NODE_ORIGINAL_DATA, e);
                    node.addJournalForUnicast(command, source);
                    node.send(command, source);
                    decCommandID--;
                }
            }
            //Send it this node map data
            peerEntry.setLastID(1000);
            node.log("Sending my local data to "+source.getPort());
            for(DataTypeElement e:node.getDataTypeElementCollection(node.getLocalData())){
                CNodeCommand<DataTypeElement> command = new CNodeCommand<DataTypeElement>(decCommandID,CNode.PEER_SYNC_DATA, e);
                node.addJournalForUnicast(command, source);
                node.send(command, source);
                decCommandID--;
            }
            node.cleanJournalHistoryForSource(source);
        }
    }
}
