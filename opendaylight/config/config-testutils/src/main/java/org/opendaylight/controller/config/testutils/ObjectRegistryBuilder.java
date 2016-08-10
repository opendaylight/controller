/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils;

import org.opendaylight.controller.config.testutils.ObjectRegistry.SimpleObjectRegistry;

public class ObjectRegistryBuilder implements ObjectRegistry.Builder {

    final SimpleObjectRegistry buildingRegistry = new SimpleObjectRegistry();

    @Override
    public <T> void putInstance(T object, Class<T> lookupType) throws IllegalArgumentException {
        buildingRegistry.putInstance(object, lookupType);
    }

    @Override
    public ObjectRegistry build() {
        return new SimpleObjectRegistry(buildingRegistry);
    }

}
