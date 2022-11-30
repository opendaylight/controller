/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class LocalHistoryFailureTest extends AbstractRequestFailureTest<LocalHistoryFailure> {
    private static final LocalHistoryFailure OBJECT = new LocalHistoryFailure(HISTORY_IDENTIFIER, 0, CAUSE);

    public LocalHistoryFailureTest() {
        super(OBJECT, 392);
    }

    @Test
    public void cloneAsVersionTest() {
        final LocalHistoryFailure clone = OBJECT.cloneAsVersion(ABIVersion.current());
        assertEquals(OBJECT, clone);
    }
}