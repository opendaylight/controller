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
 * Set source transport port action
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetTpSrc extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "SET_TP_SRC";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=(.*)", Pattern.CASE_INSENSITIVE);
    private static int MIN = 1;
    private static int MAX = 0xFFFF;
    @XmlElement
    private int port;

    public SetTpSrc() {
        super(NAME);
    }

    public SetTpSrc(int port) {
        super(NAME);
        this.port = port;
    }

    /**
     * Returns the transport port the action will set
     *
     * @return
     */
    public int getPort() {
        return port;
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
        SetTpSrc other = (SetTpSrc) obj;
        if (port != other.port) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + port;
        return result;
    }

    @Override
    public String toString() {
        return NAME + "=" + port;
    }

    @Override
    public SetTpSrc fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            try {
                Integer port = Integer.decode(matcher.group(1));
                if (port != null) {
                    return new SetTpSrc(port);
                }
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return (port >= MIN) && (port <= MAX);
    }
}
