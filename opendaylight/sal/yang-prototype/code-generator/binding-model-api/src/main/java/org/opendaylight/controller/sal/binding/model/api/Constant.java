/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api;

/**
 * Interface Contact is designed to hold and model java constant. In Java
 * there are no constant keywords instead of the constant is defined as
 * static final field with assigned value. For this purpose the Constant
 * interface contains methods {@link #getType()} to provide wrapped return
 * Type of Constant, {@link #getName()} the Name of constant and the {@link
 * #getValue()} for providing of value assigned to Constant. To determine of
 * which type the constant value is it is recommended firstly to retrieve
 * Type from constant. The Type interface holds base information like java
 * package name and java type name (e.g. fully qualified name). From this
 * string user should be able to determine to which type can be {@link
 * #getValue()} type typecasted to unbox and provide value assigned to
 * constant.
 */
public interface Constant {

    /**
     * Returns the Type that declares constant.
     *
     * @return the Type that declares constant.
     */
    public Type getDefiningType();

    /**
     * Returns the return Type (or just Type) of the Constant.
     *
     * @return the return Type (or just Type) of the Constant.
     */
    public Type getType();

    /**
     * Returns the name of constant.
     * <br>
     * By conventions the name SHOULD be in CAPITALS separated with
     * underscores.
     *
     * @return the name of constant.
     */
    public String getName();

    /**
     * Returns boxed value that is assigned for context.
     *
     * @return boxed value that is assigned for context.
     */
    public Object getValue();

    /**
     * Returns Constant definition in formatted string.
     * <br>
     * <br>
     * The expected string SHOULD be in format: <code>public final
     * static [Type] CONSTANT_NAME = [value];</code>
     *
     * @return Constant definition in formatted string.
     */
    public String toFormattedString();
}
