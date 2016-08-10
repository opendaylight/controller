/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils;

import javax.management.ObjectName;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;

/**
 * DependencyResolver based on an ObjectRegistry.
 *
 * @author Michael Vorburger
 */
public abstract class ObjectRepositoryDependencyResolver
    extends AbstractObjectRepositoryBasedLookup
    implements DependencyResolver {

    @Override
    public <T> T resolveInstance(Class<T> expectedType, ObjectName objectName, JmxAttribute jmxAttribute) {
        if (objectName != null) {
            throw new IllegalArgumentException("resolveInstance() by objectName not supported, only by expectedType");
        }
        return getObjectRepository().getInstanceOrException(expectedType);
    }

}
