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
 * The Method Signature interface contains simplified meta model for java
 * method definition. Each method MUST be defined by name, return type,
 * parameters and access modifier.
 * <br>
 * Additionally method MAY contain associated annotations and comment. By
 * contract if method does not contain any comments or annotation definitions
 * the {@link #getComment()} SHOULD rather return empty string and {@link
 * #getAnnotations()} SHOULD rather return empty list than <code>null</code>
 * values.
 * <br>
 * The defining Type contains the reference to Generated Type that declares
 * Method Signature.
 */
public interface MethodSignature extends TypeMember {

    /**
     * Returns <code>true</code> if the method signature is defined as abstract.
     * <br>
     * By default in java all method declarations in interface are defined as abstract,
     * but the user don't need necessary to declare abstract keyword in front of each method.
     * <br>
     * The abstract methods are allowed in Class definitions but only when the class is declared as abstract.
     *
     * @return <code>true</code> if the method signature is defined as abstract.
     */
    public boolean isAbstract();

    /**
     * Returns the List of parameters that method declare. If the method does
     * not contain any parameters, the method will return empty List.
     *
     * @return the List of parameters that method declare.
     */
    public List<Parameter> getParameters();

    /**
     * The Parameter interface is designed to hold the information of method
     * Parameter(s). The parameter is defined by his Name which MUST be
     * unique as java does not allow multiple parameters with same names for
     * one method and Type that is associated with parameter.
     */
    interface Parameter {

        /**
         * Returns the parameter name.
         *
         * @return the parameter name.
         */
        public String getName();

        /**
         * Returns Type that is bounded to parameter name.
         *
         * @return Type that is bounded to parameter name.
         */
        public Type getType();
    }
}
