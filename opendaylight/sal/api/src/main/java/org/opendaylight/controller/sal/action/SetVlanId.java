
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import static org.opendaylight.controller.sal.utils.Arguments.*;
/**
 * Set vlan id action
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SetVlanId extends AbstractParameterAction<Integer> {

	
    public SetVlanId(int vlanId) {
        super(vlanId);
    }

    /**
     * Returns the vlan id this action will set
     *
     * @return int
     */
    @Deprecated
    public int getVlanId() {
        return getValue();
    }

    @Override
    public String toString() {
        return "setVlan" + "[vlanId = " + getValue() + "]";
    }
    
    @Override
    protected boolean checkValue(Integer value) throws IllegalArgumentException {
        argInRange(0, 0xfff, value);
        return true;
    }
}
