/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;

import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

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
     * @throws IllegalArgumentException when module is not found
     * @throws IllegalStateException if module does not export this
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
     * @throws IllegalArgumentException when module is not found
     */
    <T> T resolveInstance(Class<T> expectedType, ObjectName objectName,
            JmxAttribute jmxAttribute);

    // TODO finish javadoc

    /**
     * To be used during commit phase to resolve identity-ref config attributes.
     *
     * @return actual class object generated from identity
     */
    <T extends BaseIdentity> Class<? extends T> resolveIdentity(IdentityAttributeRef identityRef, Class<T> expectedBaseClass);

    <T extends BaseIdentity> void validateIdentity(IdentityAttributeRef identityRef, Class<T> expectedBaseClass, JmxAttribute jmxAttribute);

    /**
     * Method that can be used during validation phase, to obtain dependencies of certain service interface dynamically.
     * Calling this method is advised before using @{#resolveInstances} as possible cycles will be detected sooner.
     *
     * @param expectedServiceInterface service interface that must be implemented. This class must be annotated with {@link org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation}
     * @return all modules currently registered in transaction
     */
    Set<ModuleIdentifier> validateDependencies(Class<? extends AbstractServiceInterface> expectedServiceInterface);

    boolean containsDependency(ModuleIdentifier moduleIdentifier);

    /**
     * During second phase commit, resolve dependencies that implement service interface dynamically.
     *
     * @param expectedServiceInterface service interface that must be implemented. This class must be annotated with {@link org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation}
     * @return resolved instances indexed by their module identifier
     */
    Map<ModuleIdentifier, AutoCloseable> resolveInstances(
            Class<? extends AbstractServiceInterface> expectedServiceInterface);


    /**
     * Type safe wrapper around {@link #resolveInstances(Class)}
     * @param expectedServiceInterface service interface that must be implemented. This class must be annotated with {@link org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation}
     * @param expectedInstanceInterface type to which each instance should be cast. It is advised to use what {@link org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation#osgiRegistrationType()} announces
     * @param <T>
     * @return resolved instances indexed by their module identifier
     */
    <T> Map<ModuleIdentifier, T> resolveInstances(
            Class<? extends AbstractServiceInterface> expectedServiceInterface,
            Class<T> expectedInstanceInterface);

}
