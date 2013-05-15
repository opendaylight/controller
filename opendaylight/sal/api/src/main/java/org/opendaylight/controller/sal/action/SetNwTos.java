
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import static org.opendaylight.controller.sal.utils.Arguments.*;

/**
 * Set network TOS action
 */
public class SetNwTos extends AbstractParameterAction<Integer> {


    public SetNwTos(Integer tos) {
        super(tos);
    }

    /**
     * Returns the network TOS value which the action will set
     *
     * @return int
     */
    @Deprecated
    public int getNwTos() {
        return getValue();
    }

    @Override
    public String toString() {
        return "setNwTos" + "[tos = 0x" + Integer.toHexString(getValue()) + "]";
    }
    
    @Override
    protected boolean checkValue(Integer value) throws IllegalArgumentException {
        argInRange(0, 0x3f, value);
        return super.checkValue(value);
    }
}
