package org.opendaylight.datasand.agents.cnode;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.datasand.network.NetworkID;

public class CNodeCommandEntry<DataTypeElement> {
    private CNodeCommand<DataTypeElement> cNodeCommand = null;
    private Set<NetworkID> waitingPeersReply = new HashSet<NetworkID>();
    private long timeStamp = -1;

    public CNodeCommandEntry(CNodeCommand<DataTypeElement> _cNodeCommand){
        this.cNodeCommand = _cNodeCommand;
        this.timeStamp = System.currentTimeMillis();
    }

    public CNodeCommand<DataTypeElement> getcNodeCommand() {
        return cNodeCommand;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void addToWaitingPeerReply(NetworkID nid){
        this.waitingPeersReply.add(nid);
    }

    public boolean isEmpty(){
        return this.waitingPeersReply.isEmpty();
    }

    public boolean containSource(NetworkID source){
        return this.waitingPeersReply.contains(source);
    }

    public void removeFromWaitingPeerReply(NetworkID nid){
        this.waitingPeersReply.remove(nid);
    }

    public String toString(){
        StringBuffer buff = new StringBuffer();
        buff.append("OP=").append(this.cNodeCommand.getOperation()).append("\n");
        for(NetworkID id:waitingPeersReply){
            buff.append(id).append("\n");
        }
        return buff.toString();
    }

    public boolean isTimeOut(){
        if(System.currentTimeMillis()-this.timeStamp>5000)
            return true;
        return false;
    }
}
