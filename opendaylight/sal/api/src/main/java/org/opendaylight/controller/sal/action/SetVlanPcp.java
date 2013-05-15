
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import org.opendaylight.controller.sal.utils.Arguments;

/**
 * Set vlan PCP action
 */
public class SetVlanPcp extends AbstractParameterAction<Integer>{
	
    public SetVlanPcp(int pcp) {
        super(pcp);
    }
    
    /**
     * Returns the value of the vlan PCP this action will set
     * @return int
     */
    public int getPcp() {
        return getValue();
    }

    
    @Override
    public String toString() {
        return "setVlanPcp" + "[pcp = " + Integer.toHexString(getValue()) + "]";
    }

    @Override
    protected boolean checkValue(Integer value) throws IllegalArgumentException {
        Arguments.argInRange(0, 0x7, value);
        return true;
    }
}
