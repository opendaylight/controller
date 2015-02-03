/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.packet;

/**
 * Describes an exception that is raised during BitBufferHelper operations.
 */
@Deprecated
public class BufferException extends Exception {
    private static final long serialVersionUID = 1L;

    public BufferException(String message) {
        super(message);
    }
}
