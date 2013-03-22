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
 * Every Java interface has to be specified with:
 * <ul>
 * <li><code>package</code> that belongs into</li>
 * <li><code>interface</code> name (with commentary that <b>SHOULD</b> be
 * present to proper define interface and base <i>contracts</i> specified for
 * interface)</li>
 * <li><code>enum</code> and <code>constant</code> definitions (i.e. each
 * constant definition is by default defined as <code>public static final</code>
 * + type (either primitive or object) and constant name</li>
 * <li><code>method definitions</code> with specified input parameters (with
 * types) and return values</li>
 * </ul>
 * 
 * By the definition of the interface constant, enum and method definitions MUST
 * be public, so there is no need to specify the scope of visibility.
 * 
 * 
 */
public interface GeneratedType extends Type {

    public Type getParentType();

    /**
     * Returns Set of all Enumerator definitions associated with interface.
     * 
     * @return Set of all Enumerator definitions associated with interface.
     */
    public List<Enumeration> getEnumDefintions();

    /**
     * 
     * 
     * @return
     */
    public List<Constant> getConstantDefinitions();

    /**
     * 
     * 
     * @return
     */
    public List<MethodSignature> getMethodDefinitions();

}
