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
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest.FinishTransaction;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;
import org.opendaylight.yangtools.concepts.Builder;

@Beta
@NotThreadSafe
public final class ModifyTransactionRequestBuilder implements Builder<ModifyTransactionRequest> {
    private final List<TransactionModification> modifications = new ArrayList<>(1);
    private final GlobalTransactionIdentifier id;
    private final ActorRef frontendRef;
    private FinishTransaction finish = FinishTransaction.NONE;
    private long requestId;

    public ModifyTransactionRequestBuilder(final GlobalTransactionIdentifier id, final ActorRef frontendRef) {
        this.id = Preconditions.checkNotNull(id);
        this.frontendRef = Preconditions.checkNotNull(frontendRef);
    }

    private void checkFinished() {
        Preconditions.checkState(finish != FinishTransaction.NONE, "Batch has already been finished");
    }

    public void setRequestId(final long requestId) {
        checkFinished();
        Preconditions.checkState(modifications.isEmpty(), "Request ID must be set first");
        this.requestId = requestId;
    }

    public void addModification(final TransactionModification operation) {
        checkFinished();
        modifications.add(Preconditions.checkNotNull(operation));
    }

    public void setAbort() {
        checkFinished();
        // Transaction is being aborted, no need to transmit operations
        modifications.clear();
        finish = FinishTransaction.ABORT;
    }

    public void setCommit(final boolean coordinated) {
        checkFinished();
        finish = coordinated ? FinishTransaction.COORDINATED_COMMIT : FinishTransaction.SIMPLE_COMMIT;
    }

    public void reset() {
    }

    public int size() {
        return modifications.size();
    }

    @Override
    public ModifyTransactionRequest build() {
        final ModifyTransactionRequest ret = new ModifyTransactionRequest(
            new TransactionRequestIdentifier(id, requestId), frontendRef, modifications, finish);
        modifications.clear();
        finish = FinishTransaction.NONE;
        requestId = 0;
        return ret;
    }
}
