/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.opendaylight.controller.yang2sources.plugin.ConfigArg.ResourceProviderArg;
import org.opendaylight.controller.yang2sources.spi.ResourceGenerator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

@Mojo(name = "generate-resources", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class YangToResourcesMojo extends AbstractMojo {

    private static final String LOG_PREFIX = "yang-to-resources:";

    @Parameter(required = true)
    private ResourceProviderArg[] resourceProviders;

    @Parameter(required = true)
    private String yangFilesRootDir;

    @VisibleForTesting
    YangToResourcesMojo(ResourceProviderArg[] resourceProviderArgs,
            String yangFilesRootDir) {
        super();
        this.resourceProviders = resourceProviderArgs;
        this.yangFilesRootDir = yangFilesRootDir;
    }

    public YangToResourcesMojo() {
        super();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (resourceProviders.length == 0) {
            getLog().warn(
                    Util.message("No resource provider classes provided",
                            LOG_PREFIX));
            return;
        }

        Map<String, String> thrown = Maps.newHashMap();
        Collection<File> yangFiles = Util.listFiles(yangFilesRootDir);

        for (ResourceProviderArg resourceProvider : resourceProviders) {
            try {

                provideResourcesWithOneProvider(yangFiles, resourceProvider);

            } catch (Exception e) {
                // try other generators, exception will be thrown after
                getLog().error(
                        Util.message(
                                "Unable to provide resources with %s resource provider",
                                LOG_PREFIX,
                                resourceProvider.getResourceProviderClass()), e);
                thrown.put(resourceProvider.getResourceProviderClass(), e
                        .getClass().getCanonicalName());
            }
        }

        if (!thrown.isEmpty()) {
            String message = Util
                    .message(
                            "One or more code resource provider failed, including failed list(resourceProviderClass=exception) %s",
                            LOG_PREFIX, thrown.toString());
            getLog().error(message);
            throw new MojoFailureException(message);
        }
    }

    /**
     * Instantiate provider from class and call required method
     */
    private void provideResourcesWithOneProvider(Collection<File> yangFiles,
            ResourceProviderArg resourceProvider)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {

        resourceProvider.check();

        ResourceGenerator g = Util.getInstance(
                resourceProvider.getResourceProviderClass(),
                ResourceGenerator.class);
        getLog().info(
                Util.message("Resource provider instantiated from %s",
                        LOG_PREFIX, resourceProvider.getResourceProviderClass()));

        g.generateResourceFiles(yangFiles, resourceProvider.getOutputBaseDir());
        getLog().info(
                Util.message("Resource provider %s call successful",
                        LOG_PREFIX, resourceProvider.getResourceProviderClass()));
    }
}
