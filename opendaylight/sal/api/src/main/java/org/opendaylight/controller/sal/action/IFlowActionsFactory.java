/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.List;

import org.opendaylight.controller.sal.core.Node;

/**
 * The interface describes the methods exposed by the FlowActionFactory service
 * for parsing the flow actions from string form
 */
public interface IFlowActionsFactory {
    /**
     * Returns the current list of SAL flow action classes which can be parsed
     * from string form
     *
     * @return The list of Action classes parse-able from string form
     */
    List<Class <? extends Action>> getAvailableActions();

    /**
     * Parses a flow action in string form and returns the Action instance
     *
     * @param actionString
     *            The action in string form
     * @return The corresponding Action object, null if not recognized
     */
    Action parseAction(String actionString, Node node);

    /**
     * Parses a list of flow actions in string form and returns the
     * corresponding list of successfully parsed Action instances along with the
     * status of the operation as a ActionsParseResult object
     *
     * @param actionList
     *            The list of actions in string form
     * @param node
     *            The network node context for which the flow actions are
     *            specified
     * @return The ActionResult instance containing the list of Action objects
     */
    ActionsParseResult parseActionList(List<String> actionList, Node node);
}
