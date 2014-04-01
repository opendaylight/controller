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

/**
 * Set vlan id action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetVlanId extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "SET_VLAN_ID";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    private static int MIN = 1;
    private static int MAX = 0xFFF;
    @XmlElement
    private int vlanId;

    public SetVlanId() {
        super(NAME);
    }

    public SetVlanId(int vlanId) {
        super(NAME);
        this.vlanId = vlanId;
    }

    /**
     * Returns the vlan id this action will set
     *
     * @return int
     */
    public int getVlanId() {
        return vlanId;
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
        SetVlanId other = (SetVlanId) obj;
        if (vlanId != other.vlanId) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + vlanId;
        return result;
    }

    @Override
    public String toString() {
        return NAME + "=" + vlanId;
    }

    @Override
    public SetVlanId fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            try {
                Integer id = Integer.decode(matcher.group(1));
                if (id != null) {
                    return new SetVlanId(id);
                }
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return (vlanId >= MIN) && (vlanId <= MAX);
    }
}
