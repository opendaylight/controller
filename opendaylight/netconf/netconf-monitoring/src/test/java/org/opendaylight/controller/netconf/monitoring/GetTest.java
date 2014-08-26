/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.monitoring;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.Collections;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SchemasBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SessionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;
import org.w3c.dom.Document;

public class GetTest {

    @Mock
    private NetconfMonitoringService monitor;
    @Mock
    private Document request;
    @Mock
    private NetconfOperationChainedExecution subsequentOperation;
    private Document incorrectSubsequentResult;
    private Document correctSubsequentResult;

    private Get get;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        incorrectSubsequentResult = XmlUtil.readXmlToDocument("<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"/>");
        correctSubsequentResult = XmlUtil.readXmlToDocument("<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><data></data></rpc-reply>");

        doReturn(new SessionsBuilder().setSession(Collections.<Session>emptyList()).build()).when(monitor).getSessions();
        doReturn(new SchemasBuilder().setSchema(Collections.<Schema>emptyList()).build()).when(monitor).getSchemas();
        doReturn(false).when(subsequentOperation).isExecutionTermination();

        get = new Get(monitor);
    }

    @Test
    public void testHandleNoSubsequent() throws Exception {
        try {
            get.handle(null, NetconfOperationChainedExecution.EXECUTION_TERMINATION_POINT);
        } catch (final NetconfDocumentedException e) {
            assertNetconfDocumentedEx(e, NetconfDocumentedException.ErrorSeverity.error, NetconfDocumentedException.ErrorTag.operation_failed, NetconfDocumentedException.ErrorType.application);
            return;
        }

        fail("Get should fail without subsequent operation");
    }

    @Test
    public void testHandleWrongPlaceholder() throws Exception {
        doReturn(incorrectSubsequentResult).when(subsequentOperation).execute(request);
        try {
            get.handle(request, subsequentOperation);
        } catch (final NetconfDocumentedException e) {
            assertNetconfDocumentedEx(e, NetconfDocumentedException.ErrorSeverity.error, NetconfDocumentedException.ErrorTag.invalid_value, NetconfDocumentedException.ErrorType.application);
            return;
        }

        fail("Get should fail with wrong xml");
    }

    @Test
    public void testHandleRuntimeEx() throws Exception {
        doThrow(RuntimeException.class).when(subsequentOperation).execute(request);
        try {
            get.handle(request, subsequentOperation);
        } catch (final NetconfDocumentedException e) {
            assertNetconfDocumentedEx(e, NetconfDocumentedException.ErrorSeverity.error, NetconfDocumentedException.ErrorTag.operation_failed, NetconfDocumentedException.ErrorType.application);
            assertEquals(1, e.getErrorInfo().size());
            return;
        }

        fail("Get should fail with wrong xml");
    }

    @Test
    public void testSuccessHandle() throws Exception {
        doReturn(correctSubsequentResult).when(subsequentOperation).execute(request);
        assertTrue(get.getHandlingPriority().getPriority().get() > HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY.getPriority().get());
        final Document result = get.handle(request, subsequentOperation);
        assertThat(XmlUtil.toString(result), CoreMatchers.containsString("sessions"));
        assertThat(XmlUtil.toString(result), CoreMatchers.containsString("schemas"));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testHandle() throws Exception {
        get.handle(null, null, null);

    }

    private void assertNetconfDocumentedEx(final NetconfDocumentedException e, final NetconfDocumentedException.ErrorSeverity severity, final NetconfDocumentedException.ErrorTag errorTag, final NetconfDocumentedException.ErrorType type) {
        assertEquals(severity, e.getErrorSeverity());
        assertEquals(errorTag, e.getErrorTag());
        assertEquals(type, e.getErrorType());
    }
}
