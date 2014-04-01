/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.Set;

/**
 * The interface describes the methods that the FlowActionProvider services
 * have to implement. These services dynamically provide the controller with
 * the methods for parsing new Action classes from string form
 */
public interface IFlowActionsProvider {
    /**
     * The Flow Action provider's name
     *
     * @return The name of this provider
     */
    String getName();

    /**
     * Returns the this provider's Action factory
     *
     * @return The map of Action classes and their empty instance this providers
     *         brings in. This is consumed by the FlowActionFactory service when
     *         this action providers registers
     */
    Set<Action> getActionsFactory();
}
