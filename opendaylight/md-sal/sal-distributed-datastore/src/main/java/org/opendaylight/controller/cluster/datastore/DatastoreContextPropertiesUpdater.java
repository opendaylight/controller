/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update DatastoreContext settings on invoke update method.
 *
 */
public class DatastoreContextPropertiesUpdater implements AutoCloseable {

    public interface Listener {
        void onDatastoreContextUpdated(DatastoreContextFactory contextFactory);
    }

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreContextPropertiesUpdater.class);

    private final DatastoreContextIntrospector introspector;
    private Listener listener;

    /**
     * Base init of updater for DatastoreContext settings with base properties.
     *
     * @param introspector
     *            - introspection on DatastoreContext
     * @param props
     *            - base properties
     */
    public DatastoreContextPropertiesUpdater(final DatastoreContextIntrospector introspector,
            final Dictionary<String, Object> props) {
        this.introspector = introspector;
        update(props);
    }

    public void setListener(final Listener listener) {
        this.listener = listener;
    }

    public void update(final Dictionary<String, Object> properties) {
        LOG.debug("Overlaying settings: {}", properties);

        if (introspector.update(properties) && listener != null) {
            listener.onDatastoreContextUpdated(introspector.newContextFactory());
        }
    }

    @Override
    public void close() {
        listener = null;
    }
}
