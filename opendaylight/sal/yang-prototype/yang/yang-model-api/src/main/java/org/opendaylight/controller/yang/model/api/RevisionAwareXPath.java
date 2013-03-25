/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

public interface RevisionAwareXPath {
    
    
    /**
     * Returns <code>true</code> if the XPapth starts in root of Yang model, otherwise returns <code>false</cdoe>.
     * 
     * @return <code>true</code> if the XPapth starts in root of Yang model, otherwise returns <code>false</cdoe>
     */
    public boolean isAbsolute();
    
    /**
     * Returns the XPath formatted string as is defined in model. 
     * <br>
     * For example: /prefix:container/prefix:container::cond[when()=foo]/prefix:leaf
     * 
     * @return the XPath formatted string as is defined in model.
     */
    public String toString();
}
