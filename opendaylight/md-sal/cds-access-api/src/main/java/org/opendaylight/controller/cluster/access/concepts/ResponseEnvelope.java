/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

public abstract class ResponseEnvelope<T extends Response<?, ?>> extends Envelope<T> {
    private static final long serialVersionUID = 1L;

    ResponseEnvelope(final T message, final long sessionId, final long txSequence) {
        super(message, sessionId, txSequence);
    }
}
