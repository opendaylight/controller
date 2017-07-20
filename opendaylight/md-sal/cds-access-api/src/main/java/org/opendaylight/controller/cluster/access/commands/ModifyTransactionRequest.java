/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.SliceableMessage;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * A transaction request to apply a particular set of operations on top of the current transaction. This message is
 * used to also finish a transaction by specifying a {@link PersistenceProtocol}.
 *
 * @author Robert Varga
 */
@Beta
public final class ModifyTransactionRequest extends TransactionRequest<ModifyTransactionRequest>
        implements SliceableMessage {
    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final List<TransactionModification> modifications;
    private final PersistenceProtocol protocol;

    ModifyTransactionRequest(final TransactionIdentifier target, final long sequence, final ActorRef replyTo,
        final List<TransactionModification> modifications, final PersistenceProtocol protocol) {
        super(target, sequence, replyTo);
        this.modifications = ImmutableList.copyOf(modifications);
        this.protocol = protocol;
    }

    public Optional<PersistenceProtocol> getPersistenceProtocol() {
        return Optional.ofNullable(protocol);
    }

    public List<TransactionModification> getModifications() {
        return modifications;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("modifications", modifications.size())
                .add("protocol", protocol);
    }

    @Override
    protected ModifyTransactionRequestProxyV1 externalizableProxy(final ABIVersion version) {
        return new ModifyTransactionRequestProxyV1(this);
    }

    @Override
    protected ModifyTransactionRequest cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
