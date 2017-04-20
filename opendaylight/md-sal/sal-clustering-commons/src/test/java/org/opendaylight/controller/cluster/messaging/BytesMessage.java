/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Serializable message that stores a byte[].
 *
 * @author Thomas Pantelis
 */
public class BytesMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] bytes;

    public BytesMessage(byte[] bytes) {
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
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
        BytesMessage other = (BytesMessage) obj;
        return Arrays.equals(bytes, other.bytes);
    }

    @Override
    public String toString() {
        return "BytesMessage [bytes=" + Arrays.toString(bytes) + "]";
    }
}
