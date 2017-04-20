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
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;

public abstract class AbstractRequestSuccessTest<T extends RequestSuccess> {

    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("test"), FrontendType.forName("one"));
    protected static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);
    protected static final LocalHistoryIdentifier HISTORY_IDENTIFIER = new LocalHistoryIdentifier(
            CLIENT_IDENTIFIER, 0);

    protected abstract T object();

    @SuppressWarnings("unchecked")
    @Test
    public void serializationTest() {
        final Object deserialize = SerializationUtils.clone(object());

        Assert.assertEquals(object().getTarget(), ((T) deserialize).getTarget());
        Assert.assertEquals(object().getVersion(), ((T) deserialize).getVersion());
        Assert.assertEquals(object().getSequence(), ((T) deserialize).getSequence());
        doAdditionalAssertions((T) deserialize);
    }

    protected abstract void doAdditionalAssertions(Object deserialize);
}
