/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api.type;

import org.opendaylight.controller.yang.model.api.ConstraintMetaDefinition;

/**
 * The Range Constraint interface is used to restrict integer and decimal
 * built-in types, or types derived from those.
 * <br>
 * A range consists of an explicit value consists of an explicit value, or a lower bound
 * returned by {@link #getMin()} and an upper bound returned by
 * {@link #getMax()}. <br>
 * <br>
 * Each explicit value and range boundary value given in
 * the range expression MUST match the type being restricted, or be one of the
 * special values "min" or "max". "min" and "max" mean the minimum and maximum
 * value accepted for the type being restricted, respectively
 * <br>
 * <br>
 * This interface was modeled according to definition in <a
 * href="https://tools.ietf.org/html/rfc6020#section-9.2.4">[RFC-6020] The
 * range Statement</a>.
 */
public interface RangeConstraint extends ConstraintMetaDefinition {

    /**
     * Returns the length-restricting lower bound value.
     * 
     * @return the length-restricting lower bound value.
     */
    Number getMin();

    /**
     * Returns the length-restricting upper bound value.
     * 
     * @return the length-restricting upper bound value.
     */
    Number getMax();
}
