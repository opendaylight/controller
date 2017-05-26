/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.filtervalve.cors.jaxb;

import static com.google.common.base.Preconditions.checkState;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InitParam {
    private String paramName;
    private String paramValue;
    private boolean initialized;

    public synchronized void inititialize() {
        checkState(initialized == false, "Already initialized");
        initialized = true;
    }

    @XmlElement(name = "param-name")
    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    @XmlElement(name = "param-value")
    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String toString() {
        return "{" + paramName + '=' + paramValue + "}";
    }
}
