
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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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
public abstract class Action {
    private static final Logger logger = LoggerFactory.getLogger(Action.class);
    private static boolean debug = false; // Enable to find where in the code an invalid assignment is made
    @XmlTransient
    protected ActionType type;
    private transient boolean isValid = true;

    /* Dummy constructor for JAXB */
    public Action () {
    }

    /*
    public Action (ActionType type, Object value) {
    	this.type = type;
    	this.value = value;
    	this.isValid = true;
    } */

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
     * Checks if the passed value is in the valid range for the passed action type
     * This method is used for complex Action types which are
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
        String error = "Invalid field value assignement. For type: "
                + type.getId() + " Expected: " + type.getRange() + ", Got: 0x"
                + Integer.toHexString(value);
        try {
            throw new Exception(error);
        } catch (Exception e) {
            logger.error(e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
        }
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
     * @return	boolean
     */
    public boolean isValid() {
        return isValid;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return type.toString();
    }

}
