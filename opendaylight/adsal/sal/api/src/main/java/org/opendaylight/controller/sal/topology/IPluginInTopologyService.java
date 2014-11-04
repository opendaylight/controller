
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.topology;

/**
 * @file   IPluginInTopologyService.java
 *
 * @brief  Methods that are invoked from SAL toward the protocol
 * plugin.
 *
 * For example if SAL startup late in respect to a protocol plugin, or
 * restarts, we need away to sollicit the push of older updates from
 * Protocol Plugins this interface serve the purpose. This a practical
 * example of the type of service provided by this interface
 */

/**
 * Methods that are invoked from SAL toward the protocol
 * plugin to sollicit Topoloy updates
 *
 */
public interface IPluginInTopologyService {
    /**
     * Tell to protocol plugin that is time to send the updates of the
     * current topology because someone joined late the game
     *
     */
    public void sollicitRefresh();
}
