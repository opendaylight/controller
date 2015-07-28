/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DefaultAkkaConfigurationReader implements AkkaConfigurationReader {
    public static final String AKKA_CONF_PATH = "./configuration/initial/akka.conf";
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAkkaConfigurationReader.class);
    private String configPath;

    public DefaultAkkaConfigurationReader () {
        this.configPath = "./configuration/initial/";
    }
    public DefaultAkkaConfigurationReader (String path) {
        Preconditions.checkNotNull(path, "akka configuration path should not be null");
        this.configPath = path;
    }

    @Override public Config read() {
        File defaultConfigFile = null;
        defaultConfigFile = new File(configPath+"akka.conf");
        if (!defaultConfigFile.exists()) {
            LOG.info ("akka.conf does not exist in the given path {}, searching in the default path", configPath);
            defaultConfigFile = new File(AKKA_CONF_PATH);
        }
        Preconditions.checkState(defaultConfigFile.exists(), "akka.conf is missing");
        return ConfigFactory.parseFile(defaultConfigFile);

    }
}
