/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.SliceableMessage;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Successful reply to an {@link ReadTransactionRequest}. It indicates presence of requested data via
 * {@link #getData()}.
 *
 * @author Robert Varga
 */
@Beta
@SuppressFBWarnings("SE_BAD_FIELD")
public final class ReadTransactionSuccess extends TransactionSuccess<ReadTransactionSuccess>
        implements SliceableMessage {
    private static final long serialVersionUID = 1L;
    private final Optional<NormalizedNode> data;

    public ReadTransactionSuccess(final TransactionIdentifier identifier, final long sequence,
            final Optional<NormalizedNode> data) {
        super(identifier, sequence);
        this.data = requireNonNull(data);
    }

    public Optional<NormalizedNode> getData() {
        return data;
    }

    @Override
    protected AbstractTransactionSuccessProxy<ReadTransactionSuccess> externalizableProxy(final ABIVersion version) {
        return new ReadTransactionSuccessProxyV1(this);
    }

    @Override
    protected ReadTransactionSuccess cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
