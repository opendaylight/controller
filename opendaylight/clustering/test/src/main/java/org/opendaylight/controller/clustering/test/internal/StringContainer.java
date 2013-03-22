
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.test.internal;

import java.io.Serializable;

public class StringContainer implements Serializable {
    private String mystring;

    public StringContainer() {
        this.mystring = null;
    }

    public StringContainer(String s) {
        setMystring(s);
    }

    public String getMystring() {
        return mystring;
    }

    public void setMystring(String mystring) {
        this.mystring = mystring;
    }

    // Return the hashCode of the containing string
    @Override
    public int hashCode() {
        if (this.mystring != null) {
            return this.mystring.hashCode();
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StringContainer) {
            StringContainer o = (StringContainer) obj;
            return this.mystring.equals(o.getMystring());
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" + this.mystring + "}";
    }
}
