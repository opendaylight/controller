/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractConfig implements UnifiedConfig {

    private final Config config;

    public AbstractConfig(Config config){
        this.config = config;
    }

    @Override
    public Config get() {
        return config;
    }

    public static abstract class Builder<T extends Builder<T>> {
        protected Map<String, Object> configHolder;
        protected Config fallback;

        private final String actorSystemName;

        public Builder(String actorSystemName){
            Preconditions.checkArgument(actorSystemName != null, "Actor system name must not be null");
            this.actorSystemName = actorSystemName;
            configHolder = new HashMap<>();
        }

        public T withConfigReader(AkkaConfigurationReader reader){
            fallback = reader.read().getConfig(actorSystemName);
            return (T)this;
        }

        protected Config merge() {
            Config config = ConfigFactory.parseMap(configHolder);
            if (fallback != null) {
                config = config.withFallback(fallback);
            }

            return config;
        }
    }
}
