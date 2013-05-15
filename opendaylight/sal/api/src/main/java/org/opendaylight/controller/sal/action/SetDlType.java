
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.utils.EtherType;

/**
 * Set ethertype/length field action
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SetDlType extends AbstractParameterAction<EtherType> {

    public SetDlType(EtherType dlType) {
        super(dlType);
    }

    /**
     * Returns the ethertype/lenght value that this action will set
     *
     * @return byte[]
     */
    @XmlElement
    @Deprecated
    public int getDlType() {
        return getValue().shortValue();
    }

    @Override
    public String toString() {
        return "setDlType" + "[dlType = 0x" + Integer.toHexString(getDlType()) + "]";
    }
}
