
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.services_implementation.internal;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.opendaylight.controller.clustering.services.IGetUpdates;

@Listener
public class CacheListenerContainer {
    private IGetUpdates toBeUpdated;
    private String containerName;
    private String cacheName;

    public CacheListenerContainer(IGetUpdates i, String containerName,
            String cacheName) {
        this.toBeUpdated = i;
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    public IGetUpdates whichListener() {
        return this.toBeUpdated;
    }

    @CacheEntryCreated
    public void observeCreate(CacheEntryCreatedEvent<Object, Object> event) {
        if (event.isPre()) {
            return;
        }

        if (this.toBeUpdated != null) {
            this.toBeUpdated.entryCreated(event.getKey(), this.containerName,
                    this.cacheName, event.isOriginLocal());
        }
    }

    @CacheEntryModified
    public void observeModify(CacheEntryModifiedEvent<Object, Object> event) {
        if (event.isPre()) {
            return;
        }

        if (this.toBeUpdated != null) {
            this.toBeUpdated.entryUpdated(event.getKey(), event.getValue(),
                    this.containerName, this.cacheName, event.isOriginLocal());
        }
    }

    @CacheEntryRemoved
    public void observeRemove(CacheEntryRemovedEvent<Object, Object> event) {
        if (event.isPre()) {
            return;
        }

        if (this.toBeUpdated != null) {
            this.toBeUpdated.entryDeleted(event.getKey(), this.containerName,
                    this.cacheName, event.isOriginLocal());
        }
    }
}
