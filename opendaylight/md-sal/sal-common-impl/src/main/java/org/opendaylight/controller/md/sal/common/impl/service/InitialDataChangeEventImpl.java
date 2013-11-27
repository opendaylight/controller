package org.opendaylight.controller.md.sal.common.impl.service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;

public class InitialDataChangeEventImpl<P,D> implements DataChangeEvent<P, D> {

    private final D originalOperationalTree;
    private final D originalConfigurationTree;

    public InitialDataChangeEventImpl(D configTree, D operTree) {
        originalConfigurationTree = configTree;
        originalOperationalTree = operTree;
    }
    
    @Override
    public Map<P, D> getCreatedConfigurationData() {
        return Collections.emptyMap();
    }
    
    @Override
    public Map<P, D> getCreatedOperationalData() {
        return Collections.emptyMap();
    }
    
    @Override
    public Map<P, D> getOriginalConfigurationData() {
        return Collections.emptyMap();
    }
    @Override
    public Map<P, D> getOriginalOperationalData() {
        return Collections.emptyMap();
    }
    @Override
    public Set<P> getRemovedConfigurationData() {
        return Collections.emptySet();
    }
    @Override
    public Set<P> getRemovedOperationalData() {
        return Collections.emptySet();
    }
    @Override
    public Map<P, D> getUpdatedConfigurationData() {
        return Collections.emptyMap();
    }
    
    @Override
    public D getUpdatedConfigurationSubtree() {
        return originalConfigurationTree;
    }
    @Override
    public D getUpdatedOperationalSubtree() {
        return originalOperationalTree;
    }
    
    @Override
    public D getOriginalConfigurationSubtree() {
        return originalConfigurationTree;
    }
    
    @Override
    public D getOriginalOperationalSubtree() {
        return originalOperationalTree;
    }
    
    @Override
    public Map<P, D> getUpdatedOperationalData() {
        return Collections.emptyMap();
    }
    

}
