/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.CommonPort.PortNumber;


public class PortNumberBuilder {

    public static PortNumber getDefaultInstance(java.lang.String defaultValue) {
        try {
            long uint32 = Long.parseLong(defaultValue);
            return new PortNumber(uint32);
        } catch(NumberFormatException e){
            return new PortNumber(defaultValue);
        }
    }

}
