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
 * Exports attribute and module class descriptions. Description annotation can
 * be applied to module directly or to its super class or MXBean interface.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {
    /**
     * Returns a human-readable description of the annotated attribute.
     * Descriptions should be clear and concise, describing what the attribute
     * affects.
     *
     * @return attribute description
     */
    String value();
}
