/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import static com.google.common.base.Preconditions.checkState;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@MetaInfServices
@Singleton
public class FileAkkaConfigurationReader implements AkkaConfigurationReader {
    private static final Logger LOG = LoggerFactory.getLogger(FileAkkaConfigurationReader.class);
    private static final Path CUSTOM_AKKA_CONF_PATH = Path.of("configuration", "initial", "pekko.conf");
    private static final Path FACTORY_AKKA_CONF_PATH = Path.of("configuration", "factory", "pekko.conf");

    @Override
    public Config read() {
        checkState(Files.exists(CUSTOM_AKKA_CONF_PATH), "%s is missing", CUSTOM_AKKA_CONF_PATH);
        final var parsed = ConfigFactory.parseFile(CUSTOM_AKKA_CONF_PATH.toFile());
        return Files.exists(FACTORY_AKKA_CONF_PATH)
            ? parsed.withFallback(ConfigFactory.parseFile(FACTORY_AKKA_CONF_PATH.toFile())) : parsed;
    }

    @Activate
    void activate() {
        LOG.info("File-based Pekko configuration reader enabled");
    }

    @Deactivate
    void deactivate() {
        LOG.info("File-based Pekko configuration reader disabled");
    }
}
