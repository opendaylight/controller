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

/**
 * IntegerTypeDefinition is interface which represents SIGNED Integer values
 * defined in Yang language. <br>
 * The integer built-in types in Yang are int8, int16, int32, int64. They
 * represent signed integers of different sizes: <br>
 * <ul>
 * <li>int8 represents integer values between -128 and 127, inclusively.</li>
 * <li>int16 represents integer values between -32768 and 32767, inclusively.</li>
 * <li>int32 represents integer values between -2147483648 and 2147483647,
 * inclusively.</li>
 * <li>int64 represents integer values between -9223372036854775808 and
 * 9223372036854775807, inclusively.</li>
 * </ul>
 * 
 * The Integer Built-In Types are defined in <a
 * href="https://tools.ietf.org/html/rfc6020#section-9.2"> [RFC-6020]</a>
 */
public interface IntegerTypeDefinition extends
        TypeDefinition<IntegerTypeDefinition> {

    /**
     * Returns Range Constraints defined for given Integer Type.
     * 
     * @return Range Constraints defined for given Integer Type.
     */
    List<RangeConstraint> getRangeStatements();
}
