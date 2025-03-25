/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.Serializable;

/**
 * An instance of a {@link Payload} class is meant to be used as the Payload for {@link AppendEntries}.
 *
 * <p>When an actor which is derived from RaftActor attempts to persistData it must pass an instance of the Payload
 * class. Similarly when state needs to be applied to the derived RaftActor it will be passed an instance of the
 * Payload class.
 */
public abstract class Payload implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /**
     * Return the estimate of in-memory size of this payload.
     *
     * @return An estimate of the in-memory size of this payload.
     */
    public abstract int size();

    /**
     * Return the estimate of serialized size of this payload when passed through serialization. The estimate needs to
     * be reasonably accurate and should err on the side of caution and report a slightly-higher size in face of
     * uncertainty.
     *
     * @return An estimate of serialized size.
     */
    public abstract int serializedSize();

    /**
     * Return the serialization proxy for this object.
     *
     * @return Serialization proxy
     */
    @java.io.Serial
    protected abstract Object writeReplace();
}
