/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.Path;

public class InitialDataChangeEventImpl<P extends Path<P>,D> implements DataChangeEvent<P, D>, Immutable {

    private final D updatedOperationalTree;
    private final D updatedConfigurationTree;
    private final Map<P,D> updatedConfigurationData;
    private final Map<P,D> updatedOperationalData;

    public InitialDataChangeEventImpl(D configTree, D operTree) {
        updatedConfigurationTree = configTree;
        updatedOperationalTree = operTree;
        updatedConfigurationData = Collections.emptyMap();
        updatedOperationalData = Collections.emptyMap();
    }
    
    public InitialDataChangeEventImpl(D configTree, D operTree, Map<P, D> updatedCfgData, Map<P, D> updatedOperData) {
        updatedConfigurationTree = configTree;
        updatedOperationalTree = operTree;
        updatedConfigurationData = updatedCfgData;
        updatedOperationalData = updatedOperData;
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
        return updatedConfigurationData;
    }
    
    @Override
    public D getUpdatedConfigurationSubtree() {
        return updatedConfigurationTree;
    }
    @Override
    public D getUpdatedOperationalSubtree() {
        return updatedOperationalTree;
    }
    
    @Override
    public D getOriginalConfigurationSubtree() {
        return updatedConfigurationTree;
    }
    
    @Override
    public D getOriginalOperationalSubtree() {
        return updatedOperationalTree;
    }
    
    @Override
    public Map<P, D> getUpdatedOperationalData() {
        return updatedOperationalData;
    }
    

}
