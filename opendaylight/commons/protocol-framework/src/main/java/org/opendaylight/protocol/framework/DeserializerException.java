/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

/**
 * Used when something occurs during parsing bytes to java objects.
 *
 * @deprecated This exception no longer carries any special meaning. Users
 * are advised to stop using it and define their own replacement.
 */
@Deprecated
public class DeserializerException extends Exception {

    private static final long serialVersionUID = -2247000673438452870L;

    /**
     * Creates a deserializer exception.
     * @param err string
     */
    public DeserializerException(final String err) {
        super(err);
    }

    /**
     * Creates a deserializer exception.
     * @param err string
     * @param e underlying exception
     */
    public DeserializerException(final String err, final Throwable e) {
        super(err, e);
    }
}
