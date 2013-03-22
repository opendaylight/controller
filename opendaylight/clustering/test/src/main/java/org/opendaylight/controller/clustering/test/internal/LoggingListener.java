
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.test.internal;

import org.opendaylight.controller.clustering.services.IGetUpdates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingListener implements IGetUpdates<Integer, StringContainer> {
    protected static Logger logger = LoggerFactory
            .getLogger(LoggingListener.class);

    @Override
    public void entryCreated(Integer key, String containerName,
            String cacheName, boolean originLocal) {
        logger.debug(" Cache entry with key " + key + " created in cache "
                + cacheName);
    }

    @Override
    public void entryUpdated(Integer key, StringContainer new_value,
            String containerName, String cacheName, boolean originLocal) {
        logger.debug(" Cache entry with key " + key + " modified to value "
                + new_value + "  in cache " + cacheName);
    }

    @Override
    public void entryDeleted(Integer key, String containerName,
            String cacheName, boolean originLocal) {
        logger.debug(" Cache entry with key " + key + " removed in cache "
                + cacheName);
    }
}
