
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.topology;

/**
 * @file   ITopologyService.java
 *
 * @brief  Topology methods provided by SAL toward the applications
 *
 * For example An application that startup late in the game or restart
 * for example, wants to know the current status of SAL, so can use
 * this interface to get a bunlk sync of the topology status.
 */

/**
 * Topology methods provided by SAL toward the applications
 */
public interface ITopologyService {
    /**
     * Tell to SAL that is time to send the updates of the
     * current topology because someone joined late the game
     *
     */
    public void sollicitRefresh();
}
