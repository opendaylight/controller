/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.api;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.yangtools.yang.model.api.Module;

public interface YangStoreSnapshot extends AutoCloseable {

    Map<String/* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> getModuleMXBeanEntryMap();

    Map<String, Entry<Module, String>> getModuleMap();

    int countModuleMXBeanEntries();

    @Override
    void close();
}
