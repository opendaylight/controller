
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.net.InetAddress;

/**
 * Set network source address action
 */

public class SetNwSrc extends AbstractParameterAction<InetAddress> {

    public SetNwSrc(InetAddress address) {
        super(address);
    }

    /**
     * Returns the network address this action will set
     *
     * @return  InetAddress
     */
    public InetAddress getAddress() {
        return getValue();
    }
    
    @Deprecated
    public String getAddressAsString() {
        return getValue().getHostAddress();
    }

    
    @Override
    public String toString() {
        return "setNwSrc" + "[address = " + getValue() + "]";
    }
}
