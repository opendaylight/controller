/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension.V6FlowMod;
import org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension.V6Match;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.action.Enqueue;
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
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFVendor;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayer;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionEnqueue;
import org.openflow.protocol.action.OFActionNetworkLayerAddress;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionNetworkTypeOfService;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionStripVirtualLan;
import org.openflow.protocol.action.OFActionTransportLayer;
import org.openflow.protocol.action.OFActionTransportLayerDestination;
import org.openflow.protocol.action.OFActionTransportLayerSource;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;
import org.openflow.protocol.action.OFActionVirtualLanPriorityCodePoint;
import org.openflow.util.U16;
import org.openflow.util.U32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for converting a SAL Flow into the OF flow and vice-versa
 */
public class FlowConverter {
    protected static final Logger logger = LoggerFactory
            .getLogger(FlowConverter.class);

    /*
     * The value 0xffff (OFP_VLAN_NONE) is used to indicate
     * that no VLAN ID is set for OF Flow.
     */
    private static final short OFP_VLAN_NONE = (short) 0xffff;

    private Flow flow; // SAL Flow
    private OFMatch ofMatch; // OF 1.0 match or OF 1.0 + IPv6 extension match
    private List<OFAction> actionsList; // OF 1.0 actions
    private int actionsLength;
    private boolean isIPv6;

    public FlowConverter(OFMatch ofMatch, List<OFAction> actionsList) {
        this.ofMatch = ofMatch;
        this.actionsList = actionsList;
        this.actionsLength = 0;
        this.flow = null;
        this.isIPv6 = ofMatch instanceof V6Match;
    }

    public FlowConverter(Flow flow) {
        this.ofMatch = null;
        this.actionsList = null;
        this.actionsLength = 0;
        this.flow = flow;
        this.isIPv6 = flow.isIPv6();
    }

    /**
     * Returns the match in OF 1.0 (OFMatch) form or OF 1.0 + IPv6 extensions
     * form (V6Match)
     *
     * @return
     */
    public OFMatch getOFMatch() {
        if (ofMatch == null) {
            Match match = flow.getMatch();
            ofMatch = (isIPv6) ? new V6Match() : new OFMatch();

            int wildcards = OFMatch.OFPFW_ALL;
            if (match.isPresent(MatchType.IN_PORT)) {
                short port = (Short) ((NodeConnector) match.getField(
                        MatchType.IN_PORT).getValue()).getID();
                if (!isIPv6) {
                    ofMatch.setInputPort(port);
                    wildcards &= ~OFMatch.OFPFW_IN_PORT;
                } else {
                    ((V6Match) ofMatch).setInputPort(port, (short) 0);
                }
            }
            if (match.isPresent(MatchType.DL_SRC)) {
                byte[] srcMac = (byte[]) match.getField(MatchType.DL_SRC)
                        .getValue();
                if (!isIPv6) {
                    ofMatch.setDataLayerSource(srcMac.clone());
                    wildcards &= ~OFMatch.OFPFW_DL_SRC;
                } else {
                    ((V6Match) ofMatch).setDataLayerSource(srcMac, null);
                }
            }
            if (match.isPresent(MatchType.DL_DST)) {
                byte[] dstMac = (byte[]) match.getField(MatchType.DL_DST)
                        .getValue();
                if (!isIPv6) {
                    ofMatch.setDataLayerDestination(dstMac.clone());
                    wildcards &= ~OFMatch.OFPFW_DL_DST;
                } else {
                    ((V6Match) ofMatch).setDataLayerDestination(dstMac, null);
                }
            }
            if (match.isPresent(MatchType.DL_VLAN)) {
                short vlan = (Short) match.getField(MatchType.DL_VLAN)
                        .getValue();
                if (vlan == MatchType.DL_VLAN_NONE) {
                    vlan = OFP_VLAN_NONE;
                }
                if (!isIPv6) {
                    ofMatch.setDataLayerVirtualLan(vlan);
                    wildcards &= ~OFMatch.OFPFW_DL_VLAN;
                } else {
                    ((V6Match) ofMatch).setDataLayerVirtualLan(vlan, (short) 0);
                }
            }
            if (match.isPresent(MatchType.DL_VLAN_PR)) {
                byte vlanPr = (Byte) match.getField(MatchType.DL_VLAN_PR)
                        .getValue();
                if (!isIPv6) {
                    ofMatch.setDataLayerVirtualLanPriorityCodePoint(vlanPr);
                    wildcards &= ~OFMatch.OFPFW_DL_VLAN_PCP;
                } else {
                    ((V6Match) ofMatch)
                            .setDataLayerVirtualLanPriorityCodePoint(vlanPr,
                                    (byte) 0);
                }
            }
            if (match.isPresent(MatchType.DL_TYPE)) {
                short ethType = (Short) match.getField(MatchType.DL_TYPE)
                        .getValue();
                if (!isIPv6) {
                    ofMatch.setDataLayerType(ethType);
                    wildcards &= ~OFMatch.OFPFW_DL_TYPE;
                } else {
                    ((V6Match) ofMatch).setDataLayerType(ethType, (short) 0);
                }
            }
            if (match.isPresent(MatchType.NW_TOS)) {
                /*
                 * OF 1.0 switch expects the TOS as the 6 msb in the byte. it is
                 * actually the DSCP field followed by a zero ECN
                 */
                byte tos = (Byte) match.getField(MatchType.NW_TOS).getValue();
                byte dscp = (byte) (tos << 2);
                if (!isIPv6) {
                    ofMatch.setNetworkTypeOfService(dscp);
                    wildcards &= ~OFMatch.OFPFW_NW_TOS;
                } else {
                    ((V6Match) ofMatch).setNetworkTypeOfService(dscp, (byte) 0);
                }
            }
            if (match.isPresent(MatchType.NW_PROTO)) {
                byte proto = (Byte) match.getField(MatchType.NW_PROTO)
                        .getValue();
                if (!isIPv6) {
                    ofMatch.setNetworkProtocol(proto);
                    wildcards &= ~OFMatch.OFPFW_NW_PROTO;
                } else {
                    ((V6Match) ofMatch).setNetworkProtocol(proto, (byte) 0);
                }
            }
            if (match.isPresent(MatchType.NW_SRC)) {
                InetAddress address = (InetAddress) match.getField(MatchType.NW_SRC).getValue();
                InetAddress mask = (InetAddress) match.getField(MatchType.NW_SRC).getMask();
                if (!isIPv6) {
                    ofMatch.setNetworkSource(NetUtils.byteArray4ToInt(address.getAddress()));
                    int maskLength = (mask == null) ? 32 : NetUtils.getSubnetMaskLength(mask);
                    wildcards = (wildcards & ~OFMatch.OFPFW_NW_SRC_MASK) | ((32 - maskLength) << OFMatch.OFPFW_NW_SRC_SHIFT);
                } else {
                    ((V6Match) ofMatch).setNetworkSource(address, mask);
                }
            }
            if (match.isPresent(MatchType.NW_DST)) {
                InetAddress address = (InetAddress) match.getField(MatchType.NW_DST).getValue();
                InetAddress mask = (InetAddress) match.getField(MatchType.NW_DST).getMask();
                if (!isIPv6) {
                    ofMatch.setNetworkDestination(NetUtils.byteArray4ToInt(address.getAddress()));
                    int maskLength = (mask == null) ? 32 : NetUtils.getSubnetMaskLength(mask);
                    wildcards = (wildcards & ~OFMatch.OFPFW_NW_DST_MASK) | ((32 - maskLength) << OFMatch.OFPFW_NW_DST_SHIFT);
                } else {
                    ((V6Match) ofMatch).setNetworkDestination(address, mask);
                }
            }
            if (match.isPresent(MatchType.TP_SRC)) {
                short port = (Short) match.getField(MatchType.TP_SRC)
                        .getValue();
                if (!isIPv6) {
                    ofMatch.setTransportSource(port);
                    wildcards &= ~OFMatch.OFPFW_TP_SRC;
                } else {
                    ((V6Match) ofMatch).setTransportSource(port, (short) 0);
                }
            }
            if (match.isPresent(MatchType.TP_DST)) {
                short port = (Short) match.getField(MatchType.TP_DST)
                        .getValue();
                if (!isIPv6) {
                    ofMatch.setTransportDestination(port);
                    wildcards &= ~OFMatch.OFPFW_TP_DST;
                } else {
                    ((V6Match) ofMatch)
                            .setTransportDestination(port, (short) 0);
                }
            }

            if (!isIPv6) {
                ofMatch.setWildcards(U32.t(Long.valueOf(wildcards)));
            }
        }
        logger.trace("SAL Match: {} Openflow Match: {}", flow.getMatch(),
                ofMatch);
        return ofMatch;
    }

    /**
     * Returns the list of actions in OF 1.0 form
     *
     * @return
     */
    public List<OFAction> getOFActions() {
        if (this.actionsList == null) {
            actionsList = new ArrayList<OFAction>();
            for (Action action : flow.getActions()) {
                if (action.getType() == ActionType.OUTPUT) {
                    Output a = (Output) action;
                    OFActionOutput ofAction = new OFActionOutput();
                    ofAction.setMaxLength((short) 0xffff);
                    ofAction.setPort(PortConverter.toOFPort(a.getPort()));
                    actionsList.add(ofAction);
                    actionsLength += OFActionOutput.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.ENQUEUE) {
                    Enqueue a = (Enqueue) action;
                    OFActionEnqueue ofAction = new OFActionEnqueue();
                    ofAction.setPort(PortConverter.toOFPort(a.getPort()));
                    ofAction.setQueueId(a.getQueue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionEnqueue.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.DROP) {
                    continue;
                }
                if (action.getType() == ActionType.LOOPBACK) {
                    OFActionOutput ofAction = new OFActionOutput();
                    ofAction.setPort(OFPort.OFPP_IN_PORT.getValue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionOutput.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.FLOOD) {
                    OFActionOutput ofAction = new OFActionOutput();
                    ofAction.setPort(OFPort.OFPP_FLOOD.getValue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionOutput.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.FLOOD_ALL) {
                    OFActionOutput ofAction = new OFActionOutput();
                    ofAction.setPort(OFPort.OFPP_ALL.getValue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionOutput.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.CONTROLLER) {
                    OFActionOutput ofAction = new OFActionOutput();
                    ofAction.setPort(OFPort.OFPP_CONTROLLER.getValue());
                    // We want the whole frame hitting the match be sent to the
                    // controller
                    ofAction.setMaxLength((short) 0xffff);
                    actionsList.add(ofAction);
                    actionsLength += OFActionOutput.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SW_PATH) {
                    OFActionOutput ofAction = new OFActionOutput();
                    ofAction.setPort(OFPort.OFPP_LOCAL.getValue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionOutput.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.HW_PATH) {
                    OFActionOutput ofAction = new OFActionOutput();
                    ofAction.setPort(OFPort.OFPP_NORMAL.getValue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionOutput.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_VLAN_ID) {
                    SetVlanId a = (SetVlanId) action;
                    OFActionVirtualLanIdentifier ofAction = new OFActionVirtualLanIdentifier();
                    ofAction.setVirtualLanIdentifier((short) a.getVlanId());
                    actionsList.add(ofAction);
                    actionsLength += OFActionVirtualLanIdentifier.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_VLAN_PCP) {
                    SetVlanPcp a = (SetVlanPcp) action;
                    OFActionVirtualLanPriorityCodePoint ofAction = new OFActionVirtualLanPriorityCodePoint();
                    ofAction.setVirtualLanPriorityCodePoint(Integer.valueOf(
                            a.getPcp()).byteValue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionVirtualLanPriorityCodePoint.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.POP_VLAN) {
                    OFActionStripVirtualLan ofAction = new OFActionStripVirtualLan();
                    actionsList.add(ofAction);
                    actionsLength += OFActionStripVirtualLan.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_DL_SRC) {
                    SetDlSrc a = (SetDlSrc) action;
                    OFActionDataLayerSource ofAction = new OFActionDataLayerSource();
                    ofAction.setDataLayerAddress(a.getDlAddress());
                    actionsList.add(ofAction);
                    actionsLength += OFActionDataLayer.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_DL_DST) {
                    SetDlDst a = (SetDlDst) action;
                    OFActionDataLayerDestination ofAction = new OFActionDataLayerDestination();
                    ofAction.setDataLayerAddress(a.getDlAddress());
                    actionsList.add(ofAction);
                    actionsLength += OFActionDataLayer.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_NW_SRC) {
                    SetNwSrc a = (SetNwSrc) action;
                    OFActionNetworkLayerSource ofAction = new OFActionNetworkLayerSource();
                    ofAction.setNetworkAddress(NetUtils.byteArray4ToInt(a
                            .getAddress().getAddress()));
                    actionsList.add(ofAction);
                    actionsLength += OFActionNetworkLayerAddress.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_NW_DST) {
                    SetNwDst a = (SetNwDst) action;
                    OFActionNetworkLayerDestination ofAction = new OFActionNetworkLayerDestination();
                    ofAction.setNetworkAddress(NetUtils.byteArray4ToInt(a
                            .getAddress().getAddress()));
                    actionsList.add(ofAction);
                    actionsLength += OFActionNetworkLayerAddress.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_NW_TOS) {
                    SetNwTos a = (SetNwTos) action;
                    OFActionNetworkTypeOfService ofAction = new OFActionNetworkTypeOfService();
                    ofAction.setNetworkTypeOfService(Integer.valueOf(
                            a.getNwTos()).byteValue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionNetworkTypeOfService.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_TP_SRC) {
                    SetTpSrc a = (SetTpSrc) action;
                    OFActionTransportLayerSource ofAction = new OFActionTransportLayerSource();
                    ofAction.setTransportPort(Integer.valueOf(a.getPort())
                            .shortValue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionTransportLayer.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_TP_DST) {
                    SetTpDst a = (SetTpDst) action;
                    OFActionTransportLayerDestination ofAction = new OFActionTransportLayerDestination();
                    ofAction.setTransportPort(Integer.valueOf(a.getPort())
                            .shortValue());
                    actionsList.add(ofAction);
                    actionsLength += OFActionTransportLayer.MINIMUM_LENGTH;
                    continue;
                }
                if (action.getType() == ActionType.SET_NEXT_HOP) {
                    logger.warn("Unsupported action: {}", action);
                    continue;
                }
            }
        }
        logger.trace("SAL Actions: {} Openflow Actions: {}", flow.getActions(),
                actionsList);
        return actionsList;
    }

    /**
     * Utility to convert a SAL flow to an OF 1.0 (OFFlowMod) or to an OF 1.0 +
     * IPv6 extension (V6FlowMod) Flow modifier Message
     *
     * @param sw
     * @param command
     * @param port
     * @return
     */
    public OFMessage getOFFlowMod(short command, OFPort port) {
        OFMessage fm = (isIPv6) ? new V6FlowMod() : new OFFlowMod();
        if (this.ofMatch == null) {
            getOFMatch();
        }
        if (this.actionsList == null) {
            getOFActions();
        }
        if (!isIPv6) {
            ((OFFlowMod) fm).setMatch(this.ofMatch);
            ((OFFlowMod) fm).setActions(this.actionsList);
            ((OFFlowMod) fm).setPriority(flow.getPriority());
            ((OFFlowMod) fm).setCookie(flow.getId());
            ((OFFlowMod) fm).setBufferId(OFPacketOut.BUFFER_ID_NONE);
            ((OFFlowMod) fm).setLength(U16.t(OFFlowMod.MINIMUM_LENGTH
                    + actionsLength));
            ((OFFlowMod) fm).setIdleTimeout(flow.getIdleTimeout());
            ((OFFlowMod) fm).setHardTimeout(flow.getHardTimeout());
            ((OFFlowMod) fm).setCommand(command);
            if (port != null) {
                ((OFFlowMod) fm).setOutPort(port);
            }
            if (command == OFFlowMod.OFPFC_ADD || command == OFFlowMod.OFPFC_MODIFY
                    || command == OFFlowMod.OFPFC_MODIFY_STRICT) {
                // Instruct switch to let controller know when flow is removed
                ((OFFlowMod) fm).setFlags((short) 1);
            }
        } else {
            ((V6FlowMod) fm).setVendor();
            ((V6FlowMod) fm).setMatch((V6Match) ofMatch);
            ((V6FlowMod) fm).setActions(this.actionsList);
            ((V6FlowMod) fm).setPriority(flow.getPriority());
            ((V6FlowMod) fm).setCookie(flow.getId());
            ((V6FlowMod) fm).setLength(U16.t(OFVendor.MINIMUM_LENGTH
                    + ((V6Match) ofMatch).getIPv6ExtMinHdrLen()
                    + ((V6Match) ofMatch).getIPv6MatchLen()
                    + ((V6Match) ofMatch).getPadSize() + actionsLength));
            ((V6FlowMod) fm).setIdleTimeout(flow.getIdleTimeout());
            ((V6FlowMod) fm).setHardTimeout(flow.getHardTimeout());
            ((V6FlowMod) fm).setCommand(command);
            if (port != null) {
                ((V6FlowMod) fm).setOutPort(port);
            }
            if (command == OFFlowMod.OFPFC_ADD || command == OFFlowMod.OFPFC_MODIFY
                    || command == OFFlowMod.OFPFC_MODIFY_STRICT) {
                // Instruct switch to let controller know when flow is removed
                ((V6FlowMod) fm).setFlags((short) 1);
            }
        }
        logger.trace("Openflow Match: {} Openflow Actions: {}", ofMatch,
                actionsList);
        logger.trace("Openflow Mod Message: {}", fm);
        return fm;
    }

    public Flow getFlow(Node node) {
        if (this.flow == null) {
            Match salMatch = new Match();

            /*
             * Installed flow may not have a Match defined like in case of a
             * drop all flow
             */
            if (ofMatch != null) {
                if (!isIPv6) {
                    // Compute OF1.0 Match
                    if (ofMatch.getInputPort() != 0 && ofMatch.getInputPort() != OFPort.OFPP_LOCAL.getValue()) {
                        salMatch.setField(new MatchField(MatchType.IN_PORT,
                                NodeConnectorCreator.createNodeConnector(
                                        ofMatch.getInputPort(), node)));
                    }
                    if (ofMatch.getDataLayerSource() != null
                            && !NetUtils
                                    .isZeroMAC(ofMatch.getDataLayerSource())) {
                        byte srcMac[] = ofMatch.getDataLayerSource();
                        salMatch.setField(new MatchField(MatchType.DL_SRC,
                                srcMac.clone()));
                    }
                    if (ofMatch.getDataLayerDestination() != null
                            && !NetUtils.isZeroMAC(ofMatch
                                    .getDataLayerDestination())) {
                        byte dstMac[] = ofMatch.getDataLayerDestination();
                        salMatch.setField(new MatchField(MatchType.DL_DST,
                                dstMac.clone()));
                    }
                    if (ofMatch.getDataLayerType() != 0) {
                        salMatch.setField(new MatchField(MatchType.DL_TYPE,
                                ofMatch.getDataLayerType()));
                    }
                    short vlan = ofMatch.getDataLayerVirtualLan();
                    if (vlan != 0) {
                        if (vlan == OFP_VLAN_NONE) {
                            vlan = MatchType.DL_VLAN_NONE;
                        }
                        salMatch.setField(new MatchField(MatchType.DL_VLAN,
                                vlan));
                    }
                    if ((ofMatch.getWildcards() & OFMatch.OFPFW_DL_VLAN_PCP) == 0) {
                        salMatch.setField(MatchType.DL_VLAN_PR, ofMatch
                                .getDataLayerVirtualLanPriorityCodePoint());
                    }
                    if (ofMatch.getNetworkSource() != 0) {
                        salMatch.setField(MatchType.NW_SRC, NetUtils
                                .getInetAddress(ofMatch.getNetworkSource()),
                                NetUtils.getInetNetworkMask(
                                        ofMatch.getNetworkSourceMaskLen(),
                                        false));
                    }
                    if (ofMatch.getNetworkDestination() != 0) {
                        salMatch.setField(MatchType.NW_DST,
                                NetUtils.getInetAddress(ofMatch
                                        .getNetworkDestination()),
                                NetUtils.getInetNetworkMask(
                                        ofMatch.getNetworkDestinationMaskLen(),
                                        false));
                    }
                    if (ofMatch.getNetworkTypeOfService() != 0) {
                        int dscp = NetUtils.getUnsignedByte(ofMatch
                                .getNetworkTypeOfService());
                        byte tos = (byte) (dscp >> 2);
                        salMatch.setField(MatchType.NW_TOS, tos);
                    }
                    //TODO: NW protocol 0 is a valid protocol
                    if (ofMatch.getNetworkProtocol() != 0) {
                        salMatch.setField(MatchType.NW_PROTO,
                                ofMatch.getNetworkProtocol());
                    }
                    if (ofMatch.getTransportSource() != 0) {
                        salMatch.setField(MatchType.TP_SRC,
                                ofMatch.getTransportSource());
                    }
                    if (ofMatch.getTransportDestination() != 0) {
                        salMatch.setField(MatchType.TP_DST,
                                ofMatch.getTransportDestination());
                    }
                } else {
                    // Compute OF1.0 + IPv6 extensions Match
                    V6Match v6Match = (V6Match) ofMatch;
                    if (v6Match.getInputPort() != 0 && v6Match.getInputPort() != OFPort.OFPP_LOCAL.getValue()) {
                        // Mask on input port is not defined
                        salMatch.setField(new MatchField(MatchType.IN_PORT,
                                NodeConnectorCreator.createOFNodeConnector(
                                        v6Match.getInputPort(), node)));
                    }
                    if (v6Match.getDataLayerSource() != null
                            && !NetUtils
                                    .isZeroMAC(ofMatch.getDataLayerSource())) {
                        byte srcMac[] = v6Match.getDataLayerSource();
                        salMatch.setField(new MatchField(MatchType.DL_SRC,
                                srcMac.clone()));
                    }
                    if (v6Match.getDataLayerDestination() != null
                            && !NetUtils.isZeroMAC(ofMatch
                                    .getDataLayerDestination())) {
                        byte dstMac[] = v6Match.getDataLayerDestination();
                        salMatch.setField(new MatchField(MatchType.DL_DST,
                                dstMac.clone()));
                    }
                    if (v6Match.getDataLayerType() != 0) {
                        salMatch.setField(new MatchField(MatchType.DL_TYPE,
                                v6Match.getDataLayerType()));
                    }
                    short vlan = v6Match.getDataLayerVirtualLan();
                    if (vlan != 0) {
                        if (vlan == OFP_VLAN_NONE) {
                            vlan = MatchType.DL_VLAN_NONE;
                        }
                        salMatch.setField(new MatchField(MatchType.DL_VLAN,
                                vlan));
                    }
                    if ((v6Match.getWildcards() & OFMatch.OFPFW_DL_VLAN_PCP) == 0) {
                        salMatch.setField(MatchType.DL_VLAN_PR, v6Match
                                .getDataLayerVirtualLanPriorityCodePoint());
                    }
                    // V6Match may carry IPv4 address
                    if (v6Match.getNetworkSrc() != null) {
                        salMatch.setField(MatchType.NW_SRC,
                                v6Match.getNetworkSrc(),
                                v6Match.getNetworkSourceMask());
                    } else if (v6Match.getNetworkSource() != 0) {
                        salMatch.setField(MatchType.NW_SRC, NetUtils
                                .getInetAddress(v6Match.getNetworkSource()),
                                NetUtils.getInetNetworkMask(
                                        v6Match.getNetworkSourceMaskLen(),
                                        false));
                    }
                    // V6Match may carry IPv4 address
                    if (v6Match.getNetworkDest() != null) {
                        salMatch.setField(MatchType.NW_DST,
                                v6Match.getNetworkDest(),
                                v6Match.getNetworkDestinationMask());
                    } else if (v6Match.getNetworkDestination() != 0) {
                        salMatch.setField(MatchType.NW_DST,
                                NetUtils.getInetAddress(v6Match
                                        .getNetworkDestination()),
                                NetUtils.getInetNetworkMask(
                                        v6Match.getNetworkDestinationMaskLen(),
                                        false));
                    }
                    if (v6Match.getNetworkTypeOfService() != 0) {
                        int dscp = NetUtils.getUnsignedByte(v6Match
                                .getNetworkTypeOfService());
                        byte tos = (byte) (dscp >> 2);
                        salMatch.setField(MatchType.NW_TOS, tos);
                    }
                    if (v6Match.getNetworkProtocol() != 0) {
                        salMatch.setField(MatchType.NW_PROTO,
                                v6Match.getNetworkProtocol());
                    }
                    if (v6Match.getTransportSource() != 0) {
                        salMatch.setField(MatchType.TP_SRC,
                                (v6Match.getTransportSource()));
                    }
                    if (v6Match.getTransportDestination() != 0) {
                        salMatch.setField(MatchType.TP_DST,
                                (v6Match.getTransportDestination()));
                    }
                }
            }

            // Convert actions
            Action salAction = null;
            List<Action> salActionList = new ArrayList<Action>();
            if (actionsList == null) {
                salActionList.add(new Drop());
            } else {
                for (OFAction ofAction : actionsList) {
                    if (ofAction instanceof OFActionOutput) {
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
                                    NodeConnectorCreator.createOFNodeConnector(
                                            ofPort, node));
                        }
                    } else if (ofAction instanceof OFActionEnqueue) {
                        salAction = new Enqueue(NodeConnectorCreator.createOFNodeConnector(
                                ((OFActionEnqueue) ofAction).getPort(), node),
                                ((OFActionEnqueue) ofAction).getQueueId());
                    } else if (ofAction instanceof OFActionVirtualLanIdentifier) {
                        salAction = new SetVlanId(
                                ((OFActionVirtualLanIdentifier) ofAction)
                                        .getVirtualLanIdentifier());
                    } else if (ofAction instanceof OFActionStripVirtualLan) {
                        salAction = new PopVlan();
                    } else if (ofAction instanceof OFActionVirtualLanPriorityCodePoint) {
                        salAction = new SetVlanPcp(
                                ((OFActionVirtualLanPriorityCodePoint) ofAction)
                                        .getVirtualLanPriorityCodePoint());
                    } else if (ofAction instanceof OFActionDataLayerSource) {
                        salAction = new SetDlSrc(
                                ((OFActionDataLayerSource) ofAction)
                                        .getDataLayerAddress().clone());
                    } else if (ofAction instanceof OFActionDataLayerDestination) {
                        salAction = new SetDlDst(
                                ((OFActionDataLayerDestination) ofAction)
                                        .getDataLayerAddress().clone());
                    } else if (ofAction instanceof OFActionNetworkLayerSource) {
                        byte addr[] = BigInteger.valueOf(
                                ((OFActionNetworkLayerSource) ofAction)
                                        .getNetworkAddress()).toByteArray();
                        InetAddress ip = null;
                        try {
                            ip = InetAddress.getByAddress(addr);
                        } catch (UnknownHostException e) {
                            logger.error("", e);
                        }
                        salAction = new SetNwSrc(ip);
                    } else if (ofAction instanceof OFActionNetworkLayerDestination) {
                        byte addr[] = BigInteger.valueOf(
                                ((OFActionNetworkLayerDestination) ofAction)
                                        .getNetworkAddress()).toByteArray();
                        InetAddress ip = null;
                        try {
                            ip = InetAddress.getByAddress(addr);
                        } catch (UnknownHostException e) {
                            logger.error("", e);
                        }
                        salAction = new SetNwDst(ip);
                    } else if (ofAction instanceof OFActionNetworkTypeOfService) {
                        salAction = new SetNwTos(
                                ((OFActionNetworkTypeOfService) ofAction)
                                        .getNetworkTypeOfService());
                    } else if (ofAction instanceof OFActionTransportLayerSource) {
                        Short port = ((OFActionTransportLayerSource) ofAction)
                                .getTransportPort();
                        int intPort = NetUtils.getUnsignedShort(port);
                        salAction = new SetTpSrc(intPort);
                    } else if (ofAction instanceof OFActionTransportLayerDestination) {
                        Short port = ((OFActionTransportLayerDestination) ofAction)
                                .getTransportPort();
                        int intPort = NetUtils.getUnsignedShort(port);
                        salAction = new SetTpDst(intPort);
                    }
                    salActionList.add(salAction);
                }
            }
            // Create Flow
            flow = new Flow(salMatch, salActionList);
        }
        logger.trace("Openflow Match: {} Openflow Actions: {}", ofMatch,
                actionsList);
        logger.trace("SAL Flow: {}", flow);
        return flow;
    }

    private static final Map<Integer, Class<? extends Action>> actionMap = new HashMap<Integer, Class<? extends Action>>() {
        private static final long serialVersionUID = 1L;
        {
            put(1 << 0, Output.class);
            put(1 << 1, SetVlanId.class);
            put(1 << 2, SetVlanPcp.class);
            put(1 << 3, PopVlan.class);
            put(1 << 4, SetDlSrc.class);
            put(1 << 5, SetDlDst.class);
            put(1 << 6, SetNwSrc.class);
            put(1 << 7, SetNwDst.class);
            put(1 << 8, SetNwTos.class);
            put(1 << 9, SetTpSrc.class);
            put(1 << 10, SetTpDst.class);
            put(1 << 11, Enqueue.class);
        }
    };

    /**
     * Returns the supported flow actions for the netwrok node given the bitmask
     * representing the actions the Openflow 1.0 switch supports
     *
     * @param ofActionBitmask
     *            OF 1.0 action bitmask
     * @return The correspondent list of SAL Action classes
     */
    public static List<Class<? extends Action>> getFlowActions(int ofActionBitmask) {
        List<Class<? extends Action>> list = new ArrayList<Class<? extends Action>>();

        for (int i = 0; i < Integer.SIZE; i++) {
            int index = 1 << i;
            if ((index & ofActionBitmask) > 0) {
                if (actionMap.containsKey(index)) {
                    list.add(actionMap.get(index));
                }
            }
        }
        // Add implicit SAL actions
        list.add(Controller.class);
        list.add(SwPath.class);
        list.add(HwPath.class);
        list.add(Drop.class);
        list.add(Flood.class);
        list.add(FloodAll.class);

        return list;
    }

}
