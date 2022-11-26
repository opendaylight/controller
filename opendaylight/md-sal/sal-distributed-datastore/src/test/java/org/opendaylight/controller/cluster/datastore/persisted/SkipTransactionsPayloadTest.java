/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import org.opendaylight.controller.cluster.datastore.utils.MutableUnsignedLongSet;

public class SkipTransactionsPayloadTest extends AbstractIdentifiablePayloadTest<SkipTransactionsPayload> {
    public SkipTransactionsPayloadTest() {
        super(SkipTransactionsPayload.create(newHistoryId(0), MutableUnsignedLongSet.of(42).immutableCopy(), 512), 131);
    }
}
