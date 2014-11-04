/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.web;

import org.opendaylight.controller.sal.authorization.UserLevel;

public interface IDaylightWeb {
    /**
     * Returns the name of the bundle. In the GUI, this name will be displayed
     * on the tab.
     *
     * @return Name assigned to the bundle.
     */
    public String getWebName();

    /**
     * Returns the Id assigned to the web bundle.
     *
     * @return Id assigned to the web bundle.
     */
    public String getWebId();

    /**
     * Returns the position where the bundle tab will be placed in the GUI.
     *
     * @return Position number for the bundle tab.
     */
    public short getWebOrder();

    /**
     * This method checks if the user is authorized to access the bundle.
     *
     * @param userLevel
     *            user role level in the controller space.
     *
     * @return true, if user is authorized to access the bundle, else false.
     */
    public boolean isAuthorized(UserLevel userLevel);
}
