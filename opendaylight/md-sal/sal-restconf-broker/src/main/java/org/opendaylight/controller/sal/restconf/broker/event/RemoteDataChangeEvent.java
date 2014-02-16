/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.DataChangedNotification;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@ThreadSafe
public class RemoteDataChangeEvent implements DataChangeEvent<InstanceIdentifier<? extends DataObject>,DataObject> {
    private final Map<InstanceIdentifier<?>, DataObject> createdConfig, createdOper, origConfig, origOper, updatedConfig, updatedOper;
    private final Set<InstanceIdentifier<?>> removedConfig, removedOper;

    public RemoteDataChangeEvent(DataChangedNotification dataChangedNotification) {
        final Map<InstanceIdentifier<?>, DataObject> createdConfig = new HashMap<>();
        final Map<InstanceIdentifier<?>, DataObject> createdOper = new HashMap<>();
        final Map<InstanceIdentifier<?>, DataObject> origConfig = new HashMap<>();
        final Map<InstanceIdentifier<?>, DataObject> origOper = new HashMap<>();
        final Map<InstanceIdentifier<?>, DataObject> updatedConfig = new HashMap<>();
        final Map<InstanceIdentifier<?>, DataObject> updatedOper = new HashMap<>();
        final Set<InstanceIdentifier<?>> removedConfig = new HashSet<>();
        final Set<InstanceIdentifier<?>> removedOper = new HashSet<>();

        for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.data.changed.notification.DataChangeEvent d :dataChangedNotification.getDataChangeEvent()) {
            switch (d.getOperation()) {
            case Created:
                switch (d.getStore()) {
                case Config:
                    createdConfig.put(d.getPath(), d);
                    break;
                case Operation:
                    createdOper.put(d.getPath(), d);
                    break;
                }
                break;
            case Deleted:
                switch (d.getStore()) {
                case Config:
                    removedConfig.add(d.getPath());
                    break;
                case Operation:
                    removedOper.add(d.getPath());
                    break;
                }
                break;
            case Updated:
                switch (d.getStore()) {
                case Config:
                    origConfig.put(d.getPath(), d);
                    updatedConfig.put(d.getPath(), d);
                    break;
                case Operation:
                    origOper.put(d.getPath(),d);
                    updatedOper.put(d.getPath(),d);
                    break;
                }
                break;
            }
        }

        this.createdConfig = Collections.unmodifiableMap(createdConfig);
        this.createdOper = Collections.unmodifiableMap(createdOper);
        this.origConfig = Collections.unmodifiableMap(origConfig);
        this.origOper = Collections.unmodifiableMap(origOper);
        this.updatedConfig = Collections.unmodifiableMap(updatedConfig);
        this.updatedOper = Collections.unmodifiableMap(updatedOper);
        this.removedConfig = Collections.unmodifiableSet(removedConfig);
        this.removedOper = Collections.unmodifiableSet(removedOper);
    }

    @Override
    public DataObject getOriginalConfigurationSubtree() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataObject getOriginalOperationalSubtree() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataObject getUpdatedConfigurationSubtree() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataObject getUpdatedOperationalSubtree() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getCreatedOperationalData() {
        return createdOper;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getCreatedConfigurationData() {
        return createdConfig;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getUpdatedOperationalData() {
        return updatedOper;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getUpdatedConfigurationData() {
        return updatedConfig;
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedConfigurationData() {
        return removedConfig;
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedOperationalData() {
        return removedOper;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getOriginalConfigurationData() {
        return origConfig;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getOriginalOperationalData() {
        return origOper;
    }
}
