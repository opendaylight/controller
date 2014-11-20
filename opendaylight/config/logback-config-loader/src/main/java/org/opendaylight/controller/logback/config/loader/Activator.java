/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.logback.config.loader;

import java.io.File;
import java.util.List;
import org.opendaylight.controller.logback.config.loader.impl.LogbackConfigUtil;
import org.opendaylight.controller.logback.config.loader.impl.LogbackConfigurationLoader;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * default activator for loading multiple logback configuration files
 */
public class Activator implements BundleActivator {

    /**
     * expected environment variable name, containing the root folder containing
     * logback configurations
     */
    private static final String LOGBACK_CONFIG_D = "logback.config.d";
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    @Override
    public void start(BundleContext context) {
        LOG.info("Starting logback configuration loader");
        String logbackConfigRoot = System.getProperty(LOGBACK_CONFIG_D);
        LOG.debug("configRoot: {}", logbackConfigRoot);
        if (logbackConfigRoot != null) {
            File logbackConfigRootFile = new File(logbackConfigRoot);
            List<File> sortedConfigFiles = LogbackConfigUtil.harvestSortedConfigFiles(logbackConfigRootFile);
            LogbackConfigurationLoader.load(true, sortedConfigFiles.toArray());
        }
    }

    @Override
    public void stop(BundleContext context) {
        LOG.info("Stopping logback configuration loader");
        // TODO: need reset/reload default config?
    }

}
