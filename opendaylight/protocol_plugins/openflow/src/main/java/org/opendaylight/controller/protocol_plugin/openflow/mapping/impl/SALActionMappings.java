package org.opendaylight.controller.protocol_plugin.openflow.mapping.impl;

import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.protocol_plugin.openflow.internal.PortConverter;
import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.SALToOFActionTransformer;
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

    public static final SALToOFActionTransformer<Output> OUTPUT = new SALToOFActionTransformer<Output>() {

        @Override
        public Class<Output> getInputClass() {
            return Output.class;
        }

        @Override
        public OFAction transform(Output salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setMaxLength((short) 0xffff);
            ofAction.setPort(PortConverter.toOFPort(salAction.getPort()));
            return ofAction;
        }
    };

    public static final SALToOFActionTransformer<?> LOOPBACK = new SALToOFActionTransformer<Loopback>() {
        public OFAction transform(Loopback salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_IN_PORT.getValue());
            return ofAction;
        }

        @Override
        public Class<Loopback> getInputClass() {
            return Loopback.class;
        }
    };
    public static final SALToOFActionTransformer<?> FLOOD = new SALToOFActionTransformer<Flood>() {
        public OFAction transform(Flood salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_FLOOD.getValue());

            return ofAction;
        }

        @Override
        public Class<Flood> getInputClass() {

            return Flood.class;
        }
    };
    public static final SALToOFActionTransformer<?> FLOOD_ALL = new SALToOFActionTransformer<FloodAll>() {
        public OFAction transform(FloodAll salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_ALL.getValue());

            return ofAction;
        }

        @Override
        public Class<FloodAll> getInputClass() {

            return FloodAll.class;
        }
    };
    public static final SALToOFActionTransformer<?> CONTROLLER = new SALToOFActionTransformer<Controller>() {
        public OFAction transform(Controller salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_CONTROLLER.getValue());
            // We want the whole frame hitting the match be sent to the
            // controller
            ofAction.setMaxLength((short) 0xffff);

            return ofAction;
        }

        @Override
        public Class<Controller> getInputClass() {

            return Controller.class;
        }
    };
    public static final SALToOFActionTransformer<?> SW_PATH = new SALToOFActionTransformer<SwPath>() {
        public OFAction transform(SwPath salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_LOCAL.getValue());

            return ofAction;
        }

        @Override
        public Class<SwPath> getInputClass() {

            return SwPath.class;
        }
    };
    public static final SALToOFActionTransformer<?> HW_PATH = new SALToOFActionTransformer<HwPath>() {
        public OFAction transform(HwPath salAction) {
            OFActionOutput ofAction = new OFActionOutput();
            ofAction.setPort(OFPort.OFPP_NORMAL.getValue());

            return ofAction;
        }

        @Override
        public Class<HwPath> getInputClass() {

            return HwPath.class;
        }
    };
    public static final SALToOFActionTransformer<?> SET_VLAN_ID = new SALToOFActionTransformer<SetVlanId>() {
        public OFAction transform(SetVlanId salAction) {
            OFActionVirtualLanIdentifier ofAction = new OFActionVirtualLanIdentifier();
            ofAction.setVirtualLanIdentifier((short) salAction.getVlanId());

            return ofAction;
        }

        @Override
        public Class<SetVlanId> getInputClass() {

            return SetVlanId.class;
        }
    };
    public static final SALToOFActionTransformer<?> SET_VLAN_PCP = new SALToOFActionTransformer<SetVlanPcp>() {
        public OFAction transform(SetVlanPcp salAction) {
            OFActionVirtualLanPriorityCodePoint ofAction = new OFActionVirtualLanPriorityCodePoint();
            ofAction.setVirtualLanPriorityCodePoint(Integer.valueOf(salAction.getPcp())
                    .byteValue());
            return ofAction;
        }

        @Override
        public Class<SetVlanPcp> getInputClass() {

            return SetVlanPcp.class;
        }
    };
    public static final SALToOFActionTransformer<?> POP_VLAN = new SALToOFActionTransformer<PopVlan>() {
        public OFAction transform(PopVlan salAction) {
            OFActionStripVirtualLan ofAction = new OFActionStripVirtualLan();
            return ofAction;
        }

        @Override
        public Class<PopVlan> getInputClass() {

            return PopVlan.class;
        }
    };
    public static final SALToOFActionTransformer<?> SET_DL_SRC = new SALToOFActionTransformer<SetDlSrc>() {
        public OFAction transform(SetDlSrc salAction) {
            OFActionDataLayerSource ofAction = new OFActionDataLayerSource();
            ofAction.setDataLayerAddress(salAction.getDlAddress());
            return ofAction;
        }

        @Override
        public Class<SetDlSrc> getInputClass() {

            return SetDlSrc.class;
        }
    };
    public static final SALToOFActionTransformer<?> SET_DL_DST = new SALToOFActionTransformer<SetDlDst>() {
        public OFAction transform(SetDlDst salAction) {
            OFActionDataLayerDestination ofAction = new OFActionDataLayerDestination();
            ofAction.setDataLayerAddress(salAction.getDlAddress());

            return ofAction;
        }

        @Override
        public Class<SetDlDst> getInputClass() {

            return SetDlDst.class;
        }
    };
    public static final SALToOFActionTransformer<?> SET_NW_SRC = new SALToOFActionTransformer<SetNwSrc>() {
        public OFAction transform(SetNwSrc salAction) {
            OFActionNetworkLayerSource ofAction = new OFActionNetworkLayerSource();
            ofAction.setNetworkAddress(NetUtils.byteArray4ToInt(salAction.getAddress()
                    .getAddress()));

            return ofAction;
        }

        @Override
        public Class<SetNwSrc> getInputClass() {

            return SetNwSrc.class;
        }
    };
    public static final SALToOFActionTransformer<?> SET_NW_DST = new SALToOFActionTransformer<SetNwDst>() {
        public OFAction transform(SetNwDst salAction) {
            OFActionNetworkLayerDestination ofAction = new OFActionNetworkLayerDestination();
            ofAction.setNetworkAddress(NetUtils.byteArray4ToInt(salAction.getAddress()
                    .getAddress()));

            return ofAction;
        }

        @Override
        public Class<SetNwDst> getInputClass() {

            return SetNwDst.class;
        }
    };
    public static final SALToOFActionTransformer<?> SET_NW_TOS = new SALToOFActionTransformer<SetNwTos>() {
        public OFAction transform(SetNwTos salAction) {
            OFActionNetworkTypeOfService ofAction = new OFActionNetworkTypeOfService();
            ofAction.setNetworkTypeOfService(Integer.valueOf(salAction.getNwTos())
                    .byteValue());

            return ofAction;
        }

        @Override
        public Class<SetNwTos> getInputClass() {

            return SetNwTos.class;
        }
    };
    public static final SALToOFActionTransformer<?> SET_TP_SRC = new SALToOFActionTransformer<SetTpSrc>() {
        public OFAction transform(SetTpSrc salAction) {
            OFActionTransportLayerSource ofAction = new OFActionTransportLayerSource();
            ofAction.setTransportPort(Integer.valueOf(salAction.getPort()).shortValue());

            return ofAction;
        }

        @Override
        public Class<SetTpSrc> getInputClass() {

            return SetTpSrc.class;
        }
    };
    public static final SALToOFActionTransformer<?> SET_TP_DST = new SALToOFActionTransformer<SetTpDst>() {
        public OFAction transform(SetTpDst salAction) {
            OFActionTransportLayerDestination ofAction = new OFActionTransportLayerDestination();
            ofAction.setTransportPort(Integer.valueOf(salAction.getPort()).shortValue());
            return ofAction;
        }

        @Override
        public Class<SetTpDst> getInputClass() {
            return SetTpDst.class;
        }
    };
}
