/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.selectionstrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class EntityOwnerSelectionStrategyConfigReaderTest {

    @Test
    public void testReadStrategies() {
        final Map<Object, Object> props = new java.util.HashMap<>();
        props.put("entity.type.test",
            "org.opendaylight.controller.cluster.entityownership.selectionstrategy.LastCandidateSelectionStrategy,100");


        final EntityOwnerSelectionStrategyConfig config = EntityOwnerSelectionStrategyConfigReader
                .loadStrategyWithConfig(props);

        assertTrue(config.isStrategyConfigured("test"));

        final EntityOwnerSelectionStrategy strategy = config.createStrategy("test",
                Collections.<String, Long>emptyMap());
        assertTrue(strategy.toString(), strategy instanceof LastCandidateSelectionStrategy);
        assertEquals(100L, strategy.getSelectionDelayInMillis());

        final EntityOwnerSelectionStrategy strategy1 = config.createStrategy("test", Collections.emptyMap());
        assertEquals(strategy, strategy1);

        config.clearStrategies();

        final EntityOwnerSelectionStrategy strategy2 = config.createStrategy("test", Collections.emptyMap());
        assertNotEquals(strategy1, strategy2);
    }

    @Test
    public void testReadStrategiesWithEmptyConfiguration() {

        final Map<Object, Object> props = new HashMap<>();
        final EntityOwnerSelectionStrategyConfig config = EntityOwnerSelectionStrategyConfigReader
                .loadStrategyWithConfig(props);

        assertFalse(config.isStrategyConfigured("test"));
    }

    @Test
    public void testReadStrategiesWithNullConfiguration() {
        final EntityOwnerSelectionStrategyConfig config = EntityOwnerSelectionStrategyConfigReader
                .loadStrategyWithConfig(null);
        assertFalse(config.isStrategyConfigured("test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadStrategiesInvalidDelay() {
        final Map<Object, Object> props = new HashMap<>();
        props.put("entity.type.test",
            "org.opendaylight.controller.cluster.entityownership.selectionstrategy.LastCandidateSelectionStrategy,foo");
        EntityOwnerSelectionStrategyConfigReader.loadStrategyWithConfig(props);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadStrategiesInvalidClassType() {
        final Map<Object, Object> props = new HashMap<>();
        props.put("entity.type.test", "String,100");
        EntityOwnerSelectionStrategyConfigReader.loadStrategyWithConfig(props);
    }

    @Test
    public void testReadStrategiesMissingDelay() {
        final Map<Object, Object> props = new HashMap<>();
        props.put("entity.type.test",
            "org.opendaylight.controller.cluster.entityownership.selectionstrategy.LastCandidateSelectionStrategy,100");
        props.put("entity.type.test1",
            "org.opendaylight.controller.cluster.entityownership.selectionstrategy.LastCandidateSelectionStrategy");


        final EntityOwnerSelectionStrategyConfig config = EntityOwnerSelectionStrategyConfigReader
                .loadStrategyWithConfig(props);

        assertEquals(100, config.createStrategy("test", Collections.emptyMap()).getSelectionDelayInMillis());
        assertEquals(0, config.createStrategy("test2", Collections.emptyMap()).getSelectionDelayInMillis());
    }
}
