package org.opendaylight.controller.forwardingrulesmanager.consumer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushPbbActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetQueueActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ClearActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.MeterCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;

public class FRMUtil {
    protected static final Logger logger = LoggerFactory.getLogger(FRMUtil.class);
    private static final String NAMEREGEX = "^[a-zA-Z0-9]+$";

    public static enum operation {
        ADD, DELETE, UPDATE, GET
    };

    public static boolean isNameValid(String name) {

        // Name validation
        if (name == null || name.trim().isEmpty() || !name.matches(NAMEREGEX)) {
            return false;
        }
        return true;

    }

    public static boolean validateMatch(Flow flow) {
        Match match = flow.getMatch();
        if (match != null) {
            EthernetMatch ethernetmatch = match.getEthernetMatch();
            IpMatch ipmatch = match.getIpMatch();
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
                    }
                }
            } else if (ipmatch != null) {
                if (ipmatch.getIpProtocol() != null && isProtocolValid(ipmatch.getIpProtocol().toString())) {
                    logger.error("Protocol is not valid");
                    return false;
                }
            } else if (vlanmatch != null) {
                if (vlanmatch.getVlanId() != null && isVlanIdValid(vlanmatch.getVlanId().toString())) {
                    logger.error("Vlan ID is not in the range 0 - 4095");
                    return false;
                }

                if (vlanmatch.getVlanPcp() != null && isVlanPriorityValid(vlanmatch.getVlanPcp().toString())) {
                    logger.error("Vlan priority is not in the range 0 - 7");
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
            if (action instanceof ControllerActionCase) {
                Integer length = ((ControllerActionCase) action).getControllerAction().getMaxLength();
                if (length < 0 || length > 65294) {
                    logger.error("Controller: MaxLength is not valid");
                    return false;
                }
            } else if (action instanceof OutputActionCase) {
                Integer length = ((OutputActionCase) action).getOutputAction().getMaxLength();
                Uri outputnodeconnector = ((OutputActionCase) action).getOutputAction().getOutputNodeConnector();
                if (length < 0 || length > 65294) {
                    logger.error("OutputAction: MaxLength is not valid");
                    return false;
                }
                if (outputnodeconnector != null) {
                    // TODO
                }
            } else if (action instanceof PushMplsActionCase) {
                Integer ethertype = ((PushMplsActionCase) action).getPushMplsAction().getEthernetType();
                if (ethertype != null && ethertype != 0x8847 && ethertype != 0x8848) {
                    logger.error("Ether Type is not valid for PushMplsAction");
                    return false;
                }
            } else if (action instanceof PushPbbActionCase) {
                Integer ethertype = ((PushPbbActionCase) action).getPushPbbAction().getEthernetType();
                if (ethertype != null && ethertype != 0x88E7) {
                    logger.error("Ether type is not valid for PushPbbAction");
                    return false;
                }
            } else if (action instanceof PushVlanActionCase) {
                Integer ethertype = ((PushVlanActionCase) action).getPushVlanAction().getEthernetType();
                if (ethertype != null && ethertype != 0x8100 && ethertype != 0x88a8) {
                    logger.error("Ether Type is not valid for PushVlanAction");
                    return false;
                }
            } else if (action instanceof SetDlDstActionCase || action instanceof SetDlSrcActionCase) {
                MacAddress address = ((SetDlDstActionCase) action).getSetDlDstAction().getAddress();
                if (address != null && !isL2AddressValid(address.toString())) {
                    logger.error("SetDlDstAction: Address not valid");
                    return false;
                }
            } else if (action instanceof SetDlSrcActionCase) {
                MacAddress address = ((SetDlSrcActionCase) action).getSetDlSrcAction().getAddress();
                if (address != null && !isL2AddressValid(address.toString())) {
                    logger.error("SetDlSrcAction: Address not valid");
                    return false;
                }
            } else if (action instanceof SetQueueActionCase) {
                String queue = ((SetQueueActionCase) action).getSetQueueAction().getQueue();
                if (queue != null && !isQueueValid(queue)) {
                    logger.error("Queue Id not valid");
                    return false;
                }
            } else if (action instanceof SetTpDstActionCase) {
                PortNumber port = ((SetTpDstActionCase) action).getSetTpDstAction().getPort();
                if (port != null && !isPortValid(port)) {
                    logger.error("Port not valid");
                }
            } else if (action instanceof SetTpSrcActionCase) {
                PortNumber port = ((SetTpSrcActionCase) action).getSetTpSrcAction().getPort();
                if (port != null && !isPortValid(port)) {
                    logger.error("Port not valid");
                }
            } else if (action instanceof SetVlanIdActionCase) {
                VlanId vlanid = ((SetVlanIdActionCase) action).getSetVlanIdAction().getVlanId();
                if (vlanid != null && !isVlanIdValid(vlanid.toString())) {
                    logger.error("Vlan ID %s is not in the range 0 - 4095");
                    return false;
                }
            } else if (action instanceof SetVlanPcpActionCase) {
                VlanPcp vlanpcp = ((SetVlanPcpActionCase) action).getSetVlanPcpAction().getVlanPcp();
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
        if( instructions == null ) {
            return false;
        }
        instructionsList = instructions.getInstruction();

        for (Instruction instruction : instructionsList) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction curInstruction = instruction
                    .getInstruction();
            if (curInstruction instanceof GoToTableCase) {

                Short tableid = ((GoToTableCase) curInstruction).getGoToTable().getTableId();
                if (tableid < 0) {
                    logger.error("table id is not valid");
                    return false;
                }
            }

            else if (curInstruction instanceof WriteActionsCase) {

                List<Action> action = ((WriteActionsCase) curInstruction).getWriteActions().getAction();
                validateActions(action);

            }

            else if (curInstruction instanceof ApplyActionsCase) {
                List<Action> action = ((ApplyActionsCase) curInstruction).getApplyActions().getAction();
                validateActions(action);
            }

            else if (curInstruction instanceof ClearActionsCase) {
                List<Action> action = ((ClearActionsCase) curInstruction).getClearActions().getAction();
                validateActions(action);
            }

            else if (curInstruction instanceof MeterCase) {

                String meter = ((MeterCase) curInstruction).getMeter().getMeter();
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
