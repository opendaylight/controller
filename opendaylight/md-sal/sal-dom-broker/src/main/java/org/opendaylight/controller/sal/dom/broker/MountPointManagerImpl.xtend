/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker


import org.opendaylight.controller.sal.core.api.mount.MountProvisionService
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import static com.google.common.base.Preconditions.*;
import org.opendaylight.controller.sal.core.api.data.DataProviderService
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService.MountProvisionListener
import org.opendaylight.yangtools.concepts.util.ListenerRegistry

class MountPointManagerImpl implements MountProvisionService {
    
    @Property
    DataProviderService dataBroker;
    
    val ListenerRegistry<MountProvisionListener> listeners = ListenerRegistry.create()
    
    ConcurrentMap<InstanceIdentifier,MountPointImpl> mounts = new ConcurrentHashMap();
    
    override createMountPoint(InstanceIdentifier path) {
        checkState(!mounts.containsKey(path),"Mount already created");
        val mount = new MountPointImpl(path);
        registerMountPoint(mount);
        mounts.put(path,mount);
        notifyMountCreated(path);
        return mount;
    }
    
    def notifyMountCreated(InstanceIdentifier identifier) {
        for(listener : listeners) {
            listener.instance.onMountPointCreated(identifier);
        }
    }
    
    def registerMountPoint(MountPointImpl impl) {
        //dataBroker?.registerConfigurationReader(impl.mountPath,impl.readWrapper);
        //dataBroker?.registerOperationalReader(impl.mountPath,impl.readWrapper);
    }
    
    override registerProvisionListener(MountProvisionListener listener) {
        listeners.register(listener)
    }
    
    
    override createOrGetMountPoint(InstanceIdentifier path) {
        val mount = mounts.get(path);
        if(mount === null) {
            return createMountPoint(path)
        }
        return mount;
    }
    
    
    override getMountPoint(InstanceIdentifier path) {
        mounts.get(path);
    }
}
