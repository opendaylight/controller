
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the generic action to be applied to the matched frame/packet/message
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({Controller.class, Drop.class, Flood.class, FloodAll.class, HwPath.class, Loopback.class, Output.class,
			 PopVlan.class, PushVlan.class, SetDlDst.class, SetDlSrc.class, SetDlType.class, SetNwDst.class, SetNwSrc.class,
			 SetNwTos.class, SetTpDst.class, SetTpSrc.class, SetVlanCfi.class, SetVlanId.class, SetVlanPcp.class, SwPath.class})
public abstract class ActionRTO {
	
    private static final Logger logger = LoggerFactory.getLogger(ActionRTO.class);


    @Override
    public int hashCode() {
        int result = 1;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
