/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.sal.core.Property;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Represents the generic action to be applied to the matched
 * frame/packet/message
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public abstract class Action implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(Action.class);
    private static boolean debug = false; // Enable to find where in the code an
    // invalid assignment is made
    @XmlElement
    protected ActionType type;
    private transient boolean isValid = true;
    private ConcurrentMap<String, Property> props;

    /* Dummy constructor for JAXB */
    public Action() {
    }

    /**
     * Checks if the passed value is in the valid range for this action
     *
     * @param value
     * @return boolean
     */
    protected void checkValue(int value) {
        if (type.isValidTarget(value) == false) {
            isValid = false;
            throwValueException(value);
        }
    }

    /**
     * Checks if the passed value is in the valid range for the passed action
     * type This method is used for complex Action types which are
     *
     * @param value
     * @return boolean
     */
    protected void checkValue(ActionType type, int value) {
        if (type.isValidTarget(value) == false) {
            isValid = false;
            throwValueException(value);
        }
    }

    /**
     * Throw and handle the invalid value exception
     *
     * @param value
     * @return void
     */
    private void throwValueException(int value) {
        String error = "Invalid field value assignement. For type: " + type.getId() + " Expected: " + type.getRange()
                + ", Got: 0x" + Integer.toHexString(value);
        try {
            throw new Exception(error);
        } catch (Exception e) {
            logger.error(e.getMessage());
            if (debug) {
                logger.error("", e);
            }
        }
    }

    /**
     * Gets the list of metadata currently registered with this match
     *
     * @return List of metadata currently registered
     */
    public List <Property> getMetadatas() {
        if (this.props != null) {
            // Return all the values in the map
            Collection res = this.props.values();
            if (res == null) {
                return Collections.emptyList();
            }
            return new ArrayList<Property>(res);
        }
        return Collections.emptyList();
    }

    /**
     * Gets the metadata registered with a name if present
     *
     * @param name the name of the property to be extracted
     *
     * @return List of metadata currently registered
     */
    public Property getMetadata(String name) {
        if (name == null) {
            return null;
        }
        if (this.props != null) {
            // Return the Property associated to the name
            return this.props.get(name);
        }
        return null;
    }

    /**
     * Sets the metadata associated to a name. If the name or prop is NULL,
     * an exception NullPointerException will be raised.
     *
     * @param name the name of the property to be set
     * @param prop, property to be set
     */
    public void setMetadata(String name, Property prop) {
        if (this.props == null) {
            props = new ConcurrentHashMap<String, Property>();
        }

        if (this.props != null) {
            this.props.put(name, prop);
        }
    }

    /**
     * Remove the metadata associated to a name. If the name is NULL,
     * nothing will be removed.
     *
     * @param name the name of the property to be set
     * @param prop, property to be set
     *
     * @return List of metadata currently registered
     */
    public void removeMetadata(String name) {
        if (this.props == null) {
            return;
        }

        if (this.props != null) {
            this.props.remove(name);
        }
        // It's intentional to keep the this.props still allocated
        // till the parent data structure will be alive, so to avoid
        // unnecessary allocation/deallocation, even if it's holding
        // nothing
    }

    /**
     * Returns the type of this action
     *
     * @return ActionType
     */
    public ActionType getType() {
        return type;
    }

    /**
     * Returns the id of this action
     *
     * @return String
     */
    public String getId() {
        return type.getId();
    }

    /**
     * Returns whether the Action is valid or not
     *
     * @return boolean
     */
    public boolean isValid() {
        return isValid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.calculateConsistentHashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        Action other = (Action) obj;
        if (type != other.type) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return type.toString();
    }

}
