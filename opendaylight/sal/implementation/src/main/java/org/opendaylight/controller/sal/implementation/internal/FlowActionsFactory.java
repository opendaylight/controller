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
import org.opendaylight.controller.sal.action.ActionsParseResult;
import org.opendaylight.controller.sal.action.IFlowActionsFactory;
import org.opendaylight.controller.sal.action.IFlowActionsProvider;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of IFlowActionsFactory service, for parsing flow actions in
 * string form. It holds all action factories providers bring in.
 */
public class FlowActionsFactory implements IFlowActionsFactory {
    protected static final Logger logger = LoggerFactory.getLogger(FlowActionsFactory.class);
    Set<Action> factory;
    List<IFlowActionsProvider> providers;

    public FlowActionsFactory() {
        factory = new HashSet<Action>();
        providers = new ArrayList<IFlowActionsProvider>();
    }

    void setActionProvider(IFlowActionsProvider provider) {
        if (provider != null) {
            providers.add(provider);
            factory.addAll(provider.getActionsFactory());
            if (logger.isDebugEnabled()) {
                logger.debug("Adding provider: {}, Actions: {}", provider, provider.getActionsFactory());
            }
        }
    }

    void unsetActionProvider(IFlowActionsProvider provider) {
        if (provider != null) {
            providers.remove(provider);
            factory.retainAll(provider.getActionsFactory());
            if (logger.isDebugEnabled()) {
                logger.debug("Removing provider: {}, Actions: {}", provider, provider.getActionsFactory());
            }
        }
    }

    @Override
    public List<Class<? extends Action>> getAvailableActions() {
        List<Class<? extends Action>> classList = new ArrayList<Class<? extends Action>>();
        for (Action action : this.factory) {
            classList.add(action.getClass());
        }
        return classList;
    }

    @Override
    public Action parseAction(String actionString, Node node) {
        for (Action entry : factory) {
            Action parsed = entry.fromString(actionString, node);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    @Override
    public ActionsParseResult parseActionList(List<String> actionStringList, Node node) {
        List<Action> actionList = new ArrayList<Action>();
        StringBuffer errorBuffer = new StringBuffer();
        for (String actionString : actionStringList) {
            Action action = parseAction(actionString, node);
            if (action != null) {
                actionList.add(action);
            } else {
                errorBuffer.append(actionString);
                errorBuffer.append(" ");
            }
        }
        Status status = (errorBuffer.length() == 0) ? new Status(StatusCode.SUCCESS) : new Status(
                StatusCode.BADREQUEST, "The following actions are not recognized: " + errorBuffer.toString());
        return new ActionsParseResult(actionList, status);
    }

}
