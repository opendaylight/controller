package org.opendaylight.controller.sal.restconf.impl

import org.opendaylight.yangtools.yang.data.api.CompositeNode

class RestconfImpl implements RestconfService {
    
    @Property
    BrokerFacade broker;
    
    override readAllData() {
        return broker.readOperationalData("");
    }
    
    
    override getModules() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
    override getRoot() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
        
    }
    
    override readData(String identifier) {
        return broker.readOperationalData(identifier);
    }
    
    override createConfigurationData(String identifier, CompositeNode payload) {
        return broker.commitConfigurationDataCreate(identifier,payload);
    }
    
    
    override updateConfigurationData(String identifier, CompositeNode payload) {
        return broker.commitConfigurationDataCreate(identifier,payload);
    }
    
    override invokeRpc(String identifier, CompositeNode payload) {
        return broker.invokeRpc(identifier,payload);
    }
    
}