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
 * Set source transport port action
 * 
 */
public class SetTpSrc extends AbstractParameterAction<Integer> {
    public SetTpSrc(Integer port) {
        super(port);
    }

    /**
     * Returns the transport port the action will set
     * 
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
        return "setTpSrc" + "[port = " + getValue() + "]";
    }
}
