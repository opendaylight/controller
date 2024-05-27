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
import java.io.File;
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
public class FilePekkoConfigurationReader implements PekkoConfigurationReader {
    private static final Logger LOG = LoggerFactory.getLogger(FilePekkoConfigurationReader.class);
    private static final String CUSTOM_AKKA_CONF_PATH = "./configuration/initial/akka.conf";
    private static final String FACTORY_AKKA_CONF_PATH = "./configuration/factory/akka.conf";

    @Override
    public Config read() {
        File customConfigFile = new File(CUSTOM_AKKA_CONF_PATH);
        checkState(customConfigFile.exists(), "%s is missing", customConfigFile);

        File factoryConfigFile = new File(FACTORY_AKKA_CONF_PATH);
        if (factoryConfigFile.exists()) {
            return ConfigFactory.parseFile(customConfigFile).withFallback(ConfigFactory.parseFile(factoryConfigFile));
        }

        return ConfigFactory.parseFile(customConfigFile);
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
