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
 * Set network TOS action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetNwTos extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "SET_NW_TOS";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    private static int MIN = 0;
    private static int MAX = 0x3F;
    @XmlElement
    private int tos;

    public SetNwTos() {
        super(NAME);
    }

    public SetNwTos(int tos) {
        super(NAME);
        this.tos = tos;
    }

    /**
     * Returns the network TOS value which the action will set
     *
     * @return int
     */
    public int getNwTos() {
        return tos;
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
        SetNwTos other = (SetNwTos) obj;
        if (tos != other.tos) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + tos;
        return result;
    }

    @Override
    public String toString() {
        return NAME + "=0x" + Integer.toHexString(tos);
    }

    @Override
    public SetNwTos fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            try {
                Integer tos = Integer.decode(matcher.group(1));
                if (tos != null) {
                    return new SetNwTos(tos);
                }
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return (tos >= MIN) && (tos <= MAX);
    }
}
