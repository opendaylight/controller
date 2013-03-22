
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * PortGroupConfig class represents the User's Configuration with a Opaque Regular Expression
 * String that is parsed and handled by PortGroupProvider.
 *
 * Typically, the opaque matchString will be a Regular Expression String supported by a particular
 * PortGroupProvider based on Customer requirements.
 *
 *
 *
 */
public class PortGroupConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String prettyFields[] = { "Name", "Match Criteria" };

    private String name;
    private String matchString;

    /**
     * Default Constructor with regular expression string defaulted to ".*"
     */
    public PortGroupConfig() {
        name = "default";
        matchString = ".*";
    }

    /**
     * Constructor to create a Port Group Configuration using a Group Name and an Opaque
     * String that is managed by PortGroupProvider.
     *
     * @param name Group Name representing a Port Group configuration
     * @param matchString An Opaque String managed by PortGroupProvider
     */
    public PortGroupConfig(String name, String matchString) {
        super();
        this.name = name;
        this.matchString = matchString;
    }

    /**
     * Returns the user configured PortGroup Configuration name.
     *
     * @return Configuration Name
     */
    public String getName() {
        return name;
    }

    /**
     * Assigns a name to the configuration
     * @param name configuration name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the Opaque string
     * @return
     */
    public String getMatchString() {
        return matchString;
    }

    /**
     * Assigns an opaque String to the Configuration.
     *
     * @param matchString Opaque string handled by PortGroupProvider
     */
    public void setMatchString(String matchString) {
        this.matchString = matchString;
    }

    /**
     * Returns the names of all the configurable fields in PortGroupConfig.
     * This method is typically used by NorthBound apis.
     *
     * @return List of Field names that can be configured.
     */
    public static List<String> getFieldsNames() {
        List<String> fieldList = new ArrayList<String>();
        for (Field fld : PortGroupConfig.class.getDeclaredFields()) {
            fieldList.add(fld.getName());
        }
        //remove static field(s)
        fieldList.remove(0);
        fieldList.remove(0);

        return fieldList;
    }

    /**
     * Returns the names of all the configurable fields in PortGroupConfig in human readable format for UI purposes.
     * This method is typically used by Web/UI apis.
     *
     * @return List of Human readable Strings that corresponds to the configurable field names.
     */
    public static List<String> getPrettyFieldsNames() {
        List<String> fieldList = new ArrayList<String>();
        for (String str : prettyFields) {
            fieldList.add(str);
        }
        return fieldList;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((matchString == null) ? 0 : matchString.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PortGroupConfig other = (PortGroupConfig) obj;
        if (matchString == null) {
            if (other.matchString != null)
                return false;
        } else if (!matchString.equals(other.matchString))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PortGroupConfig [name=" + name + ", matchString=" + matchString
                + "]";
    }
}
