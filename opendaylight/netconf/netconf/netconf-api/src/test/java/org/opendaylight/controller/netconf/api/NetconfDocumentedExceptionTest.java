/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


/**
 * Unit tests for NetconfDocumentedException.
 *
 * @author Thomas Pantelis
 */
public class NetconfDocumentedExceptionTest {

    private XPath xpath;

    @Before
    public void setUp() throws Exception {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        xpath = xPathfactory.newXPath();
        xpath.setNamespaceContext( new NamespaceContext() {
            @Override
            public Iterator<?> getPrefixes( String namespaceURI ) {
                return Collections.singletonList( "netconf" ).iterator();
            }

            @Override
            public String getPrefix( String namespaceURI ) {
                return "netconf";
            }

            @Override
            public String getNamespaceURI( String prefix ) {
                return XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0;
            }
        } );
    }

    @Test
    public void testToAndFromXMLDocument() throws XPathExpressionException {
        String errorMessage = "mock error message";
        NetconfDocumentedException ex = new NetconfDocumentedException( errorMessage, null,
                                                                        ErrorType.protocol,
                                                                        ErrorTag.data_exists,
                                                                        ErrorSeverity.warning,
                                                                        ImmutableMap.of( "foo", "bar" ) );

        Document doc = ex.toXMLDocument();
        assertNotNull( "Document is null", doc );

        Node rootNode = doc.getDocumentElement();

        assertEquals( "getNamespaceURI", "urn:ietf:params:xml:ns:netconf:base:1.0", rootNode.getNamespaceURI() );
        assertEquals( "getLocalName", "rpc-reply", rootNode.getLocalName() );

        Node rpcErrorNode = getNode( "/netconf:rpc-reply/netconf:rpc-error", rootNode );
        assertNotNull( "rpc-error not found", rpcErrorNode );

        Node errorTypeNode = getNode( "netconf:error-type", rpcErrorNode );
        assertNotNull( "error-type not found", errorTypeNode );
        assertEquals( "error-type", ErrorType.protocol.getTagValue(),
                      errorTypeNode.getTextContent() );

        Node errorTagNode = getNode( "netconf:error-tag", rpcErrorNode );
        assertNotNull( "error-tag not found", errorTagNode );
        assertEquals( "error-tag", ErrorTag.data_exists.getTagValue(),
                      errorTagNode.getTextContent() );

        Node errorSeverityNode = getNode( "netconf:error-severity", rpcErrorNode );
        assertNotNull( "error-severity not found", errorSeverityNode );
        assertEquals( "error-severity", ErrorSeverity.warning.getTagValue(),
                      errorSeverityNode.getTextContent() );

        Node errorInfoNode = getNode( "netconf:error-info/netconf:foo", rpcErrorNode );
        assertNotNull( "foo not found", errorInfoNode );
        assertEquals( "foo", "bar", errorInfoNode.getTextContent() );

        Node errorMsgNode = getNode( "netconf:error-message", rpcErrorNode );
        assertNotNull( "error-message not found", errorMsgNode );
        assertEquals( "error-message", errorMessage, errorMsgNode.getTextContent() );

        // Test fromXMLDocument

        ex = NetconfDocumentedException.fromXMLDocument( doc );

        assertNotNull( "NetconfDocumentedException is null", ex );
        assertEquals( "getErrorSeverity", ErrorSeverity.warning, ex.getErrorSeverity() );
        assertEquals( "getErrorTag", ErrorTag.data_exists, ex.getErrorTag() );
        assertEquals( "getErrorType", ErrorType.protocol, ex.getErrorType() );
        assertEquals( "getLocalizedMessage", errorMessage, ex.getLocalizedMessage() );
        assertEquals( "getErrorInfo", ImmutableMap.of( "foo", "bar" ), ex.getErrorInfo() );
    }

    @SuppressWarnings("unchecked")
    <T> T getNode( String xpathExp, Node node ) throws XPathExpressionException {
        return (T)xpath.compile( xpathExp ).evaluate( node, XPathConstants.NODE );
    }
}

