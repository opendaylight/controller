package org.opendaylight.controller.cluster.common.actor;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractConfig implements UnifiedConfig {

    private Config config;

    public AbstractConfig(Config config){
        this.config = config;
    }

    @Override
    public Config get() {
        return config;
    }

    protected static Config merge(Map<String, Object> customConfig, String configkey){
        Config fallback = ConfigFactory.load().getConfig(configkey);
        return ConfigFactory.parseMap(customConfig).withFallback(fallback);
    }

    protected static Config merge(Map<String, Object> customConfig, Config fallback){
        return ConfigFactory.parseMap(customConfig).withFallback(fallback);
    }

    public static abstract class Builder<T extends Builder>{
        protected Map<String, Object> configHolder;

        public Builder(){
            configHolder = new HashMap<>();
        }
    }
}
