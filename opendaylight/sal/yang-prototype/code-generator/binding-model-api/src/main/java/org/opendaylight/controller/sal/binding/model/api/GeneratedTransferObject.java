/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api;

import java.util.List;
import java.util.Set;

public interface GeneratedTransferObject extends Type {
    
    /**
     * Returns Set of all Enumerator definitions associated with interface.
     * 
     * @return Set of all Enumerator definitions associated with interface.
     */
    public List<Enumeration> getEnumDefintions();
    
    public List<GeneratedProperty> getProperties();
    
    public List<GeneratedProperty> getEqualsIdentifiers();
    
    public List<GeneratedProperty> getHashCodeIdentifiers();
    
    public List<GeneratedProperty> getToStringIdentifiers();
}
