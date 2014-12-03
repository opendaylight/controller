/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;

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
     * @param expectedServiceInterface MBean/MXBean interface which will back the proxy object.
     * @param objectName               ObjectName of dependent module without transaction name
     *                                 (platformON).
     * @param jmxAttribute             for reporting
     * @throws IllegalArgumentException when module is not found
     * @throws IllegalStateException    if module does not export this
     *                                  service interface.
     */
    void validateDependency(
            Class<? extends AbstractServiceInterface> expectedServiceInterface,
            ObjectName objectName, JmxAttribute jmxAttribute);

    /**
     * To be used during commit phase to wire actual dependencies.
     *
     * @return dependency instance using
     * {@link org.opendaylight.controller.config.spi.Module#getInstance()}
     * @throws IllegalArgumentException when module is not found
     */
    <T> T resolveInstance(Class<T> expectedType, ObjectName objectName,
                          JmxAttribute jmxAttribute);


    /**
     * To be used during commit phase to resolve identity-ref config attributes.
     *
     * @return actual class object generated from identity
     */
    <T extends BaseIdentity> Class<? extends T> resolveIdentity(IdentityAttributeRef identityRef, Class<T> expectedBaseClass);


    /**
     * Validate identity-ref config attribute.
     */
    <T extends BaseIdentity> void validateIdentity(IdentityAttributeRef identityRef, Class<T> expectedBaseClass, JmxAttribute jmxAttribute);

    /**
     * Can be used during validation or commit phase to get attribute value of dependent module.
     *
     * @param name      either direct ObjectName of a Module (type=Module) or service reference (type=ServiceReference) of dependent Module
     * @param attribute String identifying attribute name in JMX. Note that attributes start with upper case. See {@link org.opendaylight.controller.config.api.JmxAttribute#getAttributeName()}
     */
    Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException,
            InstanceNotFoundException, ReflectionException;


    /**
     * Helper method around {@link javax.management.JMX#newMXBeanProxy(javax.management.MBeanServerConnection, javax.management.ObjectName, Class)} }.
     * Returns MXBean proxy for dependent module. Can be used during validation or commit phase to inspect dependent module's attributes.
     *
     * @param objectName either direct ObjectName of a Module (type=Module) or service reference (type=ServiceReference) of dependent Module
     * @param interfaceClass MXBean interface to be used as a proxy
     * @param <T> type of proxy for type safe return value
     * @return instance of MXBean proxy
     */
    <T> T newMXBeanProxy(ObjectName objectName, Class<T> interfaceClass);

    /**
     * Check whether a dependency will be reused or (re)created. Useful when deciding if current module could be also reused.
     *
     * @param objectName ObjectName ID of a dependency
     * @param jmxAttribute JMXAttribute ID of a dependency
     * @return true if the dependency will be reused false otherwise
     */
    boolean canReuseDependency(ObjectName objectName, JmxAttribute jmxAttribute);
}
