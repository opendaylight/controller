/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.test.opendaylight.mdsal337.rev180424.Key;
import org.opendaylight.yang.gen.v1.urn.test.opendaylight.mdsal337.rev180424.KeyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

class BindingContextTest {
    @Test
    void createForEntryObject() {
        final var ctx = BindingContext.create("unused", Key.class, "whatever");
        assertEquals(DataObjectIdentifier.builder(Key.class, new KeyKey("whatever")).build(), ctx.appConfigPath);
    }
}
