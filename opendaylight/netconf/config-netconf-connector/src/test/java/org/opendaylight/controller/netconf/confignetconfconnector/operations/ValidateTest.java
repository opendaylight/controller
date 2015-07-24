/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.w3c.dom.Element;

public class ValidateTest {

    public static final String NETCONF_SESSION_ID_FOR_REPORTING = "foo";

    @Test(expected = DocumentedException.class)
    public void test() throws Exception {
        final XmlElement xml = XmlElement.fromString("<abc></abc>");
        final Validate validate = new Validate(null, NETCONF_SESSION_ID_FOR_REPORTING);
        validate.handleWithNoSubsequentOperations(null, xml);
    }

    @Test(expected = DocumentedException.class)
    public void testNoSource() throws Exception {
        final XmlElement xml = XmlElement.fromString("<validate xmlns=\""
                + XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0 + "\"/>");
        final Validate validate = new Validate(null, NETCONF_SESSION_ID_FOR_REPORTING);
        validate.handleWithNoSubsequentOperations(null, xml);
    }

    @Test(expected = DocumentedException.class)
    public void testNoNamespace() throws Exception {
        final XmlElement xml = XmlElement.fromString("<validate/>");
        final Validate validate = new Validate(null, NETCONF_SESSION_ID_FOR_REPORTING);
        validate.handleWithNoSubsequentOperations(null, xml);
    }

    @Test(expected = DocumentedException.class)
    public void testRunningSource() throws Exception {

        final XmlElement xml = XmlElement.fromString("<validate xmlns=\""
                + XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
                + "\"><source><running></running></source></validate>");
        final Validate validate = new Validate(null, NETCONF_SESSION_ID_FOR_REPORTING);
        validate.handleWithNoSubsequentOperations(null, xml);
    }

    @Test(expected = DocumentedException.class)
    public void testNoTransaction() throws Exception {
        final XmlElement xml = XmlElement.fromString("<validate xmlns=\""
                + XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
                + "\"><source><candidate/></source></validate>");
        final ConfigSubsystemFacade facade = mock(ConfigSubsystemFacade.class);
        doThrow(IllegalStateException.class).when(facade).validateConfiguration();
        final Validate validate = new Validate(facade, NETCONF_SESSION_ID_FOR_REPORTING);
        validate.handleWithNoSubsequentOperations(null, xml);
    }

    @Test(expected = DocumentedException.class)
    public void testValidationException() throws Exception {
        final XmlElement xml = XmlElement.fromString("<validate xmlns=\""
                + XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
                + "\">><source><candidate/></source></validate>");
        final ConfigSubsystemFacade facade = mock(ConfigSubsystemFacade.class);
        doThrow(ValidationException.class).when(facade).validateConfiguration();
        final Validate validate = new Validate(facade, NETCONF_SESSION_ID_FOR_REPORTING);
        validate.handleWithNoSubsequentOperations(null, xml);
    }

    @Test
    public void testValidation() throws Exception {
        final XmlElement xml = XmlElement.fromString("<validate xmlns=\""
                + XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
                + "\"><source><candidate/></source></validate>");
        final Element okElement = XmlUtil.readXmlToElement("<ok/>");
        final ConfigSubsystemFacade facade = mock(ConfigSubsystemFacade.class);
        doNothing().when(facade).validateConfiguration();
        final Validate validate = new Validate(facade, NETCONF_SESSION_ID_FOR_REPORTING);
        Element ok = validate.handleWithNoSubsequentOperations(XmlUtil.newDocument(), xml);
        assertEquals(XmlUtil.toString(okElement), XmlUtil.toString(ok));
    }

}
