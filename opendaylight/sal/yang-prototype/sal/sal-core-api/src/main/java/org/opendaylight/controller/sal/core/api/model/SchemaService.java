/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.model;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;

public interface SchemaService extends BrokerService {

    /**
     * Registers a YANG module to session and global context 
     * 
     * @param module
     */
    void addModule(Module module);
    
    /**
     * Unregisters a YANG module from session context
     * 
     * @param module
     */
    void removeModule(Module module);
    
    /**
     * Returns session specific YANG schema context
     * @return
     */
    SchemaContext getSessionContext();
    
    /**
     * Returns global schema context
     * 
     * @return
     */
    SchemaContext getGlobalContext();
}
