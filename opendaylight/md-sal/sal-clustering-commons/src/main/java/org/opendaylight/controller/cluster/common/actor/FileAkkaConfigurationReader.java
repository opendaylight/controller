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
import java.io.File;

public class FileAkkaConfigurationReader implements AkkaConfigurationReader {
    private static final String CUSTOM_AKKA_CONF_PATH = "./configuration/initial/akka.conf";
    private static final String FACTORY_AKKA_CONF_PATH = "./configuration/factory/akka.conf";

    @Override
    public Config read() {
        File customConfigFile = new File(CUSTOM_AKKA_CONF_PATH);
        Preconditions.checkState(customConfigFile.exists(), "%s is missing", customConfigFile);

        File factoryConfigFile = new File(FACTORY_AKKA_CONF_PATH);
        if(factoryConfigFile.exists()) {
            return ConfigFactory.parseFile(customConfigFile).withFallback(ConfigFactory.parseFile(factoryConfigFile));
        }

        return ConfigFactory.parseFile(customConfigFile);
    }
}
