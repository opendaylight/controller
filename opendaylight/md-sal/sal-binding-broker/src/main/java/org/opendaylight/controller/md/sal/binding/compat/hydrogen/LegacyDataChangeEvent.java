/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat.hydrogen;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Deprecated
public abstract class LegacyDataChangeEvent implements
        DataChangeEvent<InstanceIdentifier<? extends DataObject>, DataObject> {

    private LegacyDataChangeEvent() {
    }

    public static final DataChangeEvent<InstanceIdentifier<?>, DataObject> createOperational(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        return new OperationalChangeEvent(change);
    }

    public static final DataChangeEvent<InstanceIdentifier<?>, DataObject> createConfiguration(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        return new ConfigurationChangeEvent(change);
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getCreatedOperationalData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getCreatedConfigurationData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getUpdatedOperationalData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getUpdatedConfigurationData() {
        return Collections.emptyMap();
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedConfigurationData() {
        return Collections.emptySet();
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedOperationalData() {
        return Collections.emptySet();
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getOriginalConfigurationData() {
        return Collections.emptyMap();
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getOriginalOperationalData() {
        return Collections.emptyMap();
    }

    @Override
    public DataObject getOriginalConfigurationSubtree() {
        return null;
    }

    @Override
    public DataObject getOriginalOperationalSubtree() {
        return null;
    }

    @Override
    public DataObject getUpdatedConfigurationSubtree() {
        return null;
    }

    @Override
    public DataObject getUpdatedOperationalSubtree() {
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private final static class OperationalChangeEvent extends LegacyDataChangeEvent {

        private final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> delegate;
        private Map<InstanceIdentifier<?>, DataObject> updatedCache;

        public OperationalChangeEvent(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            this.delegate = change;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getCreatedOperationalData() {
            return delegate.getCreatedData();
        }

        @Override
        public Set<InstanceIdentifier<?>> getRemovedOperationalData() {
            return delegate.getRemovedPaths();
        }

        @Override
        public DataObject getOriginalOperationalSubtree() {
            return delegate.getOriginalSubtree();
        }

        @Override
        public DataObject getUpdatedOperationalSubtree() {
            return delegate.getUpdatedSubtree();
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getOriginalOperationalData() {
            return (Map) delegate.getOriginalData();
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getUpdatedOperationalData() {
            if(updatedCache == null) {
                Map<InstanceIdentifier<?>, DataObject> created = delegate.getCreatedData();
                Map<InstanceIdentifier<?>, DataObject> updated = delegate.getUpdatedData();
                HashMap<InstanceIdentifier<?>, DataObject> updatedComposite = new HashMap<>(created.size() + updated.size());
                updatedComposite.putAll(created);
                updatedComposite.putAll(updated);
                updatedCache = Collections.unmodifiableMap(updatedComposite);
            }
            return updatedCache;
        }

        @Override
        public String toString() {
            return "OperationalChangeEvent [delegate=" + delegate + "]";
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private final static class ConfigurationChangeEvent extends LegacyDataChangeEvent {

        private final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> delegate;
        private Map<InstanceIdentifier<?>, DataObject> updatedCache;

        public ConfigurationChangeEvent(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            this.delegate = change;
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getCreatedConfigurationData() {
            return delegate.getCreatedData();
        }

        @Override
        public Set<InstanceIdentifier<?>> getRemovedConfigurationData() {
            return delegate.getRemovedPaths();
        }

        @Override
        public DataObject getOriginalConfigurationSubtree() {
            return delegate.getOriginalSubtree();
        }

        @Override
        public DataObject getUpdatedConfigurationSubtree() {
            return delegate.getUpdatedSubtree();
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getOriginalConfigurationData() {
            return (Map) delegate.getOriginalData();
        }

        @Override
        public Map<InstanceIdentifier<?>, DataObject> getUpdatedConfigurationData() {
            if(updatedCache == null) {
                Map<InstanceIdentifier<?>, DataObject> created = delegate.getCreatedData();
                Map<InstanceIdentifier<?>, DataObject> updated = delegate.getUpdatedData();
                HashMap<InstanceIdentifier<?>, DataObject> updatedComposite = new HashMap<>(created.size() + updated.size());
                updatedComposite.putAll(created);
                updatedComposite.putAll(updated);
                updatedCache = Collections.unmodifiableMap(updatedComposite);
            }
            return updatedCache;
        }

        @Override
        public String toString() {
            return "ConfigurationChangeEvent [delegate=" + delegate + "]";
        }

    }

}
