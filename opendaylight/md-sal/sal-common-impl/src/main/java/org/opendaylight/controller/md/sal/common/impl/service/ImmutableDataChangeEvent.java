package org.opendaylight.controller.md.sal.common.impl.service;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.concepts.Path;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Deprecated
public final class ImmutableDataChangeEvent<P extends Path<P>, D> implements DataChangeEvent<P,D> {

    private final D updatedOperationalSubtree;
    private final Map<P, D> updatedOperational;
    private final D updatedConfigurationSubtree;
    private final Map<P, D> updatedConfiguration;
    private final Set<P> removedOperational;
    private final Set<P> removedConfiguration;
    private final D originalOperationalSubtree;
    private final Map<P, D> originalOperational;
    private final D originalConfigurationSubtree;
    private final Map<P, D> originalConfiguration;
    private final Map<P, D> createdOperational;
    private final Map<P, D> createdConfiguration;


    private ImmutableDataChangeEvent(final Builder<P, D> builder) {

        createdConfiguration = builder.getCreatedConfiguration().build();
        createdOperational = builder.getCreatedOperational().build();
        originalConfiguration = builder.getOriginalConfiguration().build();
        originalConfigurationSubtree = builder.getOriginalConfigurationSubtree();
        originalOperational = builder.getOriginalOperational().build();
        originalOperationalSubtree = builder.getOriginalOperationalSubtree();
        removedConfiguration = builder.getRemovedConfiguration().build();
        removedOperational = builder.getRemovedOperational().build();
        updatedConfiguration = builder.getUpdatedConfiguration().build();
        updatedConfigurationSubtree = builder.getUpdatedConfigurationSubtree();
        updatedOperational = builder.getUpdatedOperational().build();
        updatedOperationalSubtree = builder.getUpdatedOperationalSubtree();
    }

    @Override
    public Map<P, D> getCreatedConfigurationData() {
        return createdConfiguration;
    }

    @Override
    public Map<P, D> getCreatedOperationalData() {
        return createdOperational;
    }

    @Override
    public Map<P, D> getOriginalConfigurationData() {
        return originalConfiguration;
    }
    @Override
    public D getOriginalConfigurationSubtree() {
        return originalConfigurationSubtree;
    }
    @Override
    public Map<P, D> getOriginalOperationalData() {
        return originalOperational;
    }
    @Override
    public D getOriginalOperationalSubtree() {
        return originalOperationalSubtree;
    }
    @Override
    public Set<P> getRemovedConfigurationData() {
        return removedConfiguration;
    }
    @Override
    public Set<P> getRemovedOperationalData() {
        return removedOperational;
    }
    @Override
    public Map<P, D> getUpdatedConfigurationData() {
        return updatedConfiguration;
    }
    @Override
    public D getUpdatedConfigurationSubtree() {
        return updatedConfigurationSubtree;
    }
    @Override
    public Map<P, D> getUpdatedOperationalData() {
        return updatedOperational;
    }
    @Override
    public D getUpdatedOperationalSubtree() {
        return updatedOperationalSubtree;
    }

    public static final <P extends Path<P>,D> Builder<P, D> builder() {
        return new Builder<>();
    }

    public static final class Builder<P extends Path<P>,D> {

        private  D updatedOperationalSubtree;
        private  D originalOperationalSubtree;
        private  D originalConfigurationSubtree;
        private  D updatedConfigurationSubtree;

        private final ImmutableMap.Builder<P, D> updatedOperational = ImmutableMap.builder();
        private final ImmutableMap.Builder<P, D> updatedConfiguration = ImmutableMap.builder();
        private final ImmutableSet.Builder<P> removedOperational = ImmutableSet.builder();
        private final ImmutableSet.Builder<P> removedConfiguration = ImmutableSet.builder();
        private final ImmutableMap.Builder<P, D> originalOperational = ImmutableMap.builder();

        private final ImmutableMap.Builder<P, D> originalConfiguration = ImmutableMap.builder();
        private final ImmutableMap.Builder<P, D> createdOperational = ImmutableMap.builder();
        private final ImmutableMap.Builder<P, D> createdConfiguration = ImmutableMap.builder();





        protected Builder<P,D> addTransaction(final DataModification<P, D> data, final Predicate<P> keyFilter) {
            updatedOperational.putAll(Maps.filterKeys(data.getUpdatedOperationalData(), keyFilter));
            updatedConfiguration.putAll(Maps.filterKeys(data.getUpdatedConfigurationData(), keyFilter));
            originalConfiguration.putAll(Maps.filterKeys(data.getOriginalConfigurationData(), keyFilter));
            originalOperational.putAll(Maps.filterKeys(data.getOriginalOperationalData(), keyFilter));
            createdOperational.putAll(Maps.filterKeys(data.getCreatedOperationalData(), keyFilter));
            createdConfiguration.putAll(Maps.filterKeys(data.getCreatedConfigurationData(), keyFilter));
            removedOperational.addAll(Sets.filter(data.getRemovedOperationalData(), keyFilter));
            removedConfiguration.addAll(Sets.filter(data.getRemovedConfigurationData(), keyFilter));
            return this;
        }

        protected Builder<P, D> addConfigurationChangeSet(final RootedChangeSet<P, D> changeSet) {
            if(changeSet == null) {
                return this;
            }

            originalConfiguration.putAll(changeSet.getOriginal());
            createdConfiguration.putAll(changeSet.getCreated());
            updatedConfiguration.putAll(changeSet.getUpdated());
            removedConfiguration.addAll(changeSet.getRemoved());
            return this;
        }

        protected Builder<P, D> addOperationalChangeSet(final RootedChangeSet<P, D> changeSet) {
            if(changeSet == null) {
                return this;
            }
            originalOperational.putAll(changeSet.getOriginal());
            createdOperational.putAll(changeSet.getCreated());
            updatedOperational.putAll(changeSet.getUpdated());
            removedOperational.addAll(changeSet.getRemoved());
            return this;
        }

        public ImmutableDataChangeEvent<P, D> build() {
            return new ImmutableDataChangeEvent<P,D>(this);
        }

        protected D getUpdatedOperationalSubtree() {
            return updatedOperationalSubtree;
        }

        public Builder<P, D> setUpdatedOperationalSubtree(final D updatedOperationalSubtree) {
            this.updatedOperationalSubtree = updatedOperationalSubtree;
            return this;
        }

        protected D getOriginalOperationalSubtree() {
            return originalOperationalSubtree;
        }

        public Builder<P,D> setOriginalOperationalSubtree(final D originalOperationalSubtree) {
            this.originalOperationalSubtree = originalOperationalSubtree;
            return this;
        }

        protected D getOriginalConfigurationSubtree() {
            return originalConfigurationSubtree;
        }

        public Builder<P, D> setOriginalConfigurationSubtree(final D originalConfigurationSubtree) {
            this.originalConfigurationSubtree = originalConfigurationSubtree;
            return this;
        }

        protected D getUpdatedConfigurationSubtree() {
            return updatedConfigurationSubtree;
        }

        public Builder<P,D> setUpdatedConfigurationSubtree(final D updatedConfigurationSubtree) {
            this.updatedConfigurationSubtree = updatedConfigurationSubtree;
            return this;
        }

        protected ImmutableMap.Builder<P, D> getUpdatedOperational() {
            return updatedOperational;
        }

        protected ImmutableMap.Builder<P, D> getUpdatedConfiguration() {
            return updatedConfiguration;
        }

        protected ImmutableSet.Builder<P> getRemovedOperational() {
            return removedOperational;
        }

        protected ImmutableSet.Builder<P> getRemovedConfiguration() {
            return removedConfiguration;
        }

        protected ImmutableMap.Builder<P, D> getOriginalOperational() {
            return originalOperational;
        }

        protected ImmutableMap.Builder<P, D> getOriginalConfiguration() {
            return originalConfiguration;
        }

        protected ImmutableMap.Builder<P, D> getCreatedOperational() {
            return createdOperational;
        }

        protected ImmutableMap.Builder<P, D> getCreatedConfiguration() {
            return createdConfiguration;
        }

        public Builder<P,D> putOriginalOperational(final Map<? extends P, ? extends D> originalData) {
            originalOperational.putAll(originalData);
            return this;
        }

        public Builder<P,D> putCreatedOperational(final Map<? extends P, ? extends D> originalData) {
            createdOperational.putAll(originalData);
            return this;
        }

        public Builder<P,D> putUpdatedOperational(final Map<? extends P, ? extends D> originalData) {
            updatedOperational.putAll(originalData);
            return this;
        }

        public Builder<P,D> putRemovedOperational(final Set<? extends P> originalData) {
            removedOperational.addAll(originalData);
            return this;
        }
    }

}
