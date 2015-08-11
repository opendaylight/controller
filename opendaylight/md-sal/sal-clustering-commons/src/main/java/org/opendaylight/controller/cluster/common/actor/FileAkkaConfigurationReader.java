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
    private static final String DEFAULT_AKKA_CONF_PATH = "./configuration/initial/akka.conf";

    @Override public Config read() {
        File configFile = new File(DEFAULT_AKKA_CONF_PATH);
        Preconditions.checkState(configFile.exists(), "%s is missing", configFile);
        return ConfigFactory.parseFile(configFile);

    }
}
