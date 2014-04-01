/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.implementation.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.action.Enqueue;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.FloodAll;
import org.opendaylight.controller.sal.action.HwPath;
import org.opendaylight.controller.sal.action.IFlowActionsProvider;
import org.opendaylight.controller.sal.action.Loopback;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.PushVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetDlType;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SwPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowActionsProvider implements IFlowActionsProvider {
    protected static final Logger logger = LoggerFactory.getLogger(FlowActionsProvider.class);
    private static final String NAME = "ODL base Flow Actions Provider";
    private static List<Class<? extends Action>> myActionClassesList = new ArrayList<Class<? extends Action>>();
    private static Set<Action> myActionFactory = new HashSet<Action>();
    static {
        myActionClassesList.add(Loopback.class);
        myActionClassesList.add(Drop.class);
        myActionClassesList.add(Flood.class);
        myActionClassesList.add(FloodAll.class);
        myActionClassesList.add(SwPath.class);
        myActionClassesList.add(HwPath.class);
        myActionClassesList.add(Controller.class);
        myActionClassesList.add(Output.class);
        myActionClassesList.add(Enqueue.class);
        myActionClassesList.add(PushVlan.class);
        myActionClassesList.add(PopVlan.class);
        myActionClassesList.add(SetDlSrc.class);
        myActionClassesList.add(SetDlDst.class);
        myActionClassesList.add(SetDlType.class);
        myActionClassesList.add(SetNwSrc.class);
        myActionClassesList.add(SetNwDst.class);
        myActionClassesList.add(SetTpSrc.class);
        myActionClassesList.add(SetTpDst.class);

        for (Class<? extends Action> actionClass : myActionClassesList) {
            try {
                myActionFactory.add(actionClass.newInstance());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                logger.error("Failed to instantiate Action class {}: {}", actionClass, e.getMessage());
            }
        }
    }

    @Override
    public Set<Action> getActionsFactory() {
        return new HashSet<Action>(myActionFactory);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
