/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test.structures;

import java.util.HashSet;
import java.util.Set;

public class LfLst extends YangElement {
    Set<Lf> lfs;

    public LfLst(String name) {
        super(name);
        lfs = new HashSet<>();
    }

    public LfLst addLf(Object value) {
        return addLf(new Lf(value));
    }

    public LfLst addLf(Lf lf) {
        while (this.lfs.contains(lf)) {
            lf.incNumOfEqualItems();
        }
        this.lfs.add(lf);
        return this;
    }

    public Set<Lf> getLfs() {
        return lfs;
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
        LfLst lfLst = (LfLst) obj;
        if (this.lfs == null) {
            if (lfLst.lfs != null) {
                return false;
            }
        } else if (!this.lfs.equals(lfLst.lfs)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((lfs == null) ? 0 : lfs.hashCode());
        return result;
    }

    @Override
    public String toString() {

        return super.toString();
    }

}
