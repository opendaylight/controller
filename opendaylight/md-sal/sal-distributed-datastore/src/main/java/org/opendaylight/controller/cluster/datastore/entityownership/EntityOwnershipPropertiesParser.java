package org.opendaylight.controller.cluster.datastore.entityownership;

import com.google.common.base.Preconditions;
import java.util.Dictionary;
import java.util.Enumeration;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategy;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityOwnershipPropertiesParser {

    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnershipPropertiesParser.class);
    private static final String ENTITY_TYPE_PREFIX = "entity.type.";

    public static EntityOwnerSelectionStrategyConfig parseProperties(final Dictionary<String, Object> properties) {
        final EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder();

        if (properties != null && !properties.isEmpty()) {
            final Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                final String key = keys.nextElement();
                if (!key.startsWith(ENTITY_TYPE_PREFIX)) {
                    LOG.debug("Ignoring non-conforming property key : {}");
                    continue;
                }

                final String[] strategyClassAndDelay = ((String) properties.get(key)).split(",");
                final Class<? extends EntityOwnerSelectionStrategy> aClass;
                try {
                    aClass = loadClass(strategyClassAndDelay[0]);
                } catch (final ClassNotFoundException e) {
                    LOG.error("Failed to load class {}, ignoring it", strategyClassAndDelay[0], e);
                    continue;
                }

                final long delay;
                if (strategyClassAndDelay.length > 1) {
                    delay = Long.parseLong(strategyClassAndDelay[1]);
                } else {
                    delay = 0;
                }

                final String entityType = key.substring(key.lastIndexOf(".") + 1);
                builder.addStrategy(entityType, aClass, delay);
                LOG.debug("Entity Type '{}' using strategy {} delay {}", entityType, aClass, delay);
            }
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends EntityOwnerSelectionStrategy> loadClass(final String strategyClassAndDelay)
            throws ClassNotFoundException {
        final Class<?> clazz;
        clazz = DistributedEntityOwnershipService.class.getClassLoader().loadClass(strategyClassAndDelay);

        Preconditions.checkArgument(EntityOwnerSelectionStrategy.class.isAssignableFrom(clazz),
                "Selected implementation %s must implement EntityOwnerSelectionStrategy, clazz");

        return (Class<? extends EntityOwnerSelectionStrategy>) clazz;
    }

}
