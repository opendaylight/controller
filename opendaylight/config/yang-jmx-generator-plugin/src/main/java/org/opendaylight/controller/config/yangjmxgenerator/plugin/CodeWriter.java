/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.GeneralClassTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.GeneralInterfaceTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.RuntimeRegistratorFtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.StubFactoryTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory.AbsFactoryGeneratedObjectFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory.AbsModuleGeneratedObjectFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory.ConcreteModuleGeneratedObjectFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory.GenericGeneratedObjectFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObject;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CodeWriter {

    private static final Logger LOG = LoggerFactory.getLogger(CodeWriter.class);
    private static final Optional<String> COPYRIGHT = StringUtil.loadCopyright();

    public File writeSie(final ServiceInterfaceEntry sie, final File outputBaseDir) {
        try {
            GeneralInterfaceTemplate generalInterfaceTemplate = TemplateFactory.serviceInterfaceFromSie(sie);
            GeneratedObject go = new GenericGeneratedObjectFactory().toGeneratedObject(generalInterfaceTemplate, COPYRIGHT);
            return go.persist(outputBaseDir).get().getValue();
        } catch (Exception e) {
            String message = "An error occurred during Service interface generating, sie:"
                    + sie.getTypeName() + ", " + sie.getFullyQualifiedName();
            LOG.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public List<File> writeMbe(final ModuleMXBeanEntry mbe, final File targetBaseDir,
                               final File mainBaseDir) {
        try {
            List<File> generatedFiles = Lists.newArrayList();


            Map<GeneratedObject, Boolean /*overwrite*/> gos = new HashMap<>();

            // generate mx interface and abstract factory

            // TOs
            Map<String,GeneralClassTemplate> tosFromMbe = TemplateFactory.tOsFromMbe(mbe);
            for(GeneralClassTemplate template: tosFromMbe.values()) {
                gos.put(new GenericGeneratedObjectFactory().toGeneratedObject(template, COPYRIGHT), true);
            }

            // MXBean interface
            GeneralInterfaceTemplate ifcTemplate = TemplateFactory.mXBeanInterfaceTemplateFromMbe(mbe);
            gos.put(new GenericGeneratedObjectFactory().toGeneratedObject(ifcTemplate, COPYRIGHT), true);


            // generate abstract factory
            gos.put(new AbsFactoryGeneratedObjectFactory().toGeneratedObject(mbe, COPYRIGHT), true);

            // generate abstract module
            gos.put(new AbsModuleGeneratedObjectFactory().toGeneratedObject(mbe, COPYRIGHT), true);

            // generate concrete factory
            StubFactoryTemplate concreteFactory = TemplateFactory.stubFactoryTemplateFromMbe(mbe);
            gos.put(new GenericGeneratedObjectFactory().toGeneratedObject(concreteFactory, COPYRIGHT), false);


            // generate concrete module

            gos.put(new ConcreteModuleGeneratedObjectFactory().toGeneratedObject(mbe, COPYRIGHT, Optional.<String>absent()), false);

            // write runtime bean MXBeans and registrators
            List<FtlTemplate> allFtlFiles = getRuntimeBeanFtlTemplates(mbe.getRuntimeBeans());
            for(FtlTemplate template: allFtlFiles) {
                gos.put(new GenericGeneratedObjectFactory().toGeneratedObject(template, COPYRIGHT), true);
            }

            generatedFiles.addAll(persistGeneratedObjects(targetBaseDir, mainBaseDir, gos));

            // purge nulls
            for (Iterator<File> it = generatedFiles.iterator(); it.hasNext(); ) {
                if (it.next() == null) {
                    it.remove();
                }
            }

            return generatedFiles;

        } catch (Exception e) {
            String message = "An error occurred during Module generating, mbe:"
                    + mbe.getJavaNamePrefix();
            LOG.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private static List<File> persistGeneratedObjects(final File targetBaseDir, final File mainBaseDir,
            final Map<GeneratedObject, Boolean> gos) throws IOException {
        List<File> generatedFiles = new ArrayList<>();
        for (Entry<GeneratedObject, Boolean> entry : gos.entrySet()) {
            boolean overwrite = entry.getValue();
            File dst;
            if (overwrite) {
                dst = targetBaseDir;
            } else {
                dst = mainBaseDir;
            }
            Optional<Entry<FullyQualifiedName, File>> maybePersistEntry = entry.getKey().persist(dst, overwrite);

            if (maybePersistEntry.isPresent()) {
                generatedFiles.add(maybePersistEntry.get().getValue());
            }
        }
        return generatedFiles;
    }

    private static List<FtlTemplate> getRuntimeBeanFtlTemplates(final Collection<RuntimeBeanEntry> runtimeBeans) {
        if (runtimeBeans.isEmpty()) {
            return Collections.emptyList();
        }
        List<FtlTemplate> allFtlFiles = new ArrayList<>();
        { // registrators
            Map<String, FtlTemplate> registratorNamesToFtls = RuntimeRegistratorFtlTemplate
                    .create(RuntimeRegistratorFtlTemplate.findRoot(runtimeBeans));

            allFtlFiles.addAll(registratorNamesToFtls.values());
        }
        { // TOs, MXBean interfaces
            for (RuntimeBeanEntry runtimeBeanEntry : runtimeBeans) {
                Collection<FtlTemplate> ftlFiles = TemplateFactory
                        .getTOAndMXInterfaceFtlFiles(runtimeBeanEntry)
                        .values();
                allFtlFiles.addAll(ftlFiles);
            }
        }
        return allFtlFiles;
    }
}
