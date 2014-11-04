/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.internal;

import java.util.Map;

import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.Status;

/**
 * The class ControllerProperties is a global store
 * for controller properties. Use this api to store/retrieve properties
 * that are container independent.
 *
 * This is visible only to switch manager. Hence its a part of
 * switch manager internal bundle.
 */
public interface IControllerProperties {

    /**
     * Return all the global properties of the controller
     *
     * @return map of {@link org.opendaylight.controller.sal.core.Property} such
     *         as {@link org.opendaylight.controller.sal.core.Description}
     *         and/or {@link org.opendaylight.controller.sal.core.Tier} etc.
     */
    public Map<String, Property> getControllerProperties();

    /**
     * Return a specific property of the controller given the property name
     *
     * @param propertyName
     *            the property name specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return {@link org.opendaylight.controller.sal.core.Property}
     */
    public Property getControllerProperty(String propertyName);

    /**
     * Set a specific property of the controller
     *
     * @param property
     *            {@link org.opendaylight.controller.sal.core.Property}
     * @return Status
     */
    public Status setControllerProperty(Property property);

    /**
     * Remove a property of a node
     *
     * @param propertyName
     *            the property name specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return success or failed reason
     */
    public Status removeControllerProperty(String propertyName);

}
