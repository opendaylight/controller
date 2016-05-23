/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractTest;

public class AbortTransactionPayloadTest extends AbstractTest {
    @Test
    public void testPayloadSerDes() throws IOException {
        final AbortTransactionPayload template = AbortTransactionPayload.create(nextTransactionId());
        final AbortTransactionPayload cloned = SerializationUtils.clone(template);
        assertEquals(template.getIdentifier(), cloned.getIdentifier());
    }
}
