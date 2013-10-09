package org.opendaylight.controller.md.sal.common.impl;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataChange;
import org.opendaylight.yangtools.concepts.Path;

public abstract class AbstractDataChange<P extends Path<P>, D> implements DataChange<P, D> {

    private final Map<P, D> createdCfg;
    private final Map<P, D> createdOperational;
    private final Set<P> removedCfg;
    private final Set<P> removedOperational;
    private final Map<P, D> updatedCfg;
    private final Map<P, D> updatedOperational;

    public AbstractDataChange(Map<P, D> createdCfg, Map<P, D> createdOperational, Set<P> removedCfg,
            Set<P> removedOperational, Map<P, D> updatedCfg, Map<P, D> updatedOperational) {
        this.createdCfg = createdCfg;
        this.createdOperational =  createdOperational;
        this.removedCfg =  (removedCfg);
        this.removedOperational =  (removedOperational);
        this.updatedCfg =  (updatedCfg);
        this.updatedOperational =  (updatedOperational);
    }

    @Override
    public final Map<P, D> getCreatedConfigurationData() {
        return this.createdCfg;
    }

    @Override
    public final Map<P, D> getCreatedOperationalData() {
        return this.createdOperational;
    }

    @Override
    public final Set<P> getRemovedConfigurationData() {
        return this.removedCfg;
    }

    @Override
    public final Set<P> getRemovedOperationalData() {
        return this.removedOperational;
    }

    @Override
    public final Map<P, D> getUpdatedConfigurationData() {
        return this.updatedCfg;
    }

    @Override
    public final Map<P, D> getUpdatedOperationalData() {
        return this.updatedOperational;
    }

}
