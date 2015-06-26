/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.opendaylight.controller.netconf.util.messages.FramingMechanism;

public class FramingMechanismHandlerFactoryTest {

    @Test
    public void testCreate() throws Exception {
        MatcherAssert.assertThat(FramingMechanismHandlerFactory
                .createHandler(FramingMechanism.CHUNK), CoreMatchers
                .instanceOf(ChunkedFramingMechanismEncoder.class));
        MatcherAssert.assertThat(FramingMechanismHandlerFactory
                .createHandler(FramingMechanism.EOM), CoreMatchers
                .instanceOf(EOMFramingMechanismEncoder.class));
    }
}