/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

/**
 * A MessageSliceException indicating the message assembler has already been sealed.
 *
 * @author Thomas Pantelis
 */
public class AssemblerSealedException extends MessageSliceException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     *
     * @param message he detail message
     */
    public AssemblerSealedException(String message) {
        super(message, false);
    }
}
