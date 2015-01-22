/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The class represents Admin Config status
 *
 *
 */
@XmlRootElement
@SuppressWarnings("serial")
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class Config extends Property {
    @XmlElement(name="value")
    private short configValue;

    public static final short ADMIN_DOWN = 0;
    public static final short ADMIN_UP = 1;
    public static final short ADMIN_UNDEF = 0x7fff;
    public static final String ConfigPropName = "config";

    /*
     * Private constructor used for JAXB mapping
     */
    private Config() {
        super(ConfigPropName);
        this.configValue = ADMIN_UNDEF;
    }

    public Config(short config) {
        super(ConfigPropName);
        this.configValue = config;
    }

    @Override
    public Config clone() {
        return new Config(this.configValue);
    }

    public short getValue() {
        return this.configValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + configValue;
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
        Config other = (Config) obj;
        if (configValue != other.configValue)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Config["+ configValue +"]";
    }

    @Override
    public String getStringValue() {
        if (configValue == 0) {
            return "ADMIN_DOWN";
        } else if (configValue == 1) {
            return "ADMIN_UP";
        } else if (configValue == 0x7fff) {
            return "ADMIN_UNDEF";
        } else {
            return String.valueOf(configValue);
        }
    }
}
