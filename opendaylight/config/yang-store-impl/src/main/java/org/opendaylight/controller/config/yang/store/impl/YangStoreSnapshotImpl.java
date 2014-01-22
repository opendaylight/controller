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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class YangStoreSnapshotImpl implements YangStoreSnapshot {
    private static final Logger logger = LoggerFactory.getLogger(YangStoreSnapshotImpl.class);

    @Deprecated
    private final Map<String /* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> moduleMXBeanEntryMap;

    private final Map<Module, String> modulesToSources;
    private final Map<QName, Map<String, ModuleMXBeanEntry>> qNamesToIdentitiesToModuleMXBeanEntries;

    public YangStoreSnapshotImpl(Map<String, Map<String, ModuleMXBeanEntry>> moduleMXBeanEntryMap,
                                 Map<Module, String> modulesToSources,
                                 Map<QName, Map<String, ModuleMXBeanEntry>> qNamesToIdentitiesToModuleMXBeanEntries) {

        this.moduleMXBeanEntryMap = Collections.unmodifiableMap(moduleMXBeanEntryMap);
        this.modulesToSources = Collections.unmodifiableMap(modulesToSources);
        this.qNamesToIdentitiesToModuleMXBeanEntries = Collections.unmodifiableMap(qNamesToIdentitiesToModuleMXBeanEntries);
    }

    public static YangStoreSnapshotImpl copy(YangStoreSnapshot yangStoreSnapshot) {
        return new YangStoreSnapshotImpl(
                yangStoreSnapshot.getModuleMXBeanEntryMap(),
                yangStoreSnapshot.getModulesToSources(),
                yangStoreSnapshot.getQNamesToIdentitiesToModuleMXBeanEntries());
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
    public Map<QName, Map<String, ModuleMXBeanEntry>> getQNamesToIdentitiesToModuleMXBeanEntries() {
        return qNamesToIdentitiesToModuleMXBeanEntries;
    }

    @Override
    public Set<Module> getModules() {
        return modulesToSources.keySet();
    }

    @Override
    public String getModuleSource(Module module) {
        String result = modulesToSources.get(module);
        if (result == null) {
            logger.trace("Cannot find module {} in {}", module, modulesToSources);
            throw new IllegalArgumentException("Module not found in this snapshot:" + module);
        }
        return result;
    }

    @Override
    public Map<Module, String> getModulesToSources() {
        return modulesToSources;
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
