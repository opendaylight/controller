/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils;

import javax.inject.Provider;

public abstract class AbstractObjectRepositoryBasedLookup {

    private ObjectRegistry objectRepository;
    private Provider<ObjectRegistry> objectRepositoryProvider;

    public void setObjectRegistry(ObjectRegistry objectRepository) {
        this.objectRepository = objectRepository;
    }

    public void setObjectRegistryProvider(Provider<ObjectRegistry> objectRepositoryProvider) {
        this.objectRepositoryProvider = objectRepositoryProvider;
    }

    protected ObjectRegistry getObjectRepository() {
        if (objectRepository == null) {
            if (objectRepositoryProvider == null) {
                throw new IllegalStateException("Must call setObjectRepository() first");
            } else {
                objectRepository = objectRepositoryProvider.get();
            }
        }
        return objectRepository;
    }

}
