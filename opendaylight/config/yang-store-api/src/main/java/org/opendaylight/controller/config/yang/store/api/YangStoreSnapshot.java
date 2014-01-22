/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.api;

import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;

import java.util.Map;
import java.util.Set;

public interface YangStoreSnapshot extends AutoCloseable {

    /**
     * @deprecated Use {@link #getQNamesToIdentitiesToModuleMXBeanEntries()} instead. This method return only one
     * module representation even if multiple revisions are available.
     */
    @Deprecated
    Map<String/* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> getModuleMXBeanEntryMap();


    Map<QName, Map<String /* identity local name */, ModuleMXBeanEntry>> getQNamesToIdentitiesToModuleMXBeanEntries();

    /**
     * Get number of parsed ModuleMXBeanEntry instances.
     */
    int countModuleMXBeanEntries();

    /**
     * Get all modules discovered when this snapshot was created.
     * @return all modules discovered. If one module exists with two different revisions, return both.
     */
    Set<Module> getModules();

    /**
     * Get all modules together with their yang sources.
     */
    Map<Module, String> getModulesToSources();

    /**
     * Retrieve source of module as it appeared during creation of this snapshot.
     * @param module
     * @return yang source of given module
     * @throws java.lang.IllegalArgumentException if module does not belong to this snapshot
     */
    String getModuleSource(Module module);

    @Override
    void close();
}
