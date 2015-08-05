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

public class FileAkkaConfigurationReader implements AkkaConfigurationReader {
    private static final String DEFAULT_AKKA_CONF_PATH = "./configuration/initial";
    private static final String DEFAULT_AKKA_FILE_NAME = "akka.conf";
    private static final Logger LOG = LoggerFactory.getLogger(FileAkkaConfigurationReader.class);
    private String configPath;

    public FileAkkaConfigurationReader() {
        this.configPath = DEFAULT_AKKA_CONF_PATH;
    }
    public FileAkkaConfigurationReader(String path) {
        Preconditions.checkNotNull(path, "akka configuration path should not be null");
        this.configPath = path;
    }

    @Override public Config read() {
        File configFile = new File(configPath, DEFAULT_AKKA_FILE_NAME);
        if (!configFile.exists()) {
            LOG.info ("akka.conf does not exist in the given path {}, searching in the default path", configPath);
            configFile = new File(DEFAULT_AKKA_CONF_PATH, DEFAULT_AKKA_FILE_NAME);
        }
        Preconditions.checkState(configFile.exists(), "%s is missing", configFile);
        return ConfigFactory.parseFile(configFile);

    }
}
