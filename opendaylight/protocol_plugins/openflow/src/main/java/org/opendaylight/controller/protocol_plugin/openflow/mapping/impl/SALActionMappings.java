package org.opendaylight.controller.protocol_plugin.openflow.mapping.impl;

import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.protocol_plugin.openflow.internal.PortConverter;
import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.SALActionMapper;
import org.opendaylight.controller.sal.action.Action;
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
import org.opendaylight.controller.sal.utils.NetUtils;
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

public class SALActionMappings {

    public static final SALActionMapper<Output> OUTPUT = new SALActionMapper<Output>() {

        @Override
        public Class<Output> getSalClass() {
            return Output.class;
        }

        @Override
        public OFAction ofActionFromSal(Action salAction) {
            // FIXME Add Checks;
            Output a = (Output) salAction;
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setMaxLength((short) 0xffff);
            ofAction.setPort(PortConverter.toOFPort(a.getPort()));
            return ofAction;
        }
    };

    public static final SALActionMapper<?> LOOPBACK = new SALActionMapper<Loopback>() {
        public OFAction ofActionFromSal(Action salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_IN_PORT.getValue());

            return ofAction;
        }

        @Override
        public Class<Loopback> getSalClass() {
            return Loopback.class;
        }
    };
    public static final SALActionMapper<?> FLOOD = new SALActionMapper<Flood>() {
        public OFAction ofActionFromSal(Action salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_FLOOD.getValue());

            return ofAction;
        }

        @Override
        public Class<Flood> getSalClass() {

            return Flood.class;
        }
    };
    public static final SALActionMapper<?> FLOOD_ALL = new SALActionMapper<FloodAll>() {
        public OFAction ofActionFromSal(Action salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_ALL.getValue());

            return ofAction;
        }

        @Override
        public Class<FloodAll> getSalClass() {

            return FloodAll.class;
        }
    };
    public static final SALActionMapper<?> CONTROLLER = new SALActionMapper<Controller>() {
        public OFAction ofActionFromSal(Action salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_CONTROLLER.getValue());
            // We want the whole frame hitting the match be sent to the
            // controller
            ofAction.setMaxLength((short) 0xffff);

            return ofAction;
        }

        @Override
        public Class<Controller> getSalClass() {

            return Controller.class;
        }
    };
    public static final SALActionMapper<?> SW_PATH = new SALActionMapper<SwPath>() {
        public OFAction ofActionFromSal(Action salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_LOCAL.getValue());

            return ofAction;
        }

        @Override
        public Class<SwPath> getSalClass() {

            return SwPath.class;
        }
    };
    public static final SALActionMapper<?> HW_PATH = new SALActionMapper<HwPath>() {
        public OFAction ofActionFromSal(Action salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_NORMAL.getValue());

            return ofAction;
        }

        @Override
        public Class<HwPath> getSalClass() {

            return HwPath.class;
        }
    };
    public static final SALActionMapper<?> SET_VLAN_ID = new SALActionMapper<SetVlanId>() {
        public OFAction ofActionFromSal(Action salAction) {
            SetVlanId a = (SetVlanId) salAction;
            OFActionVirtualLanIdentifier ofAction = new OFActionVirtualLanIdentifier();
            ofAction.setVirtualLanIdentifier((short) a.getVlanId());

            return ofAction;
        }

        @Override
        public Class<SetVlanId> getSalClass() {

            return SetVlanId.class;
        }
    };
    public static final SALActionMapper<?> SET_VLAN_PCP = new SALActionMapper<SetVlanPcp>() {
        public OFAction ofActionFromSal(Action salAction) {
            SetVlanPcp a = (SetVlanPcp) salAction;
            OFActionVirtualLanPriorityCodePoint ofAction = new OFActionVirtualLanPriorityCodePoint();
            ofAction.setVirtualLanPriorityCodePoint(Integer.valueOf(a.getPcp())
                    .byteValue());

            return ofAction;
        }

        @Override
        public Class<SetVlanPcp> getSalClass() {

            return SetVlanPcp.class;
        }
    };
    public static final SALActionMapper<?> POP_VLAN = new SALActionMapper<PopVlan>() {
        public OFAction ofActionFromSal(Action salAction) {
            OFActionStripVirtualLan ofAction = new OFActionStripVirtualLan();

            return ofAction;
        }

        @Override
        public Class<PopVlan> getSalClass() {

            return PopVlan.class;
        }
    };
    public static final SALActionMapper<?> SET_DL_SRC = new SALActionMapper<SetDlSrc>() {
        public OFAction ofActionFromSal(Action salAction) {
            SetDlSrc a = (SetDlSrc) salAction;
            OFActionDataLayerSource ofAction = new OFActionDataLayerSource();
            ofAction.setDataLayerAddress(a.getDlAddress());

            return ofAction;
        }

        @Override
        public Class<SetDlSrc> getSalClass() {

            return SetDlSrc.class;
        }
    };
    public static final SALActionMapper<?> SET_DL_DST = new SALActionMapper<SetDlDst>() {
        public OFAction ofActionFromSal(Action salAction) {
            SetDlDst a = (SetDlDst) salAction;
            OFActionDataLayerDestination ofAction = new OFActionDataLayerDestination();
            ofAction.setDataLayerAddress(a.getDlAddress());

            return ofAction;
        }

        @Override
        public Class<SetDlDst> getSalClass() {

            return SetDlDst.class;
        }
    };
    public static final SALActionMapper<?> SET_NW_SRC = new SALActionMapper<SetNwSrc>() {
        public OFAction ofActionFromSal(Action salAction) {
            SetNwSrc a = (SetNwSrc) salAction;
            OFActionNetworkLayerSource ofAction = new OFActionNetworkLayerSource();
            ofAction.setNetworkAddress(NetUtils.byteArray4ToInt(a.getAddress()
                    .getAddress()));

            return ofAction;
        }

        @Override
        public Class<SetNwSrc> getSalClass() {

            return SetNwSrc.class;
        }
    };
    public static final SALActionMapper<?> SET_NW_DST = new SALActionMapper<SetNwDst>() {
        public OFAction ofActionFromSal(Action salAction) {
            SetNwDst a = (SetNwDst) salAction;
            OFActionNetworkLayerDestination ofAction = new OFActionNetworkLayerDestination();
            ofAction.setNetworkAddress(NetUtils.byteArray4ToInt(a.getAddress()
                    .getAddress()));

            return ofAction;
        }

        @Override
        public Class<SetNwDst> getSalClass() {

            return SetNwDst.class;
        }
    };
    public static final SALActionMapper<?> SET_NW_TOS = new SALActionMapper<SetNwTos>() {
        public OFAction ofActionFromSal(Action salAction) {
            SetNwTos a = (SetNwTos) salAction;
            OFActionNetworkTypeOfService ofAction = new OFActionNetworkTypeOfService();
            ofAction.setNetworkTypeOfService(Integer.valueOf(a.getNwTos())
                    .byteValue());

            return ofAction;
        }

        @Override
        public Class<SetNwTos> getSalClass() {

            return SetNwTos.class;
        }
    };
    public static final SALActionMapper<?> SET_TP_SRC = new SALActionMapper<SetTpSrc>() {
        public OFAction ofActionFromSal(Action salAction) {
            SetTpSrc a = (SetTpSrc) salAction;
            OFActionTransportLayerSource ofAction = new OFActionTransportLayerSource();
            ofAction.setTransportPort(Integer.valueOf(a.getPort()).shortValue());

            return ofAction;
        }

        @Override
        public Class<SetTpSrc> getSalClass() {

            return SetTpSrc.class;
        }
    };
    public static final SALActionMapper<?> SET_TP_DST = new SALActionMapper<SetTpDst>() {
        public OFAction ofActionFromSal(Action salAction) {
            SetTpDst a = (SetTpDst) salAction;
            OFActionTransportLayerDestination ofAction = new OFActionTransportLayerDestination();
            ofAction.setTransportPort(Integer.valueOf(a.getPort()).shortValue());

            return ofAction;
        }

        @Override
        public Class<SetTpDst> getSalClass() {

            return SetTpDst.class;
        }
    };
}
