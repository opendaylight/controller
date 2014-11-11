
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core;

import org.openflow.protocol.OFMessage;

/**
 * Interface to be implemented by applications that want to receive OF messages.
 *
 */
public interface IMessageListener {
    /**
     * This method is called by the Controller when a message is received from a switch.
     * Application who is interested in receiving OF Messages needs to implement this method.
     * @param sw The ISwitch which sent the OF message
     * @param msg The OF message
     */
    public void receive(ISwitch sw, OFMessage msg);
}
