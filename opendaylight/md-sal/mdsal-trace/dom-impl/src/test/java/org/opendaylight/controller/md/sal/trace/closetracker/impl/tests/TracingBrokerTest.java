/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.closetracker.impl.tests;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.trace.dom.impl.TracingBroker;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsaltrace.rev160908.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsaltrace.rev160908.ConfigBuilder;

/**
 * Test of {@link TracingBroker}.
 *
 * @author Michael Vorburger.ch
 */
public class TracingBrokerTest {

    @Test
    @SuppressWarnings({ "resource", "unused" }) // Finding resource leaks is the point of this test
    public void testPrintOpenTransactions() {
        DOMDataBroker domDataBroker = mock(DOMDataBroker.class, RETURNS_DEEP_STUBS);
        Config config = new ConfigBuilder().setTransactionDebugContextEnabled(true).build();
        BindingNormalizedNodeSerializer codec = mock(BindingNormalizedNodeSerializer.class);
        TracingBroker tracingBroker = new TracingBroker(domDataBroker, config, codec);

        DOMDataReadWriteTransaction tx = tracingBroker.newReadWriteTransaction();
        DOMTransactionChain txChain = tracingBroker.createTransactionChain(null);
        DOMDataReadWriteTransaction txFromChain = txChain.newReadWriteTransaction();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        boolean printReturnValue = tracingBroker.printOpenTransactions(ps);
        String output = new String(baos.toByteArray(), UTF_8);

        assertThat(printReturnValue).isTrue();
        assertThat(output).contains("testPrintOpenTransactions(TracingBrokerTest.java:41)"); // in a stack trace

        // We don't do any verify/times on the mocks,
        // because the main point of the test is just to verify that
        // printOpenTransactions runs through without any exceptions
        // (e.g. it used to have a ClassCastException).
    }

}
