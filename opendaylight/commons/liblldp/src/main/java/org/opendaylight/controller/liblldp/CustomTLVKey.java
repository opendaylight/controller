/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.liblldp;

public class CustomTLVKey {

    private int oui;
    private byte subtype;

    /**
     * @param oui
     * @param subtype
     */
    public CustomTLVKey(int oui, byte subtype) {
        this.oui = oui;
        this.subtype = subtype;
    }

    /**
     * @return the oui
     */
    public int getOui() {
        return oui;
    }

    /**
     * @return the subtype
     */
    public byte getSubtype() {
        return subtype;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + oui;
        result = prime * result + subtype;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        CustomTLVKey other = (CustomTLVKey) obj;
        if (oui != other.oui) {
            return false;
        }

        if (subtype != other.subtype) {
            return false;
        }

        return true;
    }

}
