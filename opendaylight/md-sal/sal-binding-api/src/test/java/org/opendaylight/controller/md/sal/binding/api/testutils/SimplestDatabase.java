/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api.testutils;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import com.google.common.base.Optional;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class SimplestDatabase {

    private Map<LogicalDatastoreType, Map<InstanceIdentifier<?>, DataObject>> map;

    protected Map<LogicalDatastoreType, Map<InstanceIdentifier<?>, DataObject>> getMap() {
        if (map == null) {
            map = new EnumMap<>(LogicalDatastoreType.class);
            map.put(OPERATIONAL, new HashMap<>());
            map.put(CONFIGURATION, new HashMap<>());
        }
        return map;
    }

    void mergeInto(SimplestDatabase parent) {
        map.get(OPERATIONAL).forEach((type, innerMap) -> parent.map.get(OPERATIONAL).put(type, innerMap));
        map.get(CONFIGURATION).forEach((type, innerMap) -> parent.map.get(CONFIGURATION).put(type, innerMap));
        map = null;
    }

    @SuppressWarnings("unchecked")
    public <T extends DataObject> Optional<T> get(LogicalDatastoreType store, InstanceIdentifier<T> path) {
        return (Optional<T>) Optional.fromNullable(getMap().get(store).get(path));
    }
}
