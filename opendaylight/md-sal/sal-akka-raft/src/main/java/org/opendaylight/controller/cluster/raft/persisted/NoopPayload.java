/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import akka.dispatch.ControlMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.opendaylight.controller.cluster.raft.messages.Payload;

/**
 * Payload used for no-op log entries that are put into the journal by the PreLeader in order to commit
 * entries from the prior term.
 *
 * @author Thomas Pantelis
 */
public final class NoopPayload extends Payload implements ControlMessage {
    public static final NoopPayload INSTANCE = new NoopPayload();

    // There is no need for Externalizable
    private static final class Proxy implements Serializable {
        private static final long serialVersionUID = 1L;

        private Object readResolve() {
            return INSTANCE;
        }
    }

    private static final long serialVersionUID = 1L;
    private static final Proxy PROXY = new Proxy();
    private static final int PROXY_SIZE;

    static {
        final var bos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(bos)) {
            oos.writeObject(PROXY);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        PROXY_SIZE = bos.toByteArray().length;
    }

    private NoopPayload() {
        // Hidden on purpose
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int serializedSize() {
        return PROXY_SIZE;
    }

    @Override
    protected Object writeReplace() {
        return PROXY;
    }
}
