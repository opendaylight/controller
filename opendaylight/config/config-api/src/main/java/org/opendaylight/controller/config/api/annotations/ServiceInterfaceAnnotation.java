/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface implemented by
 * {@link org.opendaylight.controller.config.spi.Module} as a Service Interface.
 * Each service interface is identified by globally unique and human readable
 * name. By convention the name is all lower case without spaces.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ServiceInterfaceAnnotation {

    /**
     * Specifies human readable name of this service. Each service name should
     * be globally unique. Should not contain spaces.
     */
    String value();

    /**
     * Mandatory class which will be used as key for OSGi service registration
     * once {@link org.opendaylight.controller.config.spi.Module#getInstance()}
     * is called.
     */
    Class<?> osgiRegistrationType();
}
