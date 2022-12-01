/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class LocalHistorySuccessTest extends AbstractRequestSuccessTest<LocalHistorySuccess> {
    private static final LocalHistorySuccess OBJECT = new LocalHistorySuccess(HISTORY_IDENTIFIER, 0);

    public LocalHistorySuccessTest() {
        super(OBJECT, 96);
    }

    @Test
    public void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.MAGNESIUM);
        assertEquals(ABIVersion.MAGNESIUM, clone.getVersion());
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
    }
}
