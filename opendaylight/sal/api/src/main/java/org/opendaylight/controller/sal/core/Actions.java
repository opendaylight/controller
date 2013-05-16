
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @file   Actions.java
 *
 * @brief  Class representing actions
 *
 * Describes supported actions
 */

@XmlRootElement
public class Actions extends Property {
	private static final long serialVersionUID = 1L;
    @XmlElement
    private int actionsValue;
    
    public enum ActionType { 
    	OUTPUT_PORT_ACTION(1<<0),
    	VLAN_VID_ACTION(1<<1),
    	VLAN_PCP_ACTION(1<<2),
    	VLAN_STRIP_ACTION(1<<3),
    	DLSRC_ACTION(1<<4),
    	DLDST_ACTION(1<<5),
    	NWSRC_ACTION(1<<6),
    	NWDST_ACTION(1<<7),
    	NWTOS_ACTION(1<<8),
    	TPTSRC_ACTION(1<<9),
    	TPTDST_ACTION(1<<10),
    	ENQUEUE_ACTION(1<<11),
    	VENDOR_ACTION(0xffff);
    	private final int at;
    	ActionType(int val) {
    		this.at = val;
    	}
    	public int getValue() {
    		return at;
    	}
    }
    
    public static final String ActionsPropName = "actions";
    /**
     * Construct a actions property
     *
     * @param actions the actions value
     * @return Constructed object
     */
    public Actions(int actions) {
        super(ActionsPropName);
        this.actionsValue = actions;
    }

    /*
     * Private constructor used for JAXB mapping
     */
    private Actions() {
        super(ActionsPropName);
        this.actionsValue = 0;
    }

    public Actions clone() {
        return new Actions(this.actionsValue);
    }
    
    public int getValue() {
    	return this.actionsValue;
    }
    
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + actionsValue;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Actions other = (Actions) obj;
        if (actionsValue != other.actionsValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Actions[" + actionsValue + "]";
    }
}
