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
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslator;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MbeParser {

    public YangStoreSnapshotImpl parseYangFiles(
            Collection<? extends InputStream> allInput)
            throws YangStoreException {
        YangParserImpl parser = new YangParserImpl();

        List<InputStream> bufferedInputStreams = new ArrayList<>();
        for (InputStream is : allInput) {
            String content;
            try {
                content = IOUtils.toString(is);
            } catch (IOException e) {
                throw new YangStoreException("Can not get yang as String from "
                        + is, e);
            }
            InputStream buf = new ByteArrayInputStream(content.getBytes());
            bufferedInputStreams.add(buf);
        }

        Map<InputStream, Module> allYangModules = parser
                .parseYangModelsFromStreamsMapped(bufferedInputStreams);

        SchemaContext resolveSchemaContext = parser.resolveSchemaContext(Sets
                .newHashSet(allYangModules.values()));

        // JMX generator

        Map<String, String> namespaceToPackageMapping = Maps.newHashMap();
        PackageTranslator packageTranslator = new PackageTranslator(
                namespaceToPackageMapping);

        Map<QName, ServiceInterfaceEntry> qNamesToSIEs = new HashMap<>();

        // create SIE structure qNamesToSIEs
        for (Module module : resolveSchemaContext.getModules()) {
            String packageName = packageTranslator.getPackageName(module);
            Map<QName, ServiceInterfaceEntry> namesToSIEntries = ServiceInterfaceEntry
                    .create(module, packageName);

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

        Map<String, Map<String, ModuleMXBeanEntry>> retVal = Maps.newHashMap();
        Map<String, Entry<Module, String>> modulesMap = new HashMap<>();

        for (Entry<InputStream, Module> moduleEntry : allYangModules.entrySet()) {
            String packageName = packageTranslator.getPackageName(moduleEntry
                    .getValue());
            TypeProviderWrapper typeProviderWrapper = new TypeProviderWrapper(
                    new TypeProviderImpl(resolveSchemaContext));
            String yangAsString;
            try {
                moduleEntry.getKey().reset();
                yangAsString = IOUtils.toString(moduleEntry.getKey());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            modulesMap.put(moduleEntry.getValue().getName(),
                    Maps.immutableEntry(moduleEntry.getValue(), yangAsString));
            Map<String /* MB identity local name */, ModuleMXBeanEntry> namesToMBEs = ModuleMXBeanEntry
                    .create(moduleEntry.getValue(), qNamesToSIEs, resolveSchemaContext, typeProviderWrapper,
                            packageName);
            retVal.put(moduleEntry.getValue().getNamespace().toString(),
                    namesToMBEs);
        }

        return new YangStoreSnapshotImpl(retVal, modulesMap);
    }

    public Map<Module, String> parseYangFilesToString(
            Collection<? extends InputStream> allYangs) {
        YangParserImpl parser = new YangParserImpl();

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
