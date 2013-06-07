/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.spi;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;

public class CodeGeneratorTestImpl implements CodeGenerator {

    private Log log;

    @Override
    public Collection<File> generateSources(SchemaContext context,
            File outputBaseDir, Set<Module> currentModuleBuilders) {
        if (log != null) {
            log.debug(getClass().getCanonicalName()
                    + " generateSources:context: " + context);
            log.debug(getClass().getCanonicalName()
                    + " generateSources:outputBaseDir: " + outputBaseDir);
            log.debug(getClass().getCanonicalName()
                    + " generateSources:currentModuleBuilders: "
                    + currentModuleBuilders);

        }
        return null;
    }

    @Override
    public void setLog(Log log) {
        this.log = log;
    }

    @Override
    public void setAdditionalConfig(Map<String, String> additionalConfiguration) {
        if (log != null)
            log.debug(getClass().getCanonicalName() + " additionalConfig: "
                    + additionalConfiguration);
    }


    @Override
    public void setResourceBaseDir(File resourceBaseDir) {
        if (log != null)
            log.debug(getClass().getCanonicalName() + " resourceBaseDir: "
                    + resourceBaseDir);
    }

    @Override
    public void setMavenProject(MavenProject project) {
        if (log != null)
            log.debug(getClass().getCanonicalName() + " maven project: "
                    + project);
    }

}
