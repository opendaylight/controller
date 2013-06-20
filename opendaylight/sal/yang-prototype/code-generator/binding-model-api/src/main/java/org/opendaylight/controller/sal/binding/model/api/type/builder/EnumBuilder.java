/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api.type.builder;

import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.Type;

/**
 * Enum Builder is interface that contains methods to build and instantiate
 * Enumeration definition.
 *
 * @see Enumeration
 */
public interface EnumBuilder extends Type {

    /**
     * The method creates new AnnotationTypeBuilder containing specified
     * package name an annotation name.
     * <br>
     * Neither the package name or annotation name can contain
     * <code>null</code> references. In case that
     * any of parameters contains <code>null</code> the method SHOULD thrown
     * {@link IllegalArgumentException}
     *
     * @param packageName Package Name of Annotation Type
     * @param name Name of Annotation Type
     * @return <code>new</code> instance of Annotation Type Builder.
     */
    public AnnotationTypeBuilder addAnnotation(final String packageName, final String name);

    /**
     *
     * @param name
     * @param value
     */
    public void addValue(final String name, final Integer value);

    /**
     *
     * @param definingType
     * @return
     */
    public Enumeration toInstance(final Type definingType);
}
