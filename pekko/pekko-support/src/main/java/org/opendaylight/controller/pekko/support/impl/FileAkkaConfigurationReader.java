/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import static com.google.common.base.Preconditions.checkState;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import org.kohsuke.MetaInfServices;
import org.opendaylight.controller.pekko.support.spi.ConfigurationReader;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MetaInfServices
@Component(immediate = true)
public class FileAkkaConfigurationReader implements ConfigurationReader {
    private static final Logger LOG = LoggerFactory.getLogger(FileAkkaConfigurationReader.class);
    private static final Path CUSTOM_AKKA_CONF_PATH = Path.of("configuration", "initial", "pekko.conf");
    private static final Path FACTORY_AKKA_CONF_PATH = Path.of("configuration", "factory", "pekko.conf");

    public FileAkkaConfigurationReader() {
        // nothing else
    }

    @Activate
    public FileAkkaConfigurationReader(final BundleContext bundleContext) {
        LOG.info("File-based Pekko configuration reader enabled");
    }

    @Deactivate
    void deactivate() {
        LOG.info("File-based Pekko configuration reader disabled");
    }

    @Override
    public Config read() {
        // FIXME: improve exceptions
        checkState(Files.exists(CUSTOM_AKKA_CONF_PATH), "%s is missing", CUSTOM_AKKA_CONF_PATH);
        final var parsed = ConfigFactory.parseFile(CUSTOM_AKKA_CONF_PATH.toFile());
        return Files.exists(FACTORY_AKKA_CONF_PATH)
            ? parsed.withFallback(ConfigFactory.parseFile(FACTORY_AKKA_CONF_PATH.toFile())) : parsed;
    }
}
