/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.DataChangedNotification;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RemoteDataChangeEvent implements DataChangeEvent<InstanceIdentifier<? extends DataObject>,DataObject> {


    private final DataChangedNotification dataChangedNotification;


    public RemoteDataChangeEvent(DataChangedNotification dataChangedNotification){

        this.dataChangedNotification = dataChangedNotification;
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
        return new HashMap<InstanceIdentifier<?>, DataObject>(){{
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.data.changed.notification.DataChangeEvent d :dataChangedNotification.getDataChangeEvent()){
                if (d.getOperation().getIntValue() == 0 && d.getStore().getIntValue() == 1){
                    put(d.getPath(),d);
                }
            }
        }};
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getCreatedConfigurationData() {
        return new HashMap<InstanceIdentifier<?>, DataObject>(){{
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.data.changed.notification.DataChangeEvent d :dataChangedNotification.getDataChangeEvent()){
                if (d.getOperation().getIntValue() == 0 && d.getStore().getIntValue() == 0){
                    put(d.getPath(),d);
                }
            }
        }};
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getUpdatedOperationalData() {
        return new HashMap<InstanceIdentifier<?>, DataObject>(){{
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.data.changed.notification.DataChangeEvent d :dataChangedNotification.getDataChangeEvent()){
                if (d.getOperation().getIntValue() == 1 && d.getStore().getIntValue() == 1){
                    put(d.getPath(),d);
                }
            }
        }};
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getUpdatedConfigurationData() {
        return new HashMap<InstanceIdentifier<?>, DataObject>(){{
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.data.changed.notification.DataChangeEvent d :dataChangedNotification.getDataChangeEvent()){
                if (d.getOperation().getIntValue() == 1 && d.getStore().getIntValue() == 0){
                    put(d.getPath(),d);
                }
            }
        }};
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedConfigurationData() {
        return new HashSet<InstanceIdentifier<?>>(){{
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.data.changed.notification.DataChangeEvent d :dataChangedNotification.getDataChangeEvent()){
                if (d.getOperation().getIntValue() == 2 && d.getStore().getIntValue() == 0){
                    add(d.getPath());
                }
            }
        }};
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedOperationalData() {
        return new HashSet<InstanceIdentifier<?>>(){{
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.data.changed.notification.DataChangeEvent d :dataChangedNotification.getDataChangeEvent()){
                if (d.getOperation().getIntValue() == 2 && d.getStore().getIntValue() == 1){
                    add(d.getPath());
                }
            }
        }};
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getOriginalConfigurationData() {
        return new HashMap<InstanceIdentifier<?>, DataObject>(){{
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.data.changed.notification.DataChangeEvent d :dataChangedNotification.getDataChangeEvent()){
                if (d.getOperation().getIntValue() == 1 && d.getStore().getIntValue() == 0){
                    put(d.getPath(),d);
                }
            }
        }};
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getOriginalOperationalData() {
        return new HashMap<InstanceIdentifier<?>, DataObject>(){{
            for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.data.changed.notification.DataChangeEvent d :dataChangedNotification.getDataChangeEvent()){
                if (d.getOperation().getIntValue() == 1 && d.getStore().getIntValue() == 1){
                    put(d.getPath(),d);
                }
            }
        }};
    }
}
