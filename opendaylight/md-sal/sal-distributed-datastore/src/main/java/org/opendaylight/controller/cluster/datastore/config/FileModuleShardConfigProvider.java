/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ModuleShardConfigProvider that reads the module and shard configuration from files.
 *
 * @author Thomas Pantelis
 */
public class FileModuleShardConfigProvider extends AbstractModuleShardConfigProvider {
    private static final Logger LOG = LoggerFactory.getLogger(FileModuleShardConfigProvider.class);

    private final String moduleShardsConfigPath;
    private final String modulesConfigPath;

    public FileModuleShardConfigProvider(String moduleShardsConfigPath, String modulesConfigPath) {
        this.moduleShardsConfigPath = moduleShardsConfigPath;
        this.modulesConfigPath = modulesConfigPath;
    }

    @Override
    protected Config loadModuleShardsConfig() {
        File moduleShardsFile = new File("./configuration/initial/" + moduleShardsConfigPath);
        if (moduleShardsFile.exists()) {
            LOG.info("module shards config file exists - reading config from it");
            return ConfigFactory.parseFile(moduleShardsFile);
        }

        LOG.warn("module shards configuration read from resource");
        return ConfigFactory.load(moduleShardsConfigPath);
    }

    @Override
    protected Config loadModulesConfig() {
        File modulesFile = new File("./configuration/initial/" + modulesConfigPath);
        if (modulesFile.exists()) {
            LOG.info("modules config file exists - reading config from it");
            return ConfigFactory.parseFile(modulesFile);
        }

        LOG.warn("modules configuration read from resource");
        return ConfigFactory.load(modulesConfigPath);
    }
}
