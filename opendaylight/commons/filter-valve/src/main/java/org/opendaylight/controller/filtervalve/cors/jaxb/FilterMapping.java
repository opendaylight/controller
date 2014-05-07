/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.filtervalve.cors.jaxb;

import static com.google.common.base.Preconditions.checkArgument;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FilterMapping {
    private String filterName;
    private String urlPattern;
    private boolean initialized;

    @XmlElement(name = "filter-name")
    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        checkArgument(initialized == false, "Already initialized");
        this.filterName = filterName;
    }

    @XmlElement(name = "url-pattern")
    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        checkArgument(initialized == false, "Already initialized");
        this.urlPattern = urlPattern;
    }

    public synchronized void initialize() {
        checkArgument(initialized == false, "Already initialized");
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
