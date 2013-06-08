
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core;

import java.util.Map;

import org.openflow.protocol.OFType;

/**
 * This interface defines an abstraction of the Open Flow Controller that allows applications to control and manage the Open Flow switches.
 *
 */
public interface IController {

    /**
     * Allows application to start receiving OF messages received from switches.
     * @param type the type of OF message that applications want to receive
     * @param listener: Object that implements the IMessageListener
     */
    public void addMessageListener(OFType type, IMessageListener listener);

    /**
     * Allows application to stop receiving OF message received from switches.
     * @param type The type of OF message that applications want to stop receiving
     * @param listener The object that implements the IMessageListener
     */
    public void removeMessageListener(OFType type, IMessageListener listener);

    /**
     * Allows application to start receiving switch state change events.
     * @param listener The object that implements the ISwitchStateListener
     */
    public void addSwitchStateListener(ISwitchStateListener listener);

    /**
     * Allows application to stop receiving switch state change events.
     * @param listener The object that implements the ISwitchStateListener
     */
    public void removeSwitchStateListener(ISwitchStateListener listener);

    /**
     * Returns a map containing all the OF switches that are currently connected to the Controller.
     * @return Map of ISwitch
     */
    public Map<Long, ISwitch> getSwitches();

    /**
     * Returns the ISwitch of the given switchId.
     *
     * @param switchId The switch ID
     * @return ISwitch if present, null otherwise
     */
    public ISwitch getSwitch(Long switchId);
    
}
