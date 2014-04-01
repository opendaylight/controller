/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Node;

/**
 * Represents the generic action to be applied to the matched
 * frame/packet/message. Any existing or new SAL action class
 * must extend this abstract Action class
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public abstract class Action implements Serializable {
    private static final long serialVersionUID = 1L;
    protected final String name;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private Action() {
        name = "none";
    }

    public Action(String action) {
        name = action;
    }

    /**
     * Generate an Action instance from its string form
     *
     * @param actionString
     *            The action in string form
     * @return The corresponding Action instance
     */
    public abstract Action fromString(String actionString, Node node);

    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns the name of this action
     *
     * @return The name of this action
     */
    @XmlElement(name = "type")
    public String getName() {
        return name;
    }

    /**
     * Returns whether the Action parameters are or are not valid
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Action)) {
            return false;
        }
        Action other = (Action) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    protected static String removeSpaces(String target) {
        return (target == null) ? null : target.replace(" ", "");
    }
}
