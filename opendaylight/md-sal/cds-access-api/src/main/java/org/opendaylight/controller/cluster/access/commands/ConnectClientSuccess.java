/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.DataInput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;

/**
 * Successful reply to an {@link ConnectClientRequest}. Client actor which initiated this connection should use
 * the version reported via {@link #getVersion()} of this message to communicate with this backend. Should this backend
 * fail, the client can try accessing the provided alternates.
 */
public final class ConnectClientSuccess extends RequestSuccess<ClientIdentifier, ConnectClientSuccess> {
    interface SerialForm extends RequestSuccess.SerialForm<ClientIdentifier, ConnectClientSuccess> {
        @Override
        default ClientIdentifier readTarget(final DataInput in) throws IOException {
            return ClientIdentifier.readFrom(in);
        }

        @Override
        default ConnectClientSuccess readExternal(final ObjectInput in, final ClientIdentifier target,
                final long sequence) throws IOException, ClassNotFoundException {
            final var backend = JavaSerializer.currentSystem().value().provider()
                .resolveActorRef((String) in.readObject());
            final var maxMessages = in.readInt();

            final int alternatesSize = in.readInt();
            final var alternates = new ArrayList<ActorSelection>(alternatesSize);
            for (int i = 0; i < alternatesSize; ++i) {
                alternates.add(ActorSelection.apply(ActorRef.noSender(), (String)in.readObject()));
            }

            return new ConnectClientSuccess(target, sequence, backend, alternates, maxMessages, null);
        }

        @Override
        default void writeExternal(final ObjectOutput out, final ConnectClientSuccess msg) throws IOException {
            out.writeObject(Serialization.serializedActorPath(msg.backend));
            out.writeInt(msg.maxMessages);

            out.writeInt(msg.alternates.size());
            for (ActorSelection b : msg.alternates) {
                out.writeObject(b.toSerializationFormat());
            }

            // We are ignoring the DataTree, it is not serializable anyway
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull ImmutableList<ActorSelection> alternates;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "See justification above.")
    private final ReadOnlyDataTree dataTree;
    private final @NonNull ActorRef backend;
    private final int maxMessages;

    ConnectClientSuccess(final ClientIdentifier target, final long sequence, final ActorRef backend,
        final List<ActorSelection> alternates, final int maxMessages, final ReadOnlyDataTree dataTree) {
        super(target, sequence);
        this.backend = requireNonNull(backend);
        this.alternates = ImmutableList.copyOf(alternates);
        this.dataTree = dataTree;
        checkArgument(maxMessages > 0, "Maximum messages has to be positive, not %s", maxMessages);
        this.maxMessages = maxMessages;
    }

    public ConnectClientSuccess(final @NonNull ClientIdentifier target, final long sequence,
            final @NonNull ActorRef backend, final @NonNull List<ActorSelection> alternates,
            final @NonNull ReadOnlyDataTree dataTree, final int maxMessages) {
        this(target, sequence, backend, alternates, maxMessages, requireNonNull(dataTree));
    }

    /**
     * Return the list of known alternate backends. The client can use this list to perform recovery procedures.
     *
     * @return a list of known backend alternates
     */
    public @NonNull List<ActorSelection> getAlternates() {
        return alternates;
    }

    public @NonNull ActorRef getBackend() {
        return backend;
    }

    public Optional<ReadOnlyDataTree> getDataTree() {
        return Optional.ofNullable(dataTree);
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lte(version) ? new ConnectClientSuccessProxyV1(this) : new CCS(this);
    }

    @Override
    protected ConnectClientSuccess cloneAsVersion(final ABIVersion version) {
        // FIXME: perform cloning
        return this;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("alternates", alternates)
                .add("dataTree present", getDataTree().isPresent()).add("maxMessages", maxMessages);
    }
}
