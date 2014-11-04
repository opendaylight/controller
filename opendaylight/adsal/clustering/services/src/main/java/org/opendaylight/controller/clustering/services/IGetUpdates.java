
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IGetUpdates.java
 *
 * @brief  Interface that needs to be implemented by the listeners of
 * updates received on data structure shared in the cluster
 *
 * Interface that needs to be implemented by the listeners of updates
 * received on data structure shared in the cluster
 */
package org.opendaylight.controller.clustering.services;

/**
 * @deprecated for internal use
 * Interface that needs to be implemented by the listeners of
 * updates received on data structure shared in the cluster
 */
public interface IGetUpdates<K, V> {
    /**
     * Invoked when a new entry is available in the cache, the key is
     * only provided, the value will come as an entryUpdate invocation
     *
     * @param key Key for the entry just created
     * @param containerName container for which the update has been received
     * @param cacheName name of the cache for which update has been received
     */
    void entryCreated(K key, String containerName, String cacheName,
            boolean local);

    /**
     * Called anytime a given entry is updated
     *
     * @param key Key for the entry modified
     * @param new_value the new value the key will have
     * @param containerName container for which the update has been received
     * @param cacheName name of the cache for which update has been received
     */
    void entryUpdated(K key, V new_value, String containerName,
            String cacheName, boolean local);

    /**
     * Called anytime a given key is removed from the
     * ConcurrentHashMap we are listening to.
     *
     * @param key Key of the entry removed
     * @param containerName container for which the update has been received
     * @param cacheName name of the cache for which update has been received
     */
    void entryDeleted(K key, String containerName, String cacheName,
            boolean originLocal);
}
