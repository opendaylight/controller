/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test.structures;

public class Lf extends YangElement {
    private Object value;
    private int numOfEqualItems = 0;

    public Lf(String name, Object value) {
        super(name);
        this.value = value;
    }

    public Lf(Object value) {
        super("");
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        Lf lf = (Lf) obj;
        if (this.value == null) {
            if (lf.value != null) {
                return false;
            }
        } else if (!this.value.equals(lf.value)) {
            return false;
        }
        if (this.numOfEqualItems != lf.numOfEqualItems) {
            return false;
        }
        return true;
    }

    public void incNumOfEqualItems() {
        this.numOfEqualItems++;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + numOfEqualItems;
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + value;
    }

}
