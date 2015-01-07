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
public class PeerSyncDataHandler <DataType, DataTypeElement> implements ICNodeCommandHandler<DataType, DataTypeElement>{

    @Override
    public void handleMessage(Message cNodeCommand, NetworkID source,NetworkID destination, CPeerEntry<DataType> peerEntry,CNode<DataType, DataTypeElement> node) {
        node.handlePeerSyncData((DataTypeElement)cNodeCommand.getMessageData(),source);
        /*
        if(listener!=null){
            listener.peerPut((K) cmd.getKey(), (V) cmd.getValue());
        }*/
        node.sendAcknowledge(cNodeCommand, source);
    }

    @Override
    public void handleUnreachableMessage(Message cNodeCommand,NetworkID unreachableSource, CPeerEntry<DataType> peerEntry,CNode<DataType, DataTypeElement> node) {
    }
}
