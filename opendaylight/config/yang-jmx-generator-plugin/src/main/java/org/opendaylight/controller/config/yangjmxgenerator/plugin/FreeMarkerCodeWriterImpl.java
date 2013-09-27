/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlFilePersister;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.RuntimeRegistratorFtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

final class FreeMarkerCodeWriterImpl implements CodeWriter {

    private static final Logger logger = LoggerFactory
            .getLogger(FreeMarkerCodeWriterImpl.class);

    private final FtlFilePersister ftlFilePersister = new FtlFilePersister();

    public FreeMarkerCodeWriterImpl() {
    }

    @Override
    public File writeSie(ServiceInterfaceEntry sie, File outputBaseDir) {
        try {
            Collection<FtlTemplate> values = TemplateFactory.getFtlTemplates(
                    sie).values();
            return ftlFilePersister.persist(values, outputBaseDir, true).get(0);
        } catch (Exception e) {
            String message = "An error occurred during Service interface generating, sie:"
                    + sie.getTypeName() + ", " + sie.getFullyQualifiedName();
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public List<File> writeMbe(ModuleMXBeanEntry mbe, File targetBaseDir,
            File mainBaseDir, File resourceBaseDir) {
        try {
            List<File> generatedFiles = Lists.newArrayList();

            generatedFiles.addAll(ftlFilePersister.persist(TemplateFactory
                    .getFtlTemplates(mbe).values(), targetBaseDir, true));
            generatedFiles.addAll(ftlFilePersister.persist(TemplateFactory
                    .getFtlStubTemplates(mbe).values(), mainBaseDir, false));

            // write runtime bean MXBeans and registrators
            Collection<RuntimeBeanEntry> runtimeBeans = mbe.getRuntimeBeans();
            if (runtimeBeans.size() > 0) {
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
                boolean overwrite = true;

                FtlFilePersister ftlFilePersister = new FtlFilePersister();
                List<File> persisted = ftlFilePersister.persist(allFtlFiles,
                        targetBaseDir, overwrite);
                // FIXME: check for intersection
                generatedFiles.addAll(persisted);
            }

            // purge nulls
            for (Iterator<File> it = generatedFiles.iterator(); it.hasNext();) {
                if (it.next() == null) {
                    it.remove();
                }
            }

            return generatedFiles;

        } catch (Exception e) {
            String message = "An error occurred during Module generating, mbe:"
                    + mbe.getJavaNamePrefix();
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

}
