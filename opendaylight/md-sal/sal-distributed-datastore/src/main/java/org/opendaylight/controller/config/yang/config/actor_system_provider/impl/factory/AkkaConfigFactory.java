/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.actor_system_provider.impl.factory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;

public class AkkaConfigFactory {

    private static final String CONFIGURATION_NAME = "odl-cluster-data";

    public static Config createAkkaConfig(final AkkaConfigurationReader reader) {
        return ConfigFactory.load(reader.read()).getConfig(CONFIGURATION_NAME);
    }
}
