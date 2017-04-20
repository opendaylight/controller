/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import org.opendaylight.yangtools.concepts.Identifier;

/**
 * A MessageSliceException indicating the message assembler has already been closed.
 *
 * @author Thomas Pantelis
 */
public class AssemblerClosedException extends MessageSliceException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     *
     * @param identifier the identifier whose state was closed
     */
    public AssemblerClosedException(final Identifier identifier) {
        super(String.format("Message assembler for %s has already been closed", identifier), false);
    }
}
