/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.test.types.rev131127;


/**
**/
public class UnionTestBuilder {

    public static UnionTest getDefaultInstance(String defaultValue) {
        try {
            int i = Integer.valueOf(defaultValue);
            return new UnionTest(new ExtendTwice(i));
        } catch (NumberFormatException e) {
            return new UnionTest(defaultValue);
        }
    }

}
