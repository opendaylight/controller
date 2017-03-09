/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public abstract class AbstractRequestFailureTest<T extends RequestFailure> {
    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));

    protected static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);
    protected static final LocalHistoryIdentifier HISTORY_IDENTIFIER = new LocalHistoryIdentifier(CLIENT_IDENTIFIER, 0);
    protected static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(
            HISTORY_IDENTIFIER, 0);
    protected static final RequestException CAUSE = new RuntimeRequestException("fail", new Throwable());

    abstract T object();

    @Test
    public void getCauseTest() throws Exception {
        Assert.assertEquals(CAUSE, object().getCause());
    }

    @Test
    public void isHardFailureTest() throws Exception {
        Assert.assertTrue(object().isHardFailure());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serializationTest() {
        final Object deserialize = SerializationUtils.clone(object());

        Assert.assertEquals(object().getTarget(), ((T) deserialize).getTarget());
        Assert.assertEquals(object().getVersion(), ((T) deserialize).getVersion());
        Assert.assertEquals(object().getSequence(), ((T) deserialize).getSequence());
    }
}
