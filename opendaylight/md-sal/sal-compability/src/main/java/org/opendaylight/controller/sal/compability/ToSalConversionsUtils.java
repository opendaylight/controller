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

import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.ControllerAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.PopMplsAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.PushMplsAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.PushPbbAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.PushVlanAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.SetMplsTtlAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.SetNwTtlAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.action.action.SetQueueAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.MacAddressFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.ethernet.match.fields.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev130819.match.VlanMatch;
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
        }

        return targetAction;
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
