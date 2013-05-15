
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
 * Set destination transport port action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SetTpDst extends AbstractParameterAction<Integer> {
    public SetTpDst(Integer port) {
	    super(port);
    }

    /**
     * Returns the transport port the action will set
     * @return
     */
    public int getPort() {
        return getValue();
    }

    @Override
    protected boolean checkValue(Integer value) throws IllegalArgumentException {
        Arguments.argInRange(0, 0xffff, value);
        return super.checkValue(value);
    }
    @Override
    public String toString() {
        return "setTpDst" + "[port = " + getValue() + "]";
    }
}