package org.opendaylight.datasand.agents.cnode;

import org.opendaylight.datasand.network.NetworkID;

public interface ICNodeCommandHandler<DataType,DataTypeElement> {
    public void handle(CNodeCommand<DataTypeElement> cNodeCommand, boolean isUnreachable,NetworkID source,NetworkID sourceForUnreachable,CPeerEntry<DataType> peerEntry,CNode<DataType,DataTypeElement> node);
}
