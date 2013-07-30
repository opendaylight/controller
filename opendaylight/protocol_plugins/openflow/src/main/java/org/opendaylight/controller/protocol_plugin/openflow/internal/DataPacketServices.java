
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import org.opendaylight.controller.protocol_plugin.openflow.IDataPacketMux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.sal.connection.IPluginOutConnectionService;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.IPluginInDataPacketService;
import org.opendaylight.controller.sal.packet.RawPacket;

public class DataPacketServices implements IPluginInDataPacketService {
    protected static final Logger logger = LoggerFactory
            .getLogger(DataPacketServices.class);
    private IDataPacketMux iDataPacketMux = null;
    private IPluginOutConnectionService connectionOutService;

    void setIDataPacketMux(IDataPacketMux s) {
        this.iDataPacketMux = s;
    }

    void unsetIDataPacketMux(IDataPacketMux s) {
        if (this.iDataPacketMux == s) {
            this.iDataPacketMux = null;
        }
    }

    void setIPluginOutConnectionService(IPluginOutConnectionService s) {
        connectionOutService = s;
    }

    void unsetIPluginOutConnectionService(IPluginOutConnectionService s) {
        if (connectionOutService == s) {
            connectionOutService = null;
        }
    }

    @Override
    public void transmitDataPacket(RawPacket outPkt) {
        NodeConnector nc = outPkt.getOutgoingNodeConnector();
        if (connectionOutService != null && connectionOutService.isLocal(nc.getNode())) {
            this.iDataPacketMux.transmitDataPacket(outPkt);
        } else {
            logger.debug("{} is dropped in the controller "+outPkt);
        }
    }
}
