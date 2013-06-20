/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api.type.builder;

import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;

/**
 * Generated Transfer Object Builder is interface that contains methods to
 * build and instantiate Generated Transfer Object definition.
 *
 * @see GeneratedTransferObject
 */
public interface GeneratedTOBuilder extends GeneratedTypeBuilder {

    /**
     * Add Generated Transfer Object from which will be extended current
     * Generated Transfer Object.
     * <br>
     * By definition Java does not allow multiple
     * inheritance, hence if there is already definition of Generated
     * Transfer Object the extending object will be overwritten by lastly
     * added Generated Transfer Object.
     * <br>
     * If Generated Transfer Object is <code>null</code> the method SHOULD
     * throw {@link IllegalArgumentException}
     *
     * @param genTransObj Generated Transfer Object
     */
    public void setExtendsType(final GeneratedTransferObject genTransObj);

    /**
     * Add new Generated Property definition for Generated Transfer Object
     * Builder and returns Generated Property Builder for specifying Property.
     * <br>
     * Name of Property cannot be <code>null</code>,
     * if it is <code>null</code> the method SHOULD throw {@link IllegalArgumentException}
     *
     * @param name Name of Property
     * @return <code>new</code> instance of Generated Property Builder.
     */
    public GeneratedPropertyBuilder addProperty(final String name);

    /**
     * Add Property that will be part of <code>equals</code> definition.
     * <br>
     * If Generated Property Builder is <code>null</code> the method SHOULD
     * throw {@link IllegalArgumentException}
     *
     * @param property Generated Property Builder
     * @return <code>true</code> if addition of Generated Property into list
     * of <code>equals</code> properties is successful.
     */
    public boolean addEqualsIdentity(final GeneratedPropertyBuilder property);

    /**
     * Add Property that will be part of <code>hashCode</code> definition.
     * <br>
     * If Generated Property Builder is <code>null</code> the method SHOULD
     * throw {@link IllegalArgumentException}
     *
     * @param property Generated Property Builder
     * @return <code>true</code> if addition of Generated Property into list
     * of <code>hashCode</code> properties is successful.
     */
    public boolean addHashIdentity(final GeneratedPropertyBuilder property);

    /**
     * Add Property that will be part of <code>toString</code> definition.
     * <br>
     * If Generated Property Builder is <code>null</code> the method SHOULD
     * throw {@link IllegalArgumentException}
     *
     * @param property Generated Property Builder
     * @return <code>true</code> if addition of Generated Property into list
     * of <code>toString</code> properties is successful.
     */
    public boolean addToStringProperty(final GeneratedPropertyBuilder property);

    @Override
    public GeneratedTransferObject toInstance();
}
