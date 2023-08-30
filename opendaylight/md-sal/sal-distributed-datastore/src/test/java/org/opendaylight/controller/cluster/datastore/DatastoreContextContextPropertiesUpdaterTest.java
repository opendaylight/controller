/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.cluster.datastore.DatastoreContextIntrospectorTest.INTROSPECTOR_FACTORY;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DatastoreContextPropertiesUpdater.Listener;

public class DatastoreContextContextPropertiesUpdaterTest {

    @SuppressWarnings("unchecked")
    @Test
    public void updateOnConstructionTest() throws Exception {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("shardTransactionIdleTimeoutInMinutes", 10);

        final DatastoreContextIntrospector introspector = INTROSPECTOR_FACTORY.newInstance(CONFIGURATION);

        final DatastoreContextPropertiesUpdater updater = new DatastoreContextPropertiesUpdater(introspector,
                properties);
        assertNotNull(updater);

        final Map<String, Object> props = (Map<String, Object>) resolveField("currentProperties", introspector);

        assertEquals(props.get("shardTransactionIdleTimeoutInMinutes"), 10);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onUpdateTest() throws Exception {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("shardTransactionIdleTimeoutInMinutes", 10);

        final DatastoreContextIntrospector introspector = INTROSPECTOR_FACTORY.newInstance(CONFIGURATION);
        assertNotNull(introspector);
        final DatastoreContextPropertiesUpdater updater = new DatastoreContextPropertiesUpdater(introspector,
                properties);
        assertNotNull(updater);

        Map<String, Object> props = (Map<String, Object>) resolveField("currentProperties", introspector);
        assertTrue(!props.isEmpty());

        properties.put("shardTransactionIdleTimeoutInMinutes", 20);
        updater.update(properties);

        props = (Map<String, Object>) resolveField("currentProperties", introspector);
        assertEquals(props.get("shardTransactionIdleTimeoutInMinutes"), 20);
    }

    @SuppressWarnings("resource")
    @Test
    public void listenerTest() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("shardTransactionIdleTimeoutInMinutes", 10);

        final DatastoreContextIntrospector introspector = INTROSPECTOR_FACTORY.newInstance(CONFIGURATION);
        final DatastoreContextPropertiesUpdater updater = new DatastoreContextPropertiesUpdater(introspector,
                properties);
        final DummyListenerImpl dummyListener = new DummyListenerImpl();
        updater.setListener(dummyListener);

        assertTrue(dummyListener.getContextFactory() == null);
        updater.setListener(dummyListener);
        properties.put("shardTransactionIdleTimeoutInMinutes", 20);
        updater.update(properties);

        final DatastoreContextFactory contextFactory = dummyListener.getContextFactory();
        assertNotNull(contextFactory);
        updater.close();
    }

    private static Object resolveField(final String name, final Object obj) throws Exception {
        final Field currProps = obj.getClass().getDeclaredField(name);
        currProps.setAccessible(true);
        return currProps.get(obj);
    }

    private static final class DummyListenerImpl implements Listener {
        private DatastoreContextFactory contextFactory;

        @Override
        @SuppressWarnings("checkstyle:hiddenField")
        public void onDatastoreContextUpdated(final DatastoreContextFactory contextFactory) {
            this.contextFactory = contextFactory;
        }

        public DatastoreContextFactory getContextFactory() {
            return contextFactory;
        }
    }
}
