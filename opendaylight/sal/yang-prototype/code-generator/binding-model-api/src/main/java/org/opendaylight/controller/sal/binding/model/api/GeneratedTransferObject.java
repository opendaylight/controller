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
 * Generated Transfer Object extends {@link GeneratedType} and is designed to
 * represent Java Class. The Generated Transfer Object contains declarations of
 * member fields stored in List of Properties. The Generated Transfer Object can
 * be extended by exactly ONE Generated Transfer Object as Java don't allow
 * multiple inheritance. For retrieval of implementing Generated Types use
 * {@link #getImplements()} method. <br>
 * Every transfer object SHOULD contain equals, hashCode and toString
 * definitions. For this purpose retrieve definitions through
 * {@link #getEqualsIdentifiers ()}, {@link #getHashCodeIdentifiers()} and
 * {@link #getToStringIdentifiers ()}.
 * 
 */
public interface GeneratedTransferObject extends GeneratedType {

    /**
     * Returns the extending Generated Transfer Object or <code>null</code> if
     * there is no extending Generated Transfer Object.
     * 
     * @return the extending Generated Transfer Object or <code>null</code> if
     *         there is no extending Generated Transfer Object.
     */
    public GeneratedTransferObject getExtends();

    /**
     * Returns List of Properties that are declared for Generated Transfer
     * Object.
     * 
     * @return List of Properties that are declared for Generated Transfer
     *         Object.
     */
    public List<GeneratedProperty> getProperties();

    /**
     * Returns List of Properties that are designated to define equality for
     * Generated Transfer Object.
     * 
     * @return List of Properties that are designated to define equality for
     *         Generated Transfer Object.
     */
    public List<GeneratedProperty> getEqualsIdentifiers();

    /**
     * Returns List of Properties that are designated to define identity for
     * Generated Transfer Object.
     * 
     * @return List of Properties that are designated to define identity for
     *         Generated Transfer Object.
     */
    public List<GeneratedProperty> getHashCodeIdentifiers();

    /**
     * Returns List of Properties that will be members of toString definition
     * for Generated Transfer Object.
     * 
     * @return List of Properties that will be members of toString definition
     *         for Generated Transfer Object.
     */
    public List<GeneratedProperty> getToStringIdentifiers();

    /**
     * Return boolean value which describe whether Generated Transfer Object
     * was/wasn't created from union YANG type.
     * 
     * @return true value if Generated Transfer Object was created from union
     *         YANG type.
     */
    @Deprecated
    public boolean isUnionType();
}
