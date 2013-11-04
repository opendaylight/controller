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