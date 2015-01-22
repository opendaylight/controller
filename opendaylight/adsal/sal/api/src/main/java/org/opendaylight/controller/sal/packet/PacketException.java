/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.packet;

/**
 * Describes an exception that is raised when the process of serializing or
 * deserializing a network packet/stream fails. This generally happens when the
 * packet/stream is malformed.
 *
 */
@Deprecated
public class PacketException extends Exception {
    private static final long serialVersionUID = 1L;

    public PacketException(String message) {
        super(message);
    }
}
