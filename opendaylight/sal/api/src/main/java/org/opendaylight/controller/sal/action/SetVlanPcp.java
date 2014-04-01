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
 * Set vlan PCP action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetVlanPcp extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "SET_VLAN_PCP";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    private static int MIN = 0;
    private static int MAX = 7;
    @XmlElement
    private int pcp;

    public SetVlanPcp() {
        super(NAME);
    }

    public SetVlanPcp(int pcp) {
        super(NAME);
        this.pcp = pcp;
    }

    /**
     * Returns the value of the vlan PCP this action will set
     *
     * @return int
     */
    public int getPcp() {
        return pcp;
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
        SetVlanPcp other = (SetVlanPcp) obj;
        if (pcp != other.pcp) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + pcp;
        return result;
    }

    @Override
    public String toString() {
        return NAME + "=" + pcp;
    }

    @Override
    public SetVlanPcp fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            try {
                Integer pcp = Integer.decode(matcher.group(1));
                if (pcp != null) {
                    return new SetVlanPcp(pcp);
                }
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return (pcp >= MIN) && (pcp <= MAX);
    }
}
