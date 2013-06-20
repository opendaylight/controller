/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api;

import java.util.List;

public interface TypeMember {

    /**
     * Returns List of annotation definitions associated with generated type.
     *
     * @return List of annotation definitions associated with generated type.
     */
    public List<AnnotationType> getAnnotations();

    /**
     * Returns the name of method.
     *
     * @return the name of method.
     */
    public String getName();

    /**
     * Returns comment string associated with method.
     *
     * @return comment string associated with method.
     */
    public String getComment();

    /**
     * Returns the Type that declares method.
     *
     * @return the Type that declares method.
     */
    public Type getDefiningType();

    /**
     * Returns the access modifier of method.
     *
     * @return the access modifier of method.
     */
    public AccessModifier getAccessModifier();

    /**
     * Returns the returning Type that methods returns.
     *
     * @return the returning Type that methods returns.
     */
    public Type getReturnType();

    /**
     * Returns <code>true</code> if method is declared as final.
     *
     * @return <code>true</code> if method is declared as final.
     */
    public boolean isFinal();
}
