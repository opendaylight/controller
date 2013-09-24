package org.opendaylight.controller.sal.compability;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.sal.action.*;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.VlanCfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.address.address.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.MacAddressFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.ethernet.match.fields.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._4.match.SctpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.layer._4.match.UdpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.vlan.match.fields.VlanId;

import com.google.common.net.InetAddresses;

public class ToSalConversionsUtils {

    private ToSalConversionsUtils() {

    }

    public static Flow flowFrom(NodeFlow source) {
        final Flow target = new Flow();

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

        target.setMatch(matchFrom(source.getMatch()));

        List<Action> actions = source.getAction();
        if (actions != null) {
            for (Action sourceAction : actions) {
                Set<org.opendaylight.controller.sal.action.Action> targetActions = actionFrom(sourceAction);
                for (org.opendaylight.controller.sal.action.Action targetAction : targetActions) {
                    target.addAction(targetAction);
                }
            }
        }

        target.setId(source.getCookie().longValue());
        return target;
    }

    public static Set<org.opendaylight.controller.sal.action.Action> actionFrom(Action source) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.Action sourceAction = source
                .getAction();
        Set<org.opendaylight.controller.sal.action.Action> targetAction = new HashSet<>();
        if (sourceAction instanceof ControllerAction) {
            targetAction.add(new Controller());
        } else if (sourceAction instanceof OutputAction) {

            List<Uri> nodeConnectors = ((OutputAction) sourceAction).getOutputNodeConnector();
            for (Uri uri : nodeConnectors) {
                targetAction.add(new Output(fromNodeConnectorRef(uri)));
            }
        } else if (sourceAction instanceof PopMplsAction) {
            // TODO: define maping
        } else if (sourceAction instanceof PushMplsAction) {
            // TODO: define maping
        } else if (sourceAction instanceof PushPbbAction) {
            // TODO: define maping
        } else if (sourceAction instanceof PushVlanAction) {
            // TODO: define maping
            // PushVlanAction vlanAction = (PushVlanAction) sourceAction;
            // targetAction.add(new PushVlan(vlanAction., pcp, cfi, vlanId);
        } else if (sourceAction instanceof SetMplsTtlAction) {
            // TODO: define maping
            // targetAction = //no action to map
        } else if (sourceAction instanceof SetNwTtlAction) {
            // TODO: define maping
        } else if (sourceAction instanceof SetQueueAction) {
            // TODO: define maping
            // targetAction = //no action to map
        } else if (sourceAction instanceof DropAction) {
            targetAction.add(new Drop());
        } else if (sourceAction instanceof FloodAction) {
            targetAction.add(new Flood());
        } else if (sourceAction instanceof FloodAllAction) {
            targetAction.add(new FloodAll());
        } else if (sourceAction instanceof HwPathAction) {
            targetAction.add(new HwPath());
        } else if (sourceAction instanceof LoopbackAction) {
            targetAction.add(new Loopback());
        } else if (sourceAction instanceof PopVlanAction) {
            targetAction.add(new PopVlan());
        } else if (sourceAction instanceof PushVlanAction) {
            PushVlanAction pushVlanAction = (PushVlanAction) sourceAction;
            PushVlan pushVlan = pushVlanFrom(pushVlanAction);
            if (pushVlan != null) {
                targetAction.add(pushVlan);
            }
        } else if (sourceAction instanceof SetDlDstAction) {
            MacAddress addressL2Dest = ((SetDlDstAction) sourceAction).getAddress();
            if (addressL2Dest != null) {
                String addressValue = addressL2Dest.getValue();
                if (addressValue != null) {
                    targetAction.add(new SetDlDst(addressValue.getBytes()));
                }
            }
        } else if (sourceAction instanceof SetDlSrcAction) {
            MacAddress addressL2Src = ((SetDlSrcAction) sourceAction).getAddress();
            if (addressL2Src != null) {
                String addressValue = addressL2Src.getValue();
                if (addressValue != null) {
                    targetAction.add(new SetDlSrc(addressValue.getBytes()));
                }
            }
        } else if (sourceAction instanceof SetDlTypeAction) {
            EtherType dlType = ((SetDlTypeAction) sourceAction).getDlType();
            if (dlType != null) {
                Long dlTypeValue = dlType.getValue();
                if (dlTypeValue != null) {
                    targetAction.add(new SetDlType(dlTypeValue.intValue()));
                }
            }
        } else if (sourceAction instanceof SetNextHopAction) {
            Address addressL3 = ((SetNextHopAction) sourceAction).getAddress();

            InetAddress inetAddress = inetAddressFrom(addressL3);
            if (inetAddress != null) {
                targetAction.add(new SetNextHop(inetAddress));
            }
        } else if (sourceAction instanceof SetNwDstAction) {
            Address addressL3 = ((SetNwDstAction) sourceAction).getAddress();

            InetAddress inetAddress = inetAddressFrom(addressL3);
            if (inetAddress != null) {
                targetAction.add(new SetNwDst(inetAddress));
            }
        } else if (sourceAction instanceof SetNwSrcAction) {
            Address addressL3 = ((SetNwDstAction) sourceAction).getAddress();

            InetAddress inetAddress = inetAddressFrom(addressL3);
            if (inetAddress != null) {
                targetAction.add(new SetNwSrc(inetAddress));
            }
        } else if (sourceAction instanceof SetNwTosAction) {
            Integer tos = ((SetNwTosAction) sourceAction).getTos();
            if (tos != null) {
                targetAction.add(new SetNwTos(tos));
            }
        } else if (sourceAction instanceof SetTpDstAction) {
            PortNumber port = ((SetTpDstAction) sourceAction).getPort();
            if (port != null) {
                Integer portValue = port.getValue();
                if (port.getValue() != null) {
                    targetAction.add(new SetTpDst(portValue));
                }
            }
        } else if (sourceAction instanceof SetTpSrcAction) {
            PortNumber port = ((SetTpSrcAction) sourceAction).getPort();
            if (port != null) {
                Integer portValue = port.getValue();
                if (port.getValue() != null) {
                    targetAction.add(new SetTpSrc(portValue));
                }
            }
        } else if (sourceAction instanceof SetVlanCfiAction) {
            VlanCfi vlanCfi = ((SetVlanCfiAction) sourceAction).getVlanCfi();
            if (vlanCfi != null) {
                Integer vlanCfiValue = vlanCfi.getValue();
                if (vlanCfiValue != null) {
                    targetAction.add(new SetVlanCfi(vlanCfiValue));
                }
            }
        } else if (sourceAction instanceof SetVlanIdAction) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId vlanID = ((SetVlanIdAction) sourceAction)
                    .getVlanId();
            if (vlanID != null) {
                Integer vlanIdValue = vlanID.getValue();
                if (vlanIdValue != null) {
                    targetAction.add(new SetVlanId(vlanIdValue));
                }
            }
        } else if (sourceAction instanceof SetVlanPcpAction) {
            VlanPcp vlanPcp = ((SetVlanPcpAction) sourceAction).getVlanPcp();
            if (vlanPcp != null) {
                Short vlanPcpValue = vlanPcp.getValue();
                if (vlanPcpValue != null) {
                    targetAction.add(new SetVlanPcp(vlanPcpValue));
                }
            }
        } else if (sourceAction instanceof SwPathAction) {
            targetAction.add(new SwPath());
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

    private static PushVlan pushVlanFrom(PushVlanAction pushVlanAction) {
        final int tag;
        final int pcp;
        final int cfi;
        final int vlanId;

        if (pushVlanAction.getTag() != null) {
            tag = pushVlanAction.getTag();
            if (pushVlanAction.getPcp() != null) {
                pcp = pushVlanAction.getPcp();
                if (pushVlanAction.getCfi() != null && pushVlanAction.getCfi().getValue() != null) {
                    cfi = pushVlanAction.getCfi().getValue();
                    if (pushVlanAction.getVlanId() != null && pushVlanAction.getVlanId().getValue() != null) {
                        vlanId = pushVlanAction.getVlanId().getValue();
                        return new PushVlan(tag, pcp, cfi, vlanId);
                    }
                }
            }
        }
        return null;
    }

    private static NodeConnector fromNodeConnectorRef(Uri uri) {
        // TODO: Define mapping
        return null;
    }

    public static Match matchFrom(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.Match source) {
        Match target = new Match();
        if (source != null) {
            fillFrom(target, source.getVlanMatch());
            fillFrom(target, source.getEthernetMatch());
            fillFrom(target, source.getLayer3Match());
            fillFrom(target, source.getLayer4Match());
            fillFrom(target, source.getIpMatch());
        }

        return target;
    }

    private static void fillFrom(Match target, VlanMatch vlanMatch) {
        if (vlanMatch != null) {
            VlanId vlanId = vlanMatch.getVlanId();
            if (vlanId != null) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId vlanIdInner = vlanId
                        .getVlanId();
                if (vlanIdInner != null) {
                    Integer vlanValue = vlanIdInner.getValue();
                    if (vlanValue != null) {
                        target.setField(DL_VLAN, vlanValue.shortValue());
                    }
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
            if (ipProtocol != null) {
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
            target.setField(NW_SRC, (InetAddress) inetAddressFrom(sourceAddress), null);
        }
        Ipv4Prefix destAddress = source.getArpSourceTransportAddress();
        if (destAddress != null) {
            target.setField(NW_DST, (InetAddress) inetAddressFrom(destAddress), null);
        }
    }

    private static void fillFromIpv6(Match target, Ipv6Match source) {
        Ipv6Prefix sourceAddress = source.getIpv6Source();
        if (sourceAddress != null) {
            target.setField(NW_SRC, (InetAddress) inetAddressFrom(sourceAddress), null);
        }
        Ipv6Prefix destAddress = source.getIpv6Source();
        if (destAddress != null) {
            target.setField(NW_DST, (InetAddress) inetAddressFrom(destAddress), null);
        }
    }

    private static void fillFromIpv4(Match target, Ipv4Match source) {
        Ipv4Prefix sourceAddress = source.getIpv4Source();
        if (sourceAddress != null) {
            target.setField(NW_SRC, (InetAddress) inetAddressFrom(sourceAddress), null);
        }
        Ipv4Prefix destAddress = source.getIpv4Source();
        if (destAddress != null) {
            target.setField(NW_DST, (InetAddress) inetAddressFrom(destAddress), null);
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
            if (ethInnerType != null) {
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

    private static byte[] bytesFrom(MacAddress address) {
        if (address != null) {
            return address.getValue().getBytes();
        }
        return null;
    }
}
