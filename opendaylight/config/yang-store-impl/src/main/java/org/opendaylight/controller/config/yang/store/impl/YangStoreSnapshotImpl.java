/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.yangtools.yang.model.api.Module;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

public class YangStoreSnapshotImpl implements YangStoreSnapshot {

    private final Map<String /* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> moduleMXBeanEntryMap;

    private final Map<String, Entry<Module, String>> moduleMap;

    public YangStoreSnapshotImpl(
            Map<String, Map<String, ModuleMXBeanEntry>> moduleMXBeanEntryMap,
            Map<String, Entry<Module, String>> moduleMap) {
        this.moduleMXBeanEntryMap = Collections.unmodifiableMap(moduleMXBeanEntryMap);
        this.moduleMap = Collections.unmodifiableMap(moduleMap);
    }

    public YangStoreSnapshotImpl(YangStoreSnapshot yangStoreSnapshot) {
        this.moduleMXBeanEntryMap = yangStoreSnapshot.getModuleMXBeanEntryMap();
        this.moduleMap = yangStoreSnapshot.getModuleMap();
    }

    /**
     * @return all loaded config modules. Key of outer map is namespace of yang file.
     * Key of inner map is name of module entry. Value is module entry.
     */
    @Override
    public Map<String, Map<String, ModuleMXBeanEntry>> getModuleMXBeanEntryMap() {
        return moduleMXBeanEntryMap;
    }

    @Override
    public Map<String, Entry<Module, String>> getModuleMap() {
        return moduleMap;
    }

    @Override
    public int countModuleMXBeanEntries() {
        int i = 0;
        for (Map<String, ModuleMXBeanEntry> value : moduleMXBeanEntryMap
                .values()) {
            i += value.keySet().size();
        }
        return i;
    }

    @Override
    public void close() {
        // TODO: reference counting
    }

}
