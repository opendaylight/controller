/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.protocol_plugin.openflow.mapping.impl;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.OFActionMapper;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.FloodAll;
import org.opendaylight.controller.sal.action.HwPath;
import org.opendaylight.controller.sal.action.Loopback;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetNwTos;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.action.SetVlanPcp;
import org.opendaylight.controller.sal.action.SwPath;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionNetworkTypeOfService;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionTransportLayerDestination;
import org.openflow.protocol.action.OFActionTransportLayerSource;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;
import org.openflow.protocol.action.OFActionVirtualLanPriorityCodePoint;

public class OFActionMappings {

    public static final OFActionMapper<OFActionOutput> Output = new OFActionMapper<OFActionOutput>() {
        public Class<OFActionOutput> getOfClass() {
            return OFActionOutput.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            short ofPort = ((OFActionOutput) ofAction).getPort();
            if (ofPort == OFPort.OFPP_CONTROLLER.getValue()) {
                salAction = new Controller();
            } else if (ofPort == OFPort.OFPP_NONE.getValue()) {
                salAction = new Drop();
            } else if (ofPort == OFPort.OFPP_IN_PORT.getValue()) {
                salAction = new Loopback();
            } else if (ofPort == OFPort.OFPP_FLOOD.getValue()) {
                salAction = new Flood();
            } else if (ofPort == OFPort.OFPP_ALL.getValue()) {
                salAction = new FloodAll();
            } else if (ofPort == OFPort.OFPP_LOCAL.getValue()) {
                salAction = new SwPath();
            } else if (ofPort == OFPort.OFPP_NORMAL.getValue()) {
                salAction = new HwPath();
            } else if (ofPort == OFPort.OFPP_TABLE.getValue()) {
                salAction = new HwPath(); // TODO: we do not handle
                                          // table in sal for now
            } else {
                salAction = new Output(
                        NodeConnectorCreator
                                .createOFNodeConnector(ofPort, node));
            }
            return salAction;
        }
    };

    public static final OFActionMapper<OFActionVirtualLanIdentifier> VirtualLanIdentifier = new OFActionMapper<OFActionVirtualLanIdentifier>() {
        public Class<OFActionVirtualLanIdentifier> getOfClass() {
            return OFActionVirtualLanIdentifier.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            salAction = new SetVlanId(
                    ((OFActionVirtualLanIdentifier) ofAction)
                            .getVirtualLanIdentifier());
            return salAction;
        }
    };

    public static final OFActionMapper<OFActionStripVirtualLan> StripVirtualLan = new OFActionMapper<OFActionStripVirtualLan>() {
        public Class<OFActionStripVirtualLan> getOfClass() {
            return OFActionStripVirtualLan.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            salAction = new PopVlan();
            return salAction;
        }
    };
    public static final OFActionMapper<OFActionVirtualLanPriorityCodePoint> VirtualLanPriorityCodePoint = new OFActionMapper<OFActionVirtualLanPriorityCodePoint>() {
        public Class<OFActionVirtualLanPriorityCodePoint> getOfClass() {
            return OFActionVirtualLanPriorityCodePoint.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            salAction = new SetVlanPcp(
                    ((OFActionVirtualLanPriorityCodePoint) ofAction)
                            .getVirtualLanPriorityCodePoint());
            return salAction;
        }
    };

    public static final OFActionMapper<OFActionDataLayerSource> DataLayerSource = new OFActionMapper<OFActionDataLayerSource>() {
        public Class<OFActionDataLayerSource> getOfClass() {
            return OFActionDataLayerSource.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            salAction = new SetDlSrc(((OFActionDataLayerSource) ofAction)
                    .getDataLayerAddress().clone());
            return salAction;
        }
    };

    public static final OFActionMapper<OFActionDataLayerDestination> DataLayerDestination = new OFActionMapper<OFActionDataLayerDestination>() {
        public Class<OFActionDataLayerDestination> getOfClass() {
            return OFActionDataLayerDestination.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            salAction = new SetDlDst(((OFActionDataLayerDestination) ofAction)
                    .getDataLayerAddress().clone());
            return salAction;
        }
    };

    public static final OFActionMapper<OFActionNetworkLayerSource> NetworkLayerSource = new OFActionMapper<OFActionNetworkLayerSource>() {
        public Class<OFActionNetworkLayerSource> getOfClass() {
            return OFActionNetworkLayerSource.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            InetAddress ip =  NetUtils.getInetAddress(((OFActionNetworkLayerSource) ofAction)
                    .getNetworkAddress());
            salAction = new SetNwSrc(ip);
            return salAction;
        }
    };

    public static final OFActionMapper<OFActionNetworkLayerDestination> NetworkLayerDestination = new OFActionMapper<OFActionNetworkLayerDestination>() {
        public Class<OFActionNetworkLayerDestination> getOfClass() {
            return OFActionNetworkLayerDestination.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            InetAddress ip =  NetUtils.getInetAddress(((OFActionNetworkLayerDestination) ofAction)
                    .getNetworkAddress());
            salAction = new SetNwDst(ip);
            return salAction;
        }
    };

    public static final OFActionMapper<OFActionNetworkTypeOfService> NetworkTypeOfService = new OFActionMapper<OFActionNetworkTypeOfService>() {
        public Class<OFActionNetworkTypeOfService> getOfClass() {
            return OFActionNetworkTypeOfService.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            salAction = new SetNwTos(
                    ((OFActionNetworkTypeOfService) ofAction)
                            .getNetworkTypeOfService());
            return salAction;
        }
    };

    public static final OFActionMapper<OFActionTransportLayerSource> TransportLayerSource = new OFActionMapper<OFActionTransportLayerSource>() {
        public Class<OFActionTransportLayerSource> getOfClass() {
            return OFActionTransportLayerSource.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            Short port = ((OFActionTransportLayerSource) ofAction)
                    .getTransportPort();
            int intPort = NetUtils.getUnsignedShort(port);
            salAction = new SetTpSrc(intPort);
            return salAction;
        }
    };

    public static final OFActionMapper<OFActionTransportLayerDestination> TransportLayerDestination = new OFActionMapper<OFActionTransportLayerDestination>() {
        public Class<OFActionTransportLayerDestination> getOfClass() {
            return OFActionTransportLayerDestination.class;
        }

        public Action salFromOpenflow(OFAction ofAction, Node node) {
            Action salAction = null;
            Short port = ((OFActionTransportLayerDestination) ofAction)
                    .getTransportPort();
            int intPort = NetUtils.getUnsignedShort(port);
            salAction = new SetTpDst(intPort);
            return salAction;
        }
    };
}