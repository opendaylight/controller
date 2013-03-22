
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.test.internal;

import java.io.Serializable;

public class ComplexClass1 implements IComplex, Serializable {
    private String identity;

    public ComplexClass1(String i) {
        this.identity = i;
    }

    @Override
    public String whoAmI() {
        return ("ComplexClass1_" + this.identity);
    }

    @Override
    public void IAm(String s) {
        this.identity = s;
    }
}
