/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import javax.management.ObjectName;

import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * Each new {@link org.opendaylight.controller.config.spi.Module} can receive
 * resolver from {@link org.opendaylight.controller.config.spi.ModuleFactory}
 * for looking up dependencies during validation and second phase commit.
 *
 * @see org.opendaylight.controller.config.spi.Module
 */
public interface DependencyResolver extends Identifiable<ModuleIdentifier> {

    /**
     * To be used during validation phase to validate serice interface of
     * dependent module.
     *
     * @param expectedServiceInterface
     *            MBean/MXBean interface which will back the proxy object.
     * @param objectName
     *            ObjectName of dependent module without transaction name
     *            (platformON).
     * @param jmxAttribute
     * @throws {@link IllegalArgumentException} when module is not found
     * @throws {@link IllegalStateException} if module does not export this
     *         service interface.
     */
    void validateDependency(
            Class<? extends AbstractServiceInterface> expectedServiceInterface,
            ObjectName objectName, JmxAttribute jmxAttribute);

    /**
     * To be used during commit phase to wire actual dependencies.
     *
     * @return dependency instance using
     *         {@link org.opendaylight.controller.config.spi.Module#getInstance()}
     * @throws {@link IllegalArgumentException} when module is not found
     */
    <T> T resolveInstance(Class<T> expectedType, ObjectName objectName,
            JmxAttribute jmxAttribute);

}
