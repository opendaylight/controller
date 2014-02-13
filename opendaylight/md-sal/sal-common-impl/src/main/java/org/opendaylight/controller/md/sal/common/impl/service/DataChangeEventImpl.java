/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.service;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataChange;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.Path;

public class DataChangeEventImpl<P extends Path<P>, D> implements DataChangeEvent<P, D>, Immutable {

    private final DataChange<P, D> dataChange;

    private final D originalConfigurationSubtree;
    private final D originalOperationalSubtree;
    private final D updatedOperationalSubtree;
    private final D updatedConfigurationSubtree;

    
    
    
    public DataChangeEventImpl(DataChange<P, D> dataChange, D originalConfigurationSubtree,
            D originalOperationalSubtree, D updatedOperationalSubtree, D updatedConfigurationSubtree) {
        super();
        this.dataChange = dataChange;
        this.originalConfigurationSubtree = originalConfigurationSubtree;
        this.originalOperationalSubtree = originalOperationalSubtree;
        this.updatedOperationalSubtree = updatedOperationalSubtree;
        this.updatedConfigurationSubtree = updatedConfigurationSubtree;
    }

    @Override
    public D getOriginalConfigurationSubtree() {
        return originalConfigurationSubtree;
    }

    @Override
    public D getOriginalOperationalSubtree() {
        return originalOperationalSubtree;
    }

    @Override
    public D getUpdatedOperationalSubtree() {
        return updatedOperationalSubtree;
    }

    @Override
    public D getUpdatedConfigurationSubtree() {
        return updatedConfigurationSubtree;
    }

    public Map<P, D> getCreatedOperationalData() {
        return dataChange.getCreatedOperationalData();
    }

    public Map<P, D> getCreatedConfigurationData() {
        return dataChange.getCreatedConfigurationData();
    }

    public Map<P, D> getUpdatedOperationalData() {
        return dataChange.getUpdatedOperationalData();
    }

    public Map<P, D> getUpdatedConfigurationData() {
        return dataChange.getUpdatedConfigurationData();
    }

    public Set<P> getRemovedConfigurationData() {
        return dataChange.getRemovedConfigurationData();
    }

    public Set<P> getRemovedOperationalData() {
        return dataChange.getRemovedOperationalData();
    }

    public Map<P, D> getOriginalConfigurationData() {
        return dataChange.getOriginalConfigurationData();
    }

    public Map<P, D> getOriginalOperationalData() {
        return dataChange.getOriginalOperationalData();
    }

}
