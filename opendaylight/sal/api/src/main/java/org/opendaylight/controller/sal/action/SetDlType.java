/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.EtherTypes;

/**
 * Set ethertype/length field action
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetDlType extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "SET_DL_TYPE";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    @XmlElement
    private int dlType;

    public SetDlType() {
        super(NAME);
    }

    public SetDlType(int dlType) {
        super(NAME);
        this.dlType = dlType;
    }

    public SetDlType(EtherTypes dlType) {
        super(NAME);
        this.dlType = dlType.intValue();
    }

    /**
     * Returns the ethertype/lenght value that this action will set
     *
     * @return The datalayer type to be set
     */
    public int getDlType() {
        return dlType;
    }


    public String getDlTypeAtring() {
        return String.format("0x%s", Integer.toHexString(dlType));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SetDlType other = (SetDlType) obj;
        if (dlType != other.dlType) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + dlType;
        return result;
    }

    @Override
    public String toString() {
        return NAME + "=0x" + Integer.toHexString(dlType);
    }

    @Override
    public SetDlType fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            return new SetDlType(Integer.decode(matcher.group(1)));
        }
        return null;
    }
}
