/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;

/**
 * The <code>default</code> implementation of Instance Rewision Aware XPath interface.
 * 
 * @see RevisionAwareXPath
 */
public class RevisionAwareXPathImpl implements RevisionAwareXPath {

    private final String xpath;
    private final boolean absolute;

    public RevisionAwareXPathImpl(String xpath, boolean absolute) {
        this.xpath = xpath;
        this.absolute = absolute;
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((xpath == null) ? 0 : xpath.hashCode());
        result = prime * result + (absolute ? 1231 : 1237);
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
        RevisionAwareXPathImpl other = (RevisionAwareXPathImpl) obj;
        if (xpath == null) {
            if (other.xpath != null) {
                return false;
            }
        } else if (!xpath.equals(other.xpath)) {
            return false;
        }
        if (absolute != other.absolute) {
            return false;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return xpath;
    }
}
