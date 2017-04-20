/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import java.io.Serializable;

/**
 * Serializable message that stores a String.
 *
 * @author Thomas Pantelis
 */
public class StringMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String string;

    public StringMessage(String string) {
        this.string = string;
    }

    @Override
    public int hashCode() {
        return string.hashCode();
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

        StringMessage other = (StringMessage) obj;
        return string.equals(other.string);
    }

    @Override
    public String toString() {
        return "StringMessage [string=" + string + "]";
    }
}
