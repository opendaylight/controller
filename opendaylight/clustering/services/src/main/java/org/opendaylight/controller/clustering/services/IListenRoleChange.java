
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IListenRoleChange.java
 *
 *
 * @brief  Interface that needs to be implemented by who wants to be
 * notified of new active change
 *
 * @deprecated
 *
 * Interface that needs to be implemented by who wants to be notified
 * of newly active taking over. Interface that is supposed to be
 * short-lived and will be removed as soon as active-standby goal is reached.
 */
package org.opendaylight.controller.clustering.services;

/**
 * Interface that needs to be implemented by who wants to be notified
 * of newly active taking over. Interface that is supposed to be
 * short-lived and will be removed as soon as active-standby goal is reached.
 *
 */
public interface IListenRoleChange {

    /**
     * @deprecated
     * Function that will be called when a new active is
     * available. This function is supposed only to be of use till
     * active-standby milestone is reached, after will be removed.
     *
     */
    public void newActiveAvailable();
}
