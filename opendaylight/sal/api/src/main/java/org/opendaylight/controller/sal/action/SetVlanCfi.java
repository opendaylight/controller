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
 * Set vlan CFI action
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetVlanCfi extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "SET_VLAN_CFI";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    private static int MIN = 0;
    private static int MAX = 1;
    @XmlElement
    private int cfi;

    public SetVlanCfi() {
        super(NAME);
    }

    public SetVlanCfi(int cfi) {
        super(NAME);
        this.cfi = cfi;
       }

    /**
     * Returns the 802.1q CFI value that this action will set
     *
     * @return
     */
    public int getCfi() {
        return cfi;
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
        SetVlanCfi other = (SetVlanCfi) obj;
        if (cfi != other.cfi) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + cfi;
        return result;
    }

    @Override
    public String toString() {
        return NAME + "=" + cfi;
    }

    @Override
    public SetVlanCfi fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            try {
                Integer cfi = Integer.decode(matcher.group(1));
                if (cfi != null) {
                    return new SetVlanCfi(cfi);
                }
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return (cfi >= MIN) && (cfi <= MAX);
    }
}