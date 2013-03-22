
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   ICacheUpdateAware.java
 *
 * @brief Interface for getting clustered cache updates
 *
 * Interface that needs to be implemented by the components
 * that want to be informed of an update in a Clustered Cache. The
 * interface need to be registerd in the OSGi service registry with a
 * property "cachenames" which will contains a PropertySet object
 * listing all the caches for which their is interes.
 */
package org.opendaylight.controller.clustering.services;

/**
 * Interface that needs to be implemented by the components
 * that want to be informed of an update in a Clustered Cache. The
 * interface need to be registerd in the OSGi service registry with a
 * property "cachenames" which will contains a PropertySet object
 * listing all the caches for which their is interes.
 *
 */
public interface ICacheUpdateAware<K, V> {
    /**
     * Invoked when a new entry is available in the cache, the key is
     * only provided, the value will come as an entryUpdate invocation
     *
     * @param key Key for the entry just created
     * @param cacheName name of the cache for which update has been
     * received
     * @param originLocal true if the event is generated from this
     * node
     */
    void entryCreated(K key, String cacheName, boolean originLocal);

    /**
     * Called anytime a given entry is updated
     *
     * @param key Key for the entry modified
     * @param new_value the new value the key will have
     * @param cacheName name of the cache for which update has been
     * received
     * @param originLocal true if the event is generated from this
     * node
     */
    void entryUpdated(K key, V new_value, String cacheName, boolean originLocal);

    /**
     * Called anytime a given key is removed from the
     * ConcurrentHashMap we are listening to.
     *
     * @param key Key of the entry removed
     * @param cacheName name of the cache for which update has been
     * received
     * @param originLocal true if the event is generated from this
     * node
     */
    void entryDeleted(K key, String cacheName, boolean originLocal);
}
