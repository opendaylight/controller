/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api;

import java.util.List;

/**
 * The Annotation Type interface is designed to hold information about
 * annotation for any type that could be annotated in Java.
 * <br>
 * For sake of simplicity the Annotation Type is not designed to model exact
 * behaviour of annotation mechanism, but just to hold information needed to
 * model annotation over java Type definition.
 */
public interface AnnotationType extends Type {

    /**
     * Returns the List of Annotations.
     * <br>
     * Each Annotation Type MAY have defined multiple Annotations.
     *
     * @return the List of Annotations.
     */
    public List<AnnotationType> getAnnotations();

    /**
     * Returns Parameter Definition assigned for given parameter name.
     * <br>
     * If Annotation does not contain parameter with specified param name,
     * the method MAY return <code>null</code> value.
     *
     * @param paramName Parameter Name
     * @return Parameter Definition assigned for given parameter name.
     */
    public Parameter getParameter(final String paramName);

    /**
     * Returns List of all parameters assigned to Annotation Type.
     *
     * @return List of all parameters assigned to Annotation Type.
     */
    public List<Parameter> getParameters();

    /**
     * Returns List of parameter names.
     *
     * @return List of parameter names.
     */
    public List<String> getParameterNames();

    /**
     * Returns <code>true</code> if annotation contains parameters.
     *
     * @return <code>true</code> if annotation contains parameters.
     */
    public boolean containsParameters();

    /**
     * Annotation Type parameter interface. For simplicity the Parameter
     * contains values and value types as Strings. Every annotation which
     * contains parameters could contain either single parameter or array of
     * parameters. To model this purposes the by contract if the parameter
     * contains single parameter the {@link #getValues()} method will return
     * empty List and {@link #getValue()} MUST always return non-<code>null</code>
     * parameter. If the Parameter holds List of values the singular {@link
     * #getValue()} parameter MAY return <code>null</code> value.
     */
    interface Parameter {

        /**
         * Returns the Name of the parameter.
         *
         * @return the Name of the parameter.
         */
        public String getName();

        /**
         * Returns value in String format if Parameter contains singular value,
         * otherwise MAY return <code>null</code>.
         *
         * @return value in String format if Parameter contains singular value.
         */
        public String getValue();

        /**
         * Returns List of Parameter assigned values in order in which they
         * were assigned for given parameter name.
         * <br>
         * If there are multiple values assigned for given parameter name the
         * method MUST NOT return empty List.
         *
         * @return List of Parameter assigned values in order in which they
         * were assigned for given parameter name.
         */
        public List<String> getValues();
    }
}
