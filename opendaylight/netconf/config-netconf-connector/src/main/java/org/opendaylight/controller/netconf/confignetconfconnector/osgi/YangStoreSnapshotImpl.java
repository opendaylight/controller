/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import com.google.common.collect.Maps;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslator;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class YangStoreSnapshotImpl implements YangStoreSnapshot {
    private static final Logger logger = LoggerFactory.getLogger(YangStoreSnapshotImpl.class);


    private final Map<String /* Namespace from yang file */,
            Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> moduleMXBeanEntryMap;


    private final Map<QName, Map<String, ModuleMXBeanEntry>> qNamesToIdentitiesToModuleMXBeanEntries;

    private final SchemaContext schemaContext;


    public YangStoreSnapshotImpl(SchemaContext resolveSchemaContext) {
        logger.trace("Resolved modules:{}", resolveSchemaContext.getModules());
        this.schemaContext = resolveSchemaContext;
        // JMX generator

        Map<String, String> namespaceToPackageMapping = Maps.newHashMap();
        PackageTranslator packageTranslator = new PackageTranslator(namespaceToPackageMapping);
        Map<QName, ServiceInterfaceEntry> qNamesToSIEs = new HashMap<>();
        Map<IdentitySchemaNode, ServiceInterfaceEntry> knownSEITracker = new HashMap<>();
        // create SIE structure qNamesToSIEs
        for (Module module : resolveSchemaContext.getModules()) {
            String packageName = packageTranslator.getPackageName(module);
            Map<QName, ServiceInterfaceEntry> namesToSIEntries = ServiceInterfaceEntry
                    .create(module, packageName, knownSEITracker);
            for (Entry<QName, ServiceInterfaceEntry> sieEntry : namesToSIEntries.entrySet()) {
                // merge value into qNamesToSIEs
                if (qNamesToSIEs.containsKey(sieEntry.getKey()) == false) {
                    qNamesToSIEs.put(sieEntry.getKey(), sieEntry.getValue());
                } else {
                    throw new IllegalStateException("Cannot add two SIE with same qname "
                                    + sieEntry.getValue());
                }
            }
        }

        Map<String, Map<String, ModuleMXBeanEntry>> moduleMXBeanEntryMap = Maps.newHashMap();

        Map<QName, Map<String /* identity local name */, ModuleMXBeanEntry>> qNamesToIdentitiesToModuleMXBeanEntries = new HashMap<>();


        for (Module module : schemaContext.getModules()) {
            String packageName = packageTranslator.getPackageName(module);
            TypeProviderWrapper typeProviderWrapper = new TypeProviderWrapper(
                    new TypeProviderImpl(resolveSchemaContext));

            QName qName = new QName(module.getNamespace(), module.getRevision(), module.getName());

            Map<String /* MB identity local name */, ModuleMXBeanEntry> namesToMBEs =
                    Collections.unmodifiableMap(ModuleMXBeanEntry.create(module, qNamesToSIEs, resolveSchemaContext,
                            typeProviderWrapper, packageName));
            moduleMXBeanEntryMap.put(module.getNamespace().toString(), namesToMBEs);

            qNamesToIdentitiesToModuleMXBeanEntries.put(qName, namesToMBEs);
        }
        this.moduleMXBeanEntryMap = Collections.unmodifiableMap(moduleMXBeanEntryMap);
        this.qNamesToIdentitiesToModuleMXBeanEntries = Collections.unmodifiableMap(qNamesToIdentitiesToModuleMXBeanEntries);

    }

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
        return schemaContext.getModules();
    }

    @Override
    public String getModuleSource(org.opendaylight.yangtools.yang.model.api.ModuleIdentifier moduleIdentifier) {
        return schemaContext.getModuleSource(moduleIdentifier).get();
    }

    @Override
    public void close() {

    }
}
