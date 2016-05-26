/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * An abstract concept of a Message. This class is not instantiable directly, use its specializations {@link Request}
 * and {@link Response}.
 *
 * Messages have a target and a sequence number. Sequence numbers are expected to be assigned monotonically on a
 * per-target basis, hence two targets can observe the same sequence number.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
@Beta
public abstract class Message<T extends Identifier & WritableObject, C extends Message<T, C>> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final T target;
    private final long sequence;

    Message(final T identifier, final long sequence) {
        // Hidden to force use of subclasses
        this.target = Preconditions.checkNotNull(identifier);
        this.sequence = sequence;
    }

    /**
     * Get the target identifier for this message.
     *
     * @return Target identifier
     */
    public final T getTarget() {
        return target;
    }

    /**
     * Get the message sequence of this message.
     *
     * @return Message sequence
     */
    public final long getSequence() {
        return sequence;
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this).omitNullValues()).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("target", target).add("sequence", sequence);
    }

    abstract AbstractMessageProxy<T, C> externalizableProxy();

    protected final Object writeReplace() {
        return externalizableProxy();
    }
}
