/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.services_implementation.internal;

import org.opendaylight.controller.clustering.services.IGetUpdates;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;

public class GetUpdatesContainer<K,V> implements IGetUpdates<K,V> {
    private ICacheUpdateAware<K,V> toBeUpdated;
    private String containerName;
    private String cacheName;

    public GetUpdatesContainer(ICacheUpdateAware<K,V> i, String containerName,
                               String cacheName) {
        this.toBeUpdated = i;
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    public ICacheUpdateAware<K,V> whichListener() {
        return this.toBeUpdated;
    }

    @Override
    public void entryCreated(K key, String containerName, String cacheName,
                             boolean local) {
        if (this.toBeUpdated != null) {
            this.toBeUpdated.entryCreated(key, cacheName, local);
        }
    }

    @Override
    public void entryUpdated(K key, V new_value, String containerName,
                             String cacheName,
                             boolean local) {
        if (this.toBeUpdated != null) {
            this.toBeUpdated.entryUpdated(key, new_value, cacheName, local);
        }
    }

    @Override
    public void entryDeleted(K key, String containerName, String cacheName,
                             boolean local) {
        if (this.toBeUpdated != null) {
            this.toBeUpdated.entryDeleted(key, cacheName, local);
        }
    }
}
