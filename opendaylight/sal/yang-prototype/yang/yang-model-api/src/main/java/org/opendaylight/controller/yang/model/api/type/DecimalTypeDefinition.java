/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api.type;

import java.util.List;

import org.opendaylight.controller.yang.model.api.TypeDefinition;

public interface DecimalTypeDefinition extends
        TypeDefinition<DecimalTypeDefinition> {

    List<RangeConstraint> getRangeStatements();

    /**
     * Returns integer between 1 and 18 inclusively. <br>
     * <br>
     * 
     * The "fraction-digits" statement controls the size of the minimum
     * difference between values of a decimal64 type, by restricting the value
     * space to numbers that are expressible as "i x 10^-n" where n is the
     * fraction-digits argument.
     * 
     * @return
     */
    Integer getFractionDigits();
}
