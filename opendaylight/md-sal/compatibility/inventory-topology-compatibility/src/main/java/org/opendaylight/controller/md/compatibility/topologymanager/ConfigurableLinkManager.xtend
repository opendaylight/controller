/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.compatibility.topologymanager

import org.opendaylight.controller.topologymanager.ITopologyManager
import org.opendaylight.controller.topologymanager.TopologyUserLinkConfig

abstract class ConfigurableLinkManager implements ITopologyManager {
    
    final override addUserLink(TopologyUserLinkConfig link) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
        
    }
    
    
    final override deleteUserLink(String linkName) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
        
    }
    
    
    final override getUserLinks() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
        
    }
}
