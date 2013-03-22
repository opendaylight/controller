
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.test.internal;

import java.io.Serializable;

public class ComplexContainer implements Serializable {
    private IComplex f;
    private IComplex f1;
    private Integer state;

    public ComplexContainer(String i, Integer s) {
        this.state = s;
        this.f = new ComplexClass(i);
        this.f1 = new ComplexClass1(i);
    }

    public String getIdentity() {
        if (this.f != null && this.f1 != null) {
            return ("[" + f.whoAmI() + "]-[" + f1.whoAmI() + "]");
        }
        return "<NOTSET>";
    }

    public void setIdentity(String i) {
        if (this.f != null) {
            this.f.IAm(i);
        }
        if (this.f1 != null) {
            this.f1.IAm(i);
        }
    }

    public Integer getState() {
        return this.state;
    }

    @Override
    public String toString() {
        return ("{ID:" + this.getIdentity() + ",STATE:" + this.state + "}");
    }
}
