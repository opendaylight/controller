/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslator;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MbeParser {
    private static final Logger logger = LoggerFactory.getLogger(MbeParser.class);

    public YangStoreSnapshotImpl parseYangFiles(Collection<? extends InputStream> allInput) throws YangStoreException {
        YangParserImpl parser = YangParserWrapper.getYangParserInstance();

        Map<InputStream, Module> allYangModules = YangParserWrapper.parseYangFiles(parser, allInput);

        SchemaContext resolveSchemaContext = YangParserWrapper.getSchemaContextFromModules(parser, allYangModules);
        logger.trace("Resolved modules:{}", resolveSchemaContext.getModules());
        // JMX generator

        Map<String, String> namespaceToPackageMapping = Maps.newHashMap();
        PackageTranslator packageTranslator = new PackageTranslator(
                namespaceToPackageMapping);

        Map<QName, ServiceInterfaceEntry> qNamesToSIEs = new HashMap<>();

        Map<IdentitySchemaNode, ServiceInterfaceEntry> knownSEITracker = new HashMap<>();
        // create SIE structure qNamesToSIEs
        for (Module module : resolveSchemaContext.getModules()) {
            String packageName = packageTranslator.getPackageName(module);
            Map<QName, ServiceInterfaceEntry> namesToSIEntries = ServiceInterfaceEntry
                    .create(module, packageName,knownSEITracker);

            for (Entry<QName, ServiceInterfaceEntry> sieEntry : namesToSIEntries
                    .entrySet()) {

                // merge value into qNamesToSIEs
                if (qNamesToSIEs.containsKey(sieEntry.getKey()) == false) {
                    qNamesToSIEs.put(sieEntry.getKey(), sieEntry.getValue());
                } else {
                    throw new IllegalStateException(
                            "Cannot add two SIE with same qname "
                                    + sieEntry.getValue());
                }
            }
        }

        Map<String, Map<String, ModuleMXBeanEntry>> moduleMXBeanEntryMap = Maps.newHashMap();
        Map<Module, String> modulesToSources = new HashMap<>();
        Map<QName, Map<String /* identity local name */, ModuleMXBeanEntry>>
                qNamesToIdentitiesToModuleMXBeanEntries = new HashMap<>();


        for (Entry<InputStream, Module> moduleEntry : allYangModules.entrySet()) {
            Module module = moduleEntry.getValue();
            String packageName = packageTranslator.getPackageName(module);
            TypeProviderWrapper typeProviderWrapper = new TypeProviderWrapper(
                    new TypeProviderImpl(resolveSchemaContext));
            String yangAsString = reReadInputStream(moduleEntry);

            QName qName = new QName(module.getNamespace(), module.getRevision(), module.getName());

            Map<String /* MB identity local name */, ModuleMXBeanEntry> namesToMBEs =
                    Collections.unmodifiableMap(ModuleMXBeanEntry.create(module, qNamesToSIEs, resolveSchemaContext,
                            typeProviderWrapper, packageName));
            moduleMXBeanEntryMap.put(module.getNamespace().toString(), namesToMBEs);
            modulesToSources.put(module, yangAsString);
            qNamesToIdentitiesToModuleMXBeanEntries.put(qName, namesToMBEs);
        }

        return new YangStoreSnapshotImpl(moduleMXBeanEntryMap, modulesToSources, qNamesToIdentitiesToModuleMXBeanEntries);
    }

    private String reReadInputStream(Entry<InputStream, Module> moduleEntry) {
        String yangAsString;
        try {
            moduleEntry.getKey().reset();
            yangAsString = IOUtils.toString(moduleEntry.getKey());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot reread " + moduleEntry.getValue(), e);
        }
        return yangAsString;
    }

    @Deprecated
    public Map<Module, String> parseYangFilesToString(Collection<? extends InputStream> allYangs) {

        logger.error("Using deprecated method that will be removed soon", new UnsupportedOperationException("Deprecated"));
        YangParserImpl parser = YangParserWrapper.getYangParserInstance();

        Map<InputStream, Module> allYangModules = parser
                .parseYangModelsFromStreamsMapped(Lists.newArrayList(allYangs));
        Map<Module, String> retVal = new HashMap<>();

        for (Entry<InputStream, Module> entry : allYangModules.entrySet()) {
            try {
                retVal.put(entry.getValue(), IOUtils.toString(entry.getKey()));
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Can not create string from yang file.");
            }
        }
        return retVal;
    }

}
