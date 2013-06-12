/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.spi;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;

/**
 * Classes implementing this interface can be submitted to maven-yang-plugin's
 * generate-sources goal.
 */
public interface CodeGenerator {

    /**
     * Generate sources from provided {@link SchemaContext}
     *
     * @param context
     *            parsed from yang files
     * @param outputBaseDir
     *            expected output directory for generated sources configured by
     *            user
     * @param currentModules
     *            yang modules parsed from yangFilesRootDir
     * @param log
     *            maven logger
     * @return collection of files that were generated from schema context
     * @throws IOException
     */
    Collection<File> generateSources(SchemaContext context, File outputBaseDir,
            Set<Module> currentModules) throws IOException;

    /**
     * Utilize maven logging if necessary
     *
     * @param log
     */
    void setLog(Log log);

    /**
     * Provided map contains all configuration that was set in pom for code
     * generator in additionalConfiguration tag
     *
     * @param additionalConfiguration
     */
    void setAdditionalConfig(Map<String, String> additionalConfiguration);

    /**
     * Provided folder is marked as resources and its content will be packaged
     * in resulting jar. Feel free to add necessary resources
     *
     * @param resourceBaseDir
     */
    void setResourceBaseDir(File resourceBaseDir);

    /**
     * Provided maven project object. Any additional information about current
     * maven project can be accessed from it.
     *
     * @param resourceBaseDir
     */
    void setMavenProject(MavenProject project);
}
