package org.opendaylight.controller.forwardingrulesmanager.consumer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushPbbAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetQueueAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ClearActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FRMUtil {
    protected static final Logger logger = LoggerFactory.getLogger(FRMUtil.class);
    private static final String NAMEREGEX = "^[a-zA-Z0-9]+$";

    public static enum operation {
        ADD, DELETE, UPDATE, GET
    };

    private enum EtherIPType {
        ANY, V4, V6;
    };

    public static boolean isNameValid(String name) {

        // Name validation
        if (name == null || name.trim().isEmpty() || !name.matches(NAMEREGEX)) {
            return false;
        }
        return true;

    }

    public static boolean validateMatch(Flow flow) {

        EtherIPType etype = EtherIPType.ANY;
        EtherIPType ipsrctype = EtherIPType.ANY;
        EtherIPType ipdsttype = EtherIPType.ANY;

        Match match = flow.getMatch();
        if (match != null) {
            EthernetMatch ethernetmatch = match.getEthernetMatch();
            IpMatch ipmatch = match.getIpMatch();
            Layer3Match layer3match = match.getLayer3Match();
            VlanMatch vlanmatch = match.getVlanMatch();
            match.getIcmpv4Match();

            if (ethernetmatch != null) {
                if ((ethernetmatch.getEthernetSource() != null)
                        && !isL2AddressValid(ethernetmatch.getEthernetSource().toString())) {

                    logger.error("Ethernet source address %s is not valid. Example: 00:05:b9:7c:81:5f",
                            ethernetmatch.getEthernetSource());
                    return false;
                }

                if ((ethernetmatch.getEthernetDestination() != null)
                        && !isL2AddressValid(ethernetmatch.getEthernetDestination().toString())) {
                    logger.error("Ethernet destination address %s is not valid. Example: 00:05:b9:7c:81:5f",
                            ethernetmatch.getEthernetDestination());
                    return false;
                }

                if (ethernetmatch.getEthernetType() != null) {
                    int type = Integer.decode(ethernetmatch.getEthernetType().toString());
                    if ((type < 0) || (type > 0xffff)) {
                        logger.error("Ethernet type is not valid");
                        return false;
                    } else {
                        if (type == 0x800) {
                            etype = EtherIPType.V4;
                        } else if (type == 0x86dd) {
                            etype = EtherIPType.V6;
                        }
                    }

                }
            } else if (layer3match != null) {
                if (layer3match instanceof Ipv4Match) {
                    if (((Ipv4Match) layer3match).getIpv4Source().getValue() != null
                            && NetUtils.isIPv4AddressValid(((Ipv4Match) layer3match).getIpv4Source().getValue())) {
                        ipsrctype = EtherIPType.V4;
                    } else if (((Ipv4Match) layer3match).getIpv4Destination().getValue() != null
                            && NetUtils.isIPv4AddressValid(((Ipv4Match) layer3match).getIpv4Destination().getValue())) {
                        ipdsttype = EtherIPType.V4;
                    }
                } else if (layer3match instanceof Ipv6Match) {
                    if (((Ipv6Match) layer3match).getIpv6Source().getValue() != null
                            && NetUtils.isIPv6AddressValid(((Ipv6Match) layer3match).getIpv6Source().getValue())) {
                        ipsrctype = EtherIPType.V6;
                    } else if (((Ipv6Match) layer3match).getIpv6Destination().getValue() != null
                            && NetUtils.isIPv6AddressValid(((Ipv6Match) layer3match).getIpv6Destination().getValue())) {
                        ipdsttype = EtherIPType.V6;
                    }

                } else {
                    logger.error("IP source address %s is not valid");
                    return false;
                }
            } else if (ipmatch != null) {
                if (ipmatch.getIpProtocol() != null && !(isProtocolValid(ipmatch.getIpProtocol().toString()))) {
                    logger.error("Protocol is not valid");
                    return false;
                }

            } else if (vlanmatch != null) {
                if (vlanmatch.getVlanId() != null && !(isVlanIdValid(vlanmatch.getVlanId().toString()))) {
                    logger.error("Vlan ID is not in the range 0 - 4095");
                    return false;
                }

                if (vlanmatch.getVlanPcp() != null && !(isVlanPriorityValid(vlanmatch.getVlanPcp().toString()))) {
                    logger.error("Vlan priority is not in the range 0 - 7");
                    return false;
                }
            }
            if (etype != EtherIPType.ANY) {
                if ((ipsrctype != EtherIPType.ANY) && (ipsrctype != etype)) {
                    logger.error("Type mismatch between Ethernet & Src IP");
                    return false;
                }
                if ((ipdsttype != EtherIPType.ANY) && (ipdsttype != etype)) {
                    logger.error("Type mismatch between Ethernet & Dst IP");
                    return false;
                }
            }
            if (ipsrctype != ipdsttype) {
                if (!((ipsrctype == EtherIPType.ANY) || (ipdsttype == EtherIPType.ANY))) {
                    logger.error("IP Src Dest Type mismatch");
                    return false;
                }
            }

        }

        return true;
    }

    public static boolean validateActions(List<Action> actions) {

        if (actions == null || actions.isEmpty()) {
            logger.error("Actions value is null or empty");
            return false;
        }

        for (Action curaction : actions) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action action = curaction
                    .getAction();
            if (action instanceof ControllerAction) {
                Integer length = ((ControllerAction) action).getMaxLength();
                if (length < 0 || length > 65294) {
                    logger.error("Controller: MaxLength is not valid");
                    return false;
                }
            } else if (action instanceof OutputAction) {
                Integer length = ((OutputAction) action).getMaxLength();
                Uri outputnodeconnector = ((OutputAction) action).getOutputNodeConnector();
                if (length < 0 || length > 65294) {
                    logger.error("OutputAction: MaxLength is not valid");
                    return false;
                }
                if (outputnodeconnector != null) {
                    // TODO
                }
            } else if (action instanceof PushMplsAction) {
                Integer ethertype = ((PushMplsAction) action).getEthernetType();
                if (ethertype != null && ethertype != 0x8847 && ethertype != 0x8848) {
                    logger.error("Ether Type is not valid for PushMplsAction");
                    return false;
                }
            } else if (action instanceof PushPbbAction) {
                Integer ethertype = ((PushPbbAction) action).getEthernetType();
                if (ethertype != null && ethertype != 0x88E7) {
                    logger.error("Ether type is not valid for PushPbbAction");
                    return false;
                }
            } else if (action instanceof PushVlanAction) {
                Integer ethertype = ((PushVlanAction) action).getEthernetType();
                if (ethertype != null && ethertype != 0x8100 && ethertype != 0x88a8) {
                    logger.error("Ether Type is not valid for PushVlanAction");
                    return false;
                }
            } else if (action instanceof SetDlDstAction || action instanceof SetDlSrcAction) {
                MacAddress address = ((SetDlDstAction) action).getAddress();
                if (address != null && !isL2AddressValid(address.toString())) {
                    logger.error("SetDlDstAction: Address not valid");
                    return false;
                }
            } else if (action instanceof SetDlSrcAction) {
                MacAddress address = ((SetDlSrcAction) action).getAddress();
                if (address != null && !isL2AddressValid(address.toString())) {
                    logger.error("SetDlSrcAction: Address not valid");
                    return false;
                }
            } else if (action instanceof SetQueueAction) {
                String queue = ((SetQueueAction) action).getQueue();
                if (queue != null && !isQueueValid(queue)) {
                    logger.error("Queue Id not valid");
                    return false;
                }
            } else if (action instanceof SetTpDstAction) {
                PortNumber port = ((SetTpDstAction) action).getPort();
                if (port != null && !isPortValid(port)) {
                    logger.error("Port not valid");
                }
            } else if (action instanceof SetTpSrcAction) {
                PortNumber port = ((SetTpSrcAction) action).getPort();
                if (port != null && !isPortValid(port)) {
                    logger.error("Port not valid");
                }
            } else if (action instanceof SetVlanIdAction) {
                VlanId vlanid = ((SetVlanIdAction) action).getVlanId();
                if (vlanid != null && !isVlanIdValid(vlanid.toString())) {
                    logger.error("Vlan ID %s is not in the range 0 - 4095");
                    return false;
                }
            } else if (action instanceof SetVlanPcpAction) {
                VlanPcp vlanpcp = ((SetVlanPcpAction) action).getVlanPcp();
                if (vlanpcp != null && !isVlanPriorityValid(vlanpcp.toString())) {
                    logger.error("Vlan priority %s is not in the range 0 - 7");
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean validateInstructions(Flow flow) {
        List<Instruction> instructionsList = new ArrayList<>();
        Instructions instructions = flow.getInstructions();
        if (instructions == null) {
            return false;
        }
        instructionsList = instructions.getInstruction();

        for (Instruction instruction : instructionsList) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction curInstruction = instruction
                    .getInstruction();
            if (curInstruction instanceof GoToTable) {

                Short tableid = ((GoToTable) curInstruction).getTableId();
                if (tableid < 0) {
                    logger.error("table id is not valid");
                    return false;
                }
            }

            else if (curInstruction instanceof WriteActions) {

                List<Action> action = ((WriteActions) curInstruction).getAction();
                validateActions(action);

            }

            else if (curInstruction instanceof ApplyActions) {
                List<Action> action = ((ApplyActions) curInstruction).getAction();
                validateActions(action);
            }

            else if (curInstruction instanceof ClearActions) {
                List<Action> action = ((ClearActions) curInstruction).getAction();
                validateActions(action);
            }

            else if (curInstruction instanceof Meter) {

                String meter = ((Meter) curInstruction).getMeter();
                if (meter != null && !isValidMeter(meter)) {
                    logger.error("Meter Id is not valid");
                    return false;
                }
            }

        }

        return true;
    }

    public static boolean isValidMeter(String meter) {
        // TODO
        return true;
    }

    public static boolean isQueueValid(String queue) {
        // TODO
        return true;
    }

    public static boolean isPortValid(PortNumber port) {
        // TODO
        return true;
    }

    public static boolean isL2AddressValid(String mac) {
        if (mac == null) {
            return false;
        }

        Pattern macPattern = Pattern.compile("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}");
        Matcher mm = macPattern.matcher(mac);
        if (!mm.matches()) {
            logger.debug("Ethernet address {} is not valid. Example: 00:05:b9:7c:81:5f", mac);
            return false;
        }
        return true;
    }

    public static boolean isProtocolValid(String protocol) {
        IPProtocols proto = IPProtocols.fromString(protocol);
        return (proto != null);
    }

    public static boolean isVlanIdValid(String vlanId) {
        int vlan = Integer.decode(vlanId);
        return ((vlan >= 0) && (vlan < 4096));
    }

    public static boolean isVlanPriorityValid(String vlanPriority) {
        int pri = Integer.decode(vlanPriority);
        return ((pri >= 0) && (pri < 8));
    }
}
