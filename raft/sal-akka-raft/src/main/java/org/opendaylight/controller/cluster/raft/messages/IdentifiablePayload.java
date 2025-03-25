/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;

public abstract class IdentifiablePayload<T extends Identifier> extends Payload implements Identifiable<T> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
}
