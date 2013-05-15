package org.opendaylight.controller.sal.rest.transform.impl;

import org.opendaylight.controller.sal.action.Action;
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
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwTos;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SetVlanCfi;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.action.SetVlanPcp;
import org.opendaylight.controller.sal.action.SwPath;
import org.opendaylight.controller.concepts.transform.CompositeClassBasedTransformer;
import org.opendaylight.controller.concepts.transform.InputClassBasedTransformer;
import org.opendaylight.controller.sal.rest.action.ActionRTO;
import org.opendaylight.controller.sal.rest.action.SetNwSrc;

public class ActionsToRest {

    public static InputClassBasedTransformer<Action, Controller, ActionRTO> CONTROLLER = new InputClassBasedTransformer<Action, Controller, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return Controller.class;
        }

        @Override
        public ActionRTO transform(Controller input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.Controller();
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, Drop, ActionRTO> DROP = new InputClassBasedTransformer<Action, Drop, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return Drop.class;
        }

        @Override
        public ActionRTO transform(Drop input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.Drop();
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, Flood, ActionRTO> FLOOD = new InputClassBasedTransformer<Action, Flood, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return Flood.class;
        }

        @Override
        public ActionRTO transform(Flood input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.Flood();
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, FloodAll, ActionRTO> FLOOD_ALL = new InputClassBasedTransformer<Action, FloodAll, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return FloodAll.class;
        }

        @Override
        public ActionRTO transform(FloodAll input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.FloodAll();
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, HwPath, ActionRTO> HW_PATH = new InputClassBasedTransformer<Action, HwPath, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return HwPath.class;
        }

        @Override
        public ActionRTO transform(HwPath input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.HwPath();

            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, Loopback, ActionRTO> LOOPBACK = new InputClassBasedTransformer<Action, Loopback, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return Loopback.class;
        }

        @Override
        public ActionRTO transform(Loopback input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.Loopback();
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, Output, ActionRTO> OUTPUT = new InputClassBasedTransformer<Action, Output, ActionRTO>() {
        @Override
        public Class<? extends Output> getInputClass() {
            return Output.class;
        }

        @Override
        public ActionRTO transform(Output input) {
            org.opendaylight.controller.sal.rest.action.Output ret = new org.opendaylight.controller.sal.rest.action.Output(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, PopVlan, ActionRTO> POP_VLAN = new InputClassBasedTransformer<Action, PopVlan, ActionRTO>() {
        @Override
        public Class<? extends PopVlan> getInputClass() {
            return PopVlan.class;
        }

        @Override
        public ActionRTO transform(PopVlan input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.PopVlan();
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, PushVlan, ActionRTO> PUSH_VLAN = new InputClassBasedTransformer<Action, PushVlan, ActionRTO>() {
        @Override
        public Class<? extends PushVlan> getInputClass() {
            return PushVlan.class;
        }

        @Override
        public ActionRTO transform(PushVlan input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.PushVlan(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetDlSrc, ActionRTO> SET_DL_SRC = new InputClassBasedTransformer<Action, SetDlSrc, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetDlSrc.class;
        }

        @Override
        public ActionRTO transform(SetDlSrc input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetDlSrc(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetDlDst, ActionRTO> SET_DL_DST = new InputClassBasedTransformer<Action, SetDlDst, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetDlDst.class;
        }

        @Override
        public ActionRTO transform(SetDlDst input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetDlDst(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetDlType, ActionRTO> SET_DL_TYPE = new InputClassBasedTransformer<Action, SetDlType, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetDlType.class;
        }

        @Override
        public ActionRTO transform(SetDlType input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetDlType(
                    input.getValue());

            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetNwDst, ActionRTO> SET_NW_DST = new InputClassBasedTransformer<Action, SetNwDst, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetNwDst.class;
        }

        @Override
        public ActionRTO transform(SetNwDst input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetNwDst(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, org.opendaylight.controller.sal.action.SetNwSrc, ActionRTO> SET_NW_SRC = new InputClassBasedTransformer<Action, org.opendaylight.controller.sal.action.SetNwSrc, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return org.opendaylight.controller.sal.action.SetNwSrc.class;
        }

        @Override
        public ActionRTO transform(
                org.opendaylight.controller.sal.action.SetNwSrc input) {
            ActionRTO ret = new SetNwSrc(input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetNwTos, ActionRTO> SET_NS_TOS = new InputClassBasedTransformer<Action, SetNwTos, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetNwTos.class;
        }

        @Override
        public ActionRTO transform(SetNwTos input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetNwTos(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetTpDst, ActionRTO> SET_TP_DST = new InputClassBasedTransformer<Action, SetTpDst, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetTpDst.class;
        }

        @Override
        public ActionRTO transform(SetTpDst input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetTpDst(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetTpSrc, ActionRTO> SET_TP_SRC = new InputClassBasedTransformer<Action, SetTpSrc, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetTpSrc.class;
        }

        @Override
        public ActionRTO transform(SetTpSrc input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetTpSrc(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetVlanCfi, ActionRTO> SET_VLAN_CFI = new InputClassBasedTransformer<Action, SetVlanCfi, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetVlanCfi.class;
        }

        @Override
        public ActionRTO transform(SetVlanCfi input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetVlanCfi(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetVlanId, ActionRTO> SET_VLAN_ID = new InputClassBasedTransformer<Action, SetVlanId, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetVlanId.class;
        }

        @Override
        public ActionRTO transform(SetVlanId input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetVlanId(
                    input.getValue());
            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SetVlanPcp, ActionRTO> SET_VLAN_PCP = new InputClassBasedTransformer<Action, SetVlanPcp, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SetVlanPcp.class;
        }

        @Override
        public ActionRTO transform(SetVlanPcp input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SetVlanPcp(
                    input.getValue());

            return ret;
        }
    };
    public static InputClassBasedTransformer<Action, SwPath, ActionRTO> SW_PATH = new InputClassBasedTransformer<Action, SwPath, ActionRTO>() {
        @Override
        public Class<? extends Action> getInputClass() {
            return SwPath.class;
        }

        @Override
        public ActionRTO transform(SwPath input) {
            ActionRTO ret = new org.opendaylight.controller.sal.rest.action.SwPath();
            return ret;
        }
    };

    protected static void registerBaseActionTransformers(
            CompositeClassBasedTransformer<Action, ActionRTO> composite) {
        composite.addTransformer(CONTROLLER);
        composite.addTransformer(DROP);
        composite.addTransformer(FLOOD);
        composite.addTransformer(FLOOD_ALL);
        composite.addTransformer(HW_PATH);
        composite.addTransformer(LOOPBACK);
        composite.addTransformer(OUTPUT);
        composite.addTransformer(POP_VLAN);
        composite.addTransformer(PUSH_VLAN);
        composite.addTransformer(SET_DL_SRC);
        composite.addTransformer(SET_DL_DST);
        composite.addTransformer(SET_DL_TYPE);
        composite.addTransformer(SET_NW_DST);
        composite.addTransformer(SET_NW_SRC);
        composite.addTransformer(SET_NS_TOS);
        composite.addTransformer(SET_TP_DST);
        composite.addTransformer(SET_TP_SRC);
        composite.addTransformer(SET_VLAN_CFI);
        composite.addTransformer(SET_VLAN_ID);
        composite.addTransformer(SET_VLAN_PCP);
        composite.addTransformer(SW_PATH);
    }
}
