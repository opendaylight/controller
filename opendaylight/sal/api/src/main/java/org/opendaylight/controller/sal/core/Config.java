
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

/**
 * The class represents Admin Config status
 *
 *
 */
@XmlRootElement
@SuppressWarnings("serial")
public class Config extends Property {
    @XmlElement
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

    public Config clone() {
        return new Config(this.configValue);
    }

    public short getValue() {
        return this.configValue;
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
        return "Config[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
