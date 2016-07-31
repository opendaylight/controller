/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * Successful reply to an {@link ConnectClientRequest}. Client actor which initiated this connection should use
 * the version reported via {@link #getVersion()} of this message to communicate with this backend. Should this backend
 * fail, the client can try accessing the provided alternates.
 *
 * @author Robert Varga
 */
@Beta
public final class ConnectClientSuccess extends RequestSuccess<ClientIdentifier, ConnectClientSuccess> {
    private static final long serialVersionUID = 1L;

    private final List<ActorSelection> alternates;
    private final DataTree dataTree;
    private final ActorRef backend;

    ConnectClientSuccess(final ClientIdentifier target, final ActorRef backend, final List<ActorSelection> alternates,
        final Optional<DataTree> dataTree) {
        super(target);
        this.backend = Preconditions.checkNotNull(backend);
        this.alternates = ImmutableList.copyOf(alternates);
        this.dataTree = dataTree.orElse(null);
    }

    public ConnectClientSuccess(final @Nonnull ClientIdentifier target, final @Nonnull ActorRef backend,
            final @Nonnull List<ActorSelection> alternates,
            final @Nonnull DataTree dataTree) {
        this(target, backend, alternates, Optional.of(dataTree));
    }

    /**
     * Return the list of known alternate backends. The client can use this list to perform recovery procedures.
     *
     * @return a list of known backend alternates
     */
    public @Nonnull List<ActorSelection> getAlternates() {
        return alternates;
    }

    public @Nonnull ActorRef getBackend() {
        return backend;
    }

    public Optional<DataTree> getDataTree() {
        return Optional.ofNullable(dataTree);
    }

    @Override
    protected ConnectClientSuccessProxyV1 externalizableProxy(final ABIVersion version) {
        return new ConnectClientSuccessProxyV1(this);
    }

    @Override
    protected ConnectClientSuccess cloneAsVersion(final ABIVersion version) {
        return this;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("alternates", alternates).add("dataTree", dataTree);
    }
}
