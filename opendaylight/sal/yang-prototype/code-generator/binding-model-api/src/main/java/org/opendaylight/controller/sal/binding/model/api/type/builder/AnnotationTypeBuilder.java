/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api.type.builder;

import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.AnnotationType;
import org.opendaylight.controller.sal.binding.model.api.Type;

/**
 * Annotation Type Builder Interface serves for creation and instantiation of
 * immutable copy of Annotation Type. The Annotation Type Builder extends
 * from {@link Type} interface. The Annotation Type contains set of methods
 * which are capable to provide information about other Annotation Types and
 * Annotation Parameters.
 *
 * @see AnnotationType
 */
public interface AnnotationTypeBuilder extends Type {

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
     * Adds the parameter into List of parameters for Annotation Type.
     * <br>
     * If there is already stored parameter with the same name as the new
     * parameter, the value of the old one will be simply overwritten by the
     * newer parameter.
     * <br>
     * Neither the param name or value can contain
     * <code>null</code> references. In case that
     * any of parameters contains <code>null</code> the method SHOULD thrown
     * {@link IllegalArgumentException}
     *
     * @param paramName Parameter Name
     * @param value Parameter Value
     * @return <code>true</code> if the parameter has been successfully
     * assigned for Annotation Type
     */
    public boolean addParameter(final String paramName, String value);

    /**
     * Adds the parameter with specified List of parameter values into List of
     * parameters for Annotation Type.
     * <br>
     * If there is already stored parameter with the same name as the new
     * parameter, the value of the old one will be simply overwritten by the
     * newer parameter.
     * <br>
     * Neither the param name or value can contain
     * <code>null</code> references. In case that
     * any of parameters contains <code>null</code> the method SHOULD thrown
     * {@link IllegalArgumentException}
     *
     * @param paramName Parameter Name
     * @param values List of Values bounded to Parameter Name
     * @return <code>true</code> if the parameter has been successfully
     * assigned for Annotation Type
     */
    public boolean addParameters(final String paramName, List<String> values);

    /**
     * Returns <code>new</code> <i>immutable</i> instance of Annotation Type
     * with values assigned in current instance of Annotation Type Builder.
     * <br>
     * The return Annotation Type instance is immutable thus no additional
     * modification to Annotation Type Builder will have an impact to
     * instantiated Annotation Type.
     * <br>
     * For this purpose call this method after
     * all additions are complete.
     *
     * @return <code>new</code> <i>immutable</i> instance of Annotation Type.
     */
    public AnnotationType toInstance();
}
