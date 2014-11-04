
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core;

/**
 * Interface to be implemented by applications that want to receive switch state event changes.
 *
 */
public interface ISwitchStateListener {
    /**
     * This method is invoked by Controller when a switch has been connected to the Controller.
     * Application who wants to receive this event needs to implement this method.
     * @param sw The switch that has just connected.
     */
    public void switchAdded(ISwitch sw);

    /**
     * This method is invoked by Controller when a switch has been disconnected from the Controller.
     * Application who wants to receive this event needs to implement this method.
     * @param sw The switch that has just disconnected.
     */
    public void switchDeleted(ISwitch sw);

}
