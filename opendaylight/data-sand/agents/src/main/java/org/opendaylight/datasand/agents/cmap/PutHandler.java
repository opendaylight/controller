package org.opendaylight.datasand.agents.cmap;

import java.util.Map;

import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CNodeCommand;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.agents.cnode.ICNodeCommandHandler;
import org.opendaylight.datasand.network.NetworkID;

public class PutHandler<K,V> implements ICNodeCommandHandler<Map<K,V>,CMapEntry<K,V>>{

    @Override
    public void handle(CNodeCommand<CMapEntry<K, V>> cNodeCommand, boolean isUnreachable,
            NetworkID source, NetworkID sourceForUnreachable,
            CPeerEntry<Map<K, V>> peerEntry, CNode<Map<K, V>, CMapEntry<K,V>> node) {
                if(!isUnreachable){
                    CMap<K,V> cmap = (CMap<K,V>)node;
                    node.log("Putting Key:"+cNodeCommand.getData().getKey());
                    if(!cmap.containsKey(cNodeCommand.getData().getKey())){
                        cmap.increaseSize();
                    }
                    peerEntry.getPeerData().put(cNodeCommand.getData().getKey(), cNodeCommand.getData().getValue());
                    peerEntry.timeStamp();
                    peerEntry.setLastID(cNodeCommand.getID());
                    cmap.sendAcknowledge(cNodeCommand, source);
                    if(cmap.getListener()!=null){
                        cmap.getListener().peerPut(cNodeCommand.getData().getKey(), cNodeCommand.getData().getValue());
                    }
                }
    }
}
