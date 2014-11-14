/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.CRUDP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.ETHERNET_ARP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.TCP;
import static org.opendaylight.controller.sal.compatibility.ProtocolConstants.UDP;
import static org.opendaylight.controller.sal.match.MatchType.DL_DST;
import static org.opendaylight.controller.sal.match.MatchType.DL_SRC;
import static org.opendaylight.controller.sal.match.MatchType.DL_TYPE;
import static org.opendaylight.controller.sal.match.MatchType.DL_VLAN;
import static org.opendaylight.controller.sal.match.MatchType.DL_VLAN_PR;
import static org.opendaylight.controller.sal.match.MatchType.NW_DST;
import static org.opendaylight.controller.sal.match.MatchType.NW_PROTO;
import static org.opendaylight.controller.sal.match.MatchType.NW_SRC;
import static org.opendaylight.controller.sal.match.MatchType.NW_TOS;
import static org.opendaylight.controller.sal.match.MatchType.TP_DST;
import static org.opendaylight.controller.sal.match.MatchType.TP_SRC;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.FloodAll;
import org.opendaylight.controller.sal.action.HwPath;
import org.opendaylight.controller.sal.action.Loopback;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.PushVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetDlType;
import org.opendaylight.controller.sal.action.SetNextHop;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetNwTos;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SetVlanCfi;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.action.SetVlanPcp;
import org.opendaylight.controller.sal.action.SwPath;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.VlanCfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodAllActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.LoopbackActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushPbbActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetMplsTtlActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNextHopActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTtlActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetQueueActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanCfiActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SwPathActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SwitchFlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.GenericFlowAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.MacAddressFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpSourceHardwareAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpTargetHardwareAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

public class ToSalConversionsUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ToSalConversionsUtils.class);

    private ToSalConversionsUtils() {
        throw new IllegalAccessError("forcing no instance for factory");
    }

    public static Flow toFlow(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow source, Node node) {
        final Flow target = new Flow();
        genericFlowToAdFlow(source, target);

        target.setMatch(toMatch(source.getMatch()));

        List<Action> actions = getAction(source);
        if (actions != null) {
            target.setActions(actionFrom(actions, node));
        }

        return target;
    }

    /**
     * @param source notification, missing instructions
     * @param node corresponding node where the flow change occured
     * @return ad-sal node, build from given data
     */
    public static Flow toFlow(SwitchFlowRemoved source, Node node) {
        final Flow target = new Flow();
        genericFlowToAdFlow(source, target);

        target.setMatch(toMatch(source.getMatch()));

        return target;
    }

    /**
     * @param source
     * @param target
     */
    private static void genericFlowToAdFlow(GenericFlowAttributes source,
            final Flow target) {
        Integer hardTimeout = source.getHardTimeout();
        if (hardTimeout != null) {
            target.setHardTimeout(hardTimeout.shortValue());
        }

        Integer idleTimeout = source.getIdleTimeout();
        if (idleTimeout != null) {
            target.setIdleTimeout(idleTimeout.shortValue());
        }

        Integer priority = source.getPriority();
        if (priority != null) {
            target.setPriority(priority.shortValue());
        }
        target.setId(source.getCookie().getValue().longValue());
    }

    public static List<Action> getAction(
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow source) {
        if (source.getInstructions() != null) {
            for (Instruction instruction : source.getInstructions().getInstruction()) {
                if (instruction.getInstruction() instanceof ApplyActionsCase) {
                    return (((ApplyActionsCase) instruction.getInstruction()).getApplyActions().getAction());
                }
            }
        }
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

    public static List<org.opendaylight.controller.sal.action.Action> actionFrom(List<Action> actions, Node node) {
        List<org.opendaylight.controller.sal.action.Action> targetAction = new ArrayList<>();
        for (Action action : actions) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action sourceAction = action
                    .getAction();

            if (sourceAction instanceof ControllerActionCase) {
                targetAction.add(new Controller());
            } else if (sourceAction instanceof OutputActionCase) {

                Uri nodeConnector = ((OutputActionCase) sourceAction).getOutputAction().getOutputNodeConnector();
                if (nodeConnector != null) {
                    // TODO: We should really have a bi-directional map from AD-SAL node types to
                    //       MD-SAL node types, but lets fix that later.
                    String type = node.getType();
                    if( type.equals(NodeIDType.OPENFLOW) ){
                        type = NodeMapping.OPENFLOW_ID_PREFIX;
                    }else{
                        type = type + ":";
                    }
                    Uri fullNodeConnector = new Uri(type+node.getID()+":"+nodeConnector.getValue());
                        targetAction.add(new Output(fromNodeConnectorRef(fullNodeConnector, node)));
                }
            } else if (sourceAction instanceof PopMplsActionCase) {
                // TODO: define maping
            } else if (sourceAction instanceof PushMplsActionCase) {
                // TODO: define maping
            } else if (sourceAction instanceof PushPbbActionCase) {
                // TODO: define maping
            } else if (sourceAction instanceof SetMplsTtlActionCase) {
                // TODO: define maping
                // targetAction = //no action to map
            } else if (sourceAction instanceof SetNwTtlActionCase) {
                // TODO: define maping
            } else if (sourceAction instanceof SetQueueActionCase) {
                // TODO: define maping
                // targetAction = //no action to map
            } else if (sourceAction instanceof DropActionCase) {
                targetAction.add(new Drop());
            } else if (sourceAction instanceof FloodActionCase) {
                targetAction.add(new Flood());
            } else if (sourceAction instanceof FloodAllActionCase) {
                targetAction.add(new FloodAll());
            } else if (sourceAction instanceof HwPathActionCase) {
                targetAction.add(new HwPath());
            } else if (sourceAction instanceof LoopbackActionCase) {
                targetAction.add(new Loopback());
            } else if (sourceAction instanceof PopVlanActionCase) {
                targetAction.add(new PopVlan());
            } else if (sourceAction instanceof PushVlanActionCase) {
                PushVlanActionCase pushVlanAction = (PushVlanActionCase) sourceAction;
                PushVlan pushVlan = pushVlanFrom(pushVlanAction.getPushVlanAction());
                if (pushVlan != null) {
                    targetAction.add(pushVlan);
                }
            } else if (sourceAction instanceof SetDlDstActionCase) {
                MacAddress addressL2Dest = ((SetDlDstActionCase) sourceAction).getSetDlDstAction().getAddress();
                if (addressL2Dest != null) {
                    targetAction.add(new SetDlDst(bytesFrom(addressL2Dest)));
                }
            } else if (sourceAction instanceof SetDlSrcActionCase) {
                MacAddress addressL2Src = ((SetDlSrcActionCase) sourceAction).getSetDlSrcAction().getAddress();
                if (addressL2Src != null) {
                    targetAction.add(new SetDlSrc(bytesFrom(addressL2Src)));

                }
            } else if (sourceAction instanceof SetDlTypeActionCase) {
                EtherType dlType = ((SetDlTypeActionCase) sourceAction).getSetDlTypeAction().getDlType();
                if (dlType != null) {
                    Long dlTypeValue = dlType.getValue();
                    if (dlTypeValue != null) {
                        targetAction.add(new SetDlType(dlTypeValue.intValue()));
                    }
                }
            } else if (sourceAction instanceof SetNextHopActionCase) {
                Address addressL3 = ((SetNextHopActionCase) sourceAction).getSetNextHopAction().getAddress();

                InetAddress inetAddress = inetAddressFrom(addressL3);
                if (inetAddress != null) {
                    targetAction.add(new SetNextHop(inetAddress));
                }
            } else if (sourceAction instanceof SetNwDstActionCase) {
                Address addressL3 = ((SetNwDstActionCase) sourceAction).getSetNwDstAction().getAddress();

                InetAddress inetAddress = inetAddressFrom(addressL3);
                if (inetAddress != null) {
                    targetAction.add(new SetNwDst(inetAddress));
                }
            } else if (sourceAction instanceof SetNwSrcActionCase) {
                Address addressL3 = ((SetNwSrcActionCase) sourceAction).getSetNwSrcAction().getAddress();

                InetAddress inetAddress = inetAddressFrom(addressL3);
                if (inetAddress != null) {
                    targetAction.add(new SetNwSrc(inetAddress));
                }
            } else if (sourceAction instanceof SetNwTosActionCase) {
                Integer tos = ((SetNwTosActionCase) sourceAction).getSetNwTosAction().getTos();
                if (tos != null) {
                    targetAction.add(new SetNwTos(ToSalConversionsUtils.tosToNwDscp(tos)));
                }
            } else if (sourceAction instanceof SetTpDstActionCase) {
                PortNumber port = ((SetTpDstActionCase) sourceAction).getSetTpDstAction().getPort();
                if (port != null) {
                    Integer portValue = port.getValue();
                    if (port.getValue() != null) {
                        targetAction.add(new SetTpDst(portValue));
                    }
                }
            } else if (sourceAction instanceof SetTpSrcActionCase) {
                PortNumber port = ((SetTpSrcActionCase) sourceAction).getSetTpSrcAction().getPort();
                if (port != null) {
                    Integer portValue = port.getValue();
                    if (port.getValue() != null) {
                        targetAction.add(new SetTpSrc(portValue));
                    }
                }
            } else if (sourceAction instanceof SetVlanCfiActionCase) {
                VlanCfi vlanCfi = ((SetVlanCfiActionCase) sourceAction).getSetVlanCfiAction().getVlanCfi();
                if (vlanCfi != null) {
                    Integer vlanCfiValue = vlanCfi.getValue();
                    if (vlanCfiValue != null) {
                        targetAction.add(new SetVlanCfi(vlanCfiValue));
                    }
                }
            } else if (sourceAction instanceof SetVlanIdActionCase) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId vlanID = ((SetVlanIdActionCase) sourceAction).getSetVlanIdAction()
                        .getVlanId();
                if (vlanID != null) {
                    Integer vlanIdValue = vlanID.getValue();
                    if (vlanIdValue != null) {
                        targetAction.add(new SetVlanId(vlanIdValue));
                    }
                }
            } else if (sourceAction instanceof SetVlanPcpActionCase) {
                VlanPcp vlanPcp = ((SetVlanPcpActionCase) sourceAction).getSetVlanPcpAction().getVlanPcp();
                if (vlanPcp != null) {
                    Short vlanPcpValue = vlanPcp.getValue();
                    if (vlanPcpValue != null) {
                        targetAction.add(new SetVlanPcp(vlanPcpValue));
                    }
                }
            } else if (sourceAction instanceof SwPathActionCase) {
                targetAction.add(new SwPath());
            }
        }

        return targetAction;
    }

    private static InetAddress inetAddressFrom(Address addressL3) {
        if (addressL3 != null) {
            if (addressL3 instanceof Ipv4) {
                Ipv4Prefix addressL3Ipv4 = ((Ipv4) addressL3).getIpv4Address();
                if (addressL3Ipv4 != null) {
                    return inetAddressFrom(addressL3Ipv4);
                }
            } else if (addressL3 instanceof Ipv6) {
                Ipv6Prefix addressL3Ipv6 = ((Ipv6) addressL3).getIpv6Address();
                if (addressL3Ipv6 != null) {
                    return inetAddressFrom(addressL3Ipv6);
                }
            }
        }
        return null;
    }

    private static PushVlan pushVlanFrom(org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanAction pushVlanAction) {
        Integer tag = pushVlanAction.getTag();
        if (tag != null) {
            return new PushVlan(tag.intValue());
        }
        return null;
    }

    /**
     * @param openflow nodeConnector uri
     * @param node
     * @return assembled nodeConnector
     */
    public static NodeConnector fromNodeConnectorRef(Uri uri, Node node) {
        NodeConnector nodeConnector = null;
        try {
            NodeConnectorId nodeConnectorId = new NodeConnectorId(uri.getValue());
            nodeConnector = NodeMapping.toADNodeConnector(nodeConnectorId, node);
        } catch (ConstructionException e) {
            LOG.warn("nodeConnector creation failed at node: {} with nodeConnectorUri: {}",
                    node, uri.getValue());
        }
        return nodeConnector;
    }

    public static Match toMatch(org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match source) {
        Match target = new Match();
        if (source != null) {
            fillFrom(target, source.getVlanMatch());
            fillFrom(target, source.getEthernetMatch());
            fillFrom(target, source.getLayer3Match());
            fillFrom(target, source.getLayer4Match());
            fillFrom(target, source.getIpMatch());
            fillFrom(target, source.getInPort());
        }

        return target;
    }

    /**
     * @param target
     * @param inPort
     */
    private static void fillFrom(Match target, NodeConnectorId inPort) {
        if (inPort != null) {
            String inPortValue = inPort.getValue();
            if (inPortValue != null) {
                try {
                    target.setField(MatchType.IN_PORT, NodeMapping.toADNodeConnector(inPort,
                            NodeMapping.toAdNodeId(inPort)));
                } catch (ConstructionException e) {
                    LOG.warn("nodeConnector construction failed", e);
                }
            }
        }
    }

    private static void fillFrom(Match target, VlanMatch vlanMatch) {
        if (vlanMatch != null) {
            VlanId vlanId = vlanMatch.getVlanId();
            if (vlanId != null) {
                if (Boolean.TRUE.equals(vlanId.isVlanIdPresent())) {
                    org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId vlanIdInner = vlanId
                            .getVlanId();
                    if (vlanIdInner != null) {
                        Integer vlanValue = vlanIdInner.getValue();
                        if (vlanValue != null) {
                            target.setField(DL_VLAN, vlanValue.shortValue());
                        }
                    }
                } else {
                    target.setField(DL_VLAN, MatchType.DL_VLAN_NONE);
                }
            }
            VlanPcp vlanPcp = vlanMatch.getVlanPcp();
            if (vlanPcp != null) {
                Short vlanPcpValue = vlanPcp.getValue();
                if (vlanPcpValue != null) {
                    target.setField(DL_VLAN_PR, vlanPcpValue.byteValue());
                }
            }
        }
    }

    private static void fillFrom(Match target, IpMatch ipMatch) {
        if (ipMatch != null) {
            Short ipProtocol = ipMatch.getIpProtocol();

            if (ipProtocol != null && target.getField(NW_PROTO) == null) {
                target.setField(NW_PROTO, ipProtocol.byteValue());
            }
            Dscp dscp = ipMatch.getIpDscp();
            if (dscp != null) {
                Short dscpValue = dscp.getValue();
                if (dscpValue != null) {
                    target.setField(NW_TOS, dscpValue.byteValue());
                }
            }
        }
    }

    private static void fillFrom(Match target, Layer4Match layer4Match) {
        if (layer4Match == null) {
            return;
        }
        if (layer4Match instanceof SctpMatch) {
            fillTransportLayer(target, (SctpMatch) layer4Match);
        } else if (layer4Match instanceof TcpMatch) {
            fillTransportLayer(target, (TcpMatch) layer4Match);
        } else if (layer4Match instanceof UdpMatch) {
            fillTransportLayer(target, (UdpMatch) layer4Match);
        }
    }

    private static void fillTransportLayer(Match target, UdpMatch source) {
        PortNumber udpSourcePort = source.getUdpSourcePort();
        if (udpSourcePort != null) {
            Integer udpSourcePortValue = udpSourcePort.getValue();
            if (udpSourcePortValue != null) {
                target.setField(TP_SRC, udpSourcePortValue.shortValue());
            }
        }

        PortNumber udpDestPort = source.getUdpDestinationPort();
        if (udpDestPort != null) {
            Integer udpDestPortValue = udpDestPort.getValue();
            if (udpDestPortValue != null) {
                target.setField(TP_DST, udpDestPortValue.shortValue());
            }
        }

        target.setField(NW_PROTO, UDP);
    }

    private static void fillTransportLayer(Match target, TcpMatch source) {
        PortNumber tcpSourcePort = source.getTcpSourcePort();
        if (tcpSourcePort != null) {
            Integer tcpSourcePortValue = tcpSourcePort.getValue();
            if (tcpSourcePortValue != null) {
                target.setField(TP_SRC, tcpSourcePortValue.shortValue());
            }
        }

        PortNumber tcpDestPort = source.getTcpDestinationPort();
        if (tcpDestPort != null) {
            Integer tcpDestPortValue = tcpDestPort.getValue();
            if (tcpDestPortValue != null) {
                target.setField(TP_DST, tcpDestPortValue.shortValue());
            }
        }

        target.setField(NW_PROTO, TCP);
    }

    private static void fillTransportLayer(Match target, SctpMatch source) {
        PortNumber sctpSourcePort = source.getSctpSourcePort();
        if (sctpSourcePort != null) {
            Integer sctpSourcePortValue = sctpSourcePort.getValue();
            if (sctpSourcePortValue != null) {
                target.setField(TP_SRC, sctpSourcePortValue.shortValue());
            }
        }
        PortNumber sctpDestPort = source.getSctpDestinationPort();
        if (sctpDestPort != null) {
            Integer sctpDestPortValue = sctpDestPort.getValue();
            if (sctpDestPortValue != null) {
                target.setField(TP_DST, sctpDestPortValue.shortValue());
            }
        }

        target.setField(NW_PROTO, CRUDP);

    }

    private static void fillFrom(Match target, Layer3Match source) {
        if (source == null)
            return;
        if (source instanceof Ipv4Match) {
            fillFromIpv4(target, (Ipv4Match) source);
        } else if (source instanceof Ipv6Match) {
            fillFromIpv6(target, (Ipv6Match) source);
        } else if (source instanceof ArpMatch) {
            fillFromArp(target, (ArpMatch) source);
        }
    }

    private static void fillFromArp(Match target, ArpMatch source) {
        Ipv4Prefix sourceAddress = source.getArpSourceTransportAddress();
        if (sourceAddress != null) {
            target.setField(NW_SRC, inetAddressFrom(sourceAddress), null);
        }
        Ipv4Prefix destAddress = source.getArpTargetTransportAddress();
        if (destAddress != null) {
            target.setField(NW_DST, inetAddressFrom(destAddress), null);
        }
        ArpSourceHardwareAddress sourceHwAddress = source.getArpSourceHardwareAddress();
        if (sourceHwAddress != null) {
            target.setField(DL_SRC, bytesFrom(sourceHwAddress.getAddress()));
        }
        ArpTargetHardwareAddress targetHwAddress = source.getArpTargetHardwareAddress();
        if (targetHwAddress != null) {
            target.setField(DL_DST, bytesFrom(targetHwAddress.getAddress()));
        }

        target.setField(DL_TYPE, new Short(ETHERNET_ARP));

    }

    private static void fillFromIpv6(Match target, Ipv6Match source) {
        Ipv6Prefix sourceAddress = source.getIpv6Source();
        if (sourceAddress != null) {
            target.setField(NW_SRC, inetAddressFrom(sourceAddress), null);
        }
        Ipv6Prefix destAddress = source.getIpv6Destination();
        if (destAddress != null) {
            target.setField(NW_DST, inetAddressFrom(destAddress), null);
        }
    }

    private static void fillFromIpv4(Match target, Ipv4Match source) {
        Ipv4Prefix sourceAddress = source.getIpv4Source();
        if (sourceAddress != null) {
            target.setField(NW_SRC, inetAddressFrom(sourceAddress), null);
        }
        Ipv4Prefix destAddress = source.getIpv4Destination();
        if (destAddress != null) {
            target.setField(NW_DST, inetAddressFrom(destAddress), null);
        }
    }

    private static InetAddress inetAddressFrom(Ipv4Prefix source) {
        if (source != null) {
            String[] parts = source.getValue().split("/");
            return InetAddresses.forString(parts[0]);
        }
        return null;
    }

    private static InetAddress inetAddressFrom(Ipv6Prefix source) {
        if (source != null) {
            String[] parts = source.getValue().split("/");
            return InetAddresses.forString(parts[0]);
        }
        return null;
    }

    private static void fillFrom(Match target, EthernetMatch source) {
        if (source == null)
            return;
        EthernetType ethType = source.getEthernetType();
        if (ethType != null) {
            EtherType ethInnerType = ethType.getType();
            if (ethInnerType != null && target.getField(DL_TYPE) == null) {
                Long value = ethInnerType.getValue();
                target.setField(DL_TYPE, value.shortValue());
            }
        }

        MacAddressFilter ethSource = source.getEthernetSource();
        if (ethSource != null) {
            target.setField(DL_SRC, bytesFrom(ethSource.getAddress()));
        }

        MacAddressFilter ethDest = source.getEthernetDestination();
        if (ethDest != null) {
            target.setField(DL_DST, bytesFrom(ethDest.getAddress()));
        }
    }

    public static byte[] bytesFrom(MacAddress address) {
        String[] mac = address.getValue().split(":");
        byte[] macAddress = new byte[6]; // mac.length == 6 bytes
        for (int i = 0; i < mac.length; i++) {
            macAddress[i] = Integer.decode("0x" + mac[i]).byteValue();
        }
        return macAddress;
    }

    public static byte[] bytesFromDpid(long dpid) {
        byte[] mac = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        for (short i = 0; i < 6; i++) {
            mac[5 - i] = (byte) dpid;
            dpid >>= 8;
        }

        return mac;
    }

    /**
     * @param nwTos NW-TOS
     * @return shifted to NW-DSCP
     */
    public static int tosToNwDscp(int nwTos) {
        return (short) (nwTos >>> FromSalConversionsUtils.ENC_FIELD_BIT_SIZE);
    }
}
