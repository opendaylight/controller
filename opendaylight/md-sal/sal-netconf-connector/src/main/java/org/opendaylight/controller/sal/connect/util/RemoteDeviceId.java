/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.util;

import java.util.Collections;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class RemoteDeviceId {

    private final String name;
    private final InstanceIdentifier path;

    public RemoteDeviceId(final ModuleIdentifier identifier) {
        Preconditions.checkNotNull(identifier);
        this.name = identifier.getInstanceName();
        this.path = InstanceIdentifier.builder(InventoryUtils.INVENTORY_PATH)
                .nodeWithKey(InventoryUtils.INVENTORY_NODE, Collections.<QName, Object>singletonMap(InventoryUtils.INVENTORY_ID, name)).toInstance();
    }

    public String getName() {
        return name;
    }

    public InstanceIdentifier getPath() {
        return path;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteDeviceId)) return false;

        final RemoteDeviceId that = (RemoteDeviceId) o;

        if (!name.equals(that.name)) return false;
        if (!path.equals(that.path)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
