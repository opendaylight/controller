/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.dispatch;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.apache.pekko.dispatch.Envelope;

/**
 * {@link Externalizable} proxy for {@link ControlAwareMessageDeque}.
 */
final class CAMDv1 implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private List<Envelope> control;
    private List<Envelope> normal;

    public CAMDv1() {
        // for Externalizable
    }

    CAMDv1(final Deque<Envelope> control, final Deque<Envelope> normal) {
        this.control = List.copyOf(control);
        this.normal = List.copyOf(normal);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        writeEnvelopes(out, control);
        writeEnvelopes(out, normal);
    }

    private static void writeEnvelopes(final ObjectOutput out, final List<Envelope> envelopes) throws IOException {
        out.writeInt(envelopes.size());
        for (var envelope : envelopes) {
            out.writeObject(envelope);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws ClassNotFoundException, IOException {
        control = readEnvelopes(in);
        normal = readEnvelopes(in);
    }

    private static List<Envelope> readEnvelopes(final ObjectInput in) throws ClassNotFoundException, IOException {
        final var size = in.readInt();
        final var envelopes = new ArrayList<Envelope>();
        for (int i = 0; i < size; ++i) {
            envelopes.add(Envelope.class.cast(requireNonNull(in.readObject())));
        }
        return envelopes;
    }


    @java.io.Serial
    private Object readResolve() {
        return new ControlAwareMessageDeque(control, normal);
    }
}
