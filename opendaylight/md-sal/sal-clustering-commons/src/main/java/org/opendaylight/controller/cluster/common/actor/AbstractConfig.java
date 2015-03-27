package org.opendaylight.controller.cluster.common.actor;

import com.google.common.base.Preconditions;
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

        protected Config merge(){
            if (fallback == null)
                fallback = ConfigFactory.load().getConfig(actorSystemName);

            return ConfigFactory.parseMap(configHolder).withFallback(fallback);
        }
    }
}
