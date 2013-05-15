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

import org.opendaylight.controller.sal.utils.Arguments;

/**
 * Set vlan CFI action
 * 
 */
public class SetVlanCfi extends AbstractParameterAction<Integer> {

    public SetVlanCfi(Integer cfi) {
        super(cfi);
    }

    /**
     * Returns the 802.1q CFI value that this action will set
     * 
     * @return
     */
    public int getCfi() {
        return getValue();
    }

    @Override
    protected boolean checkValue(Integer value) {
        Arguments.argInRange(0, 0x1, value);
        return super.checkValue(value);
    }

    @Override
    public String toString() {
        return "setVlanCfi" + "[cfi = " + Integer.toHexString(getValue()) + "]";
    }
}
