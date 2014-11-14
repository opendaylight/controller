/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api;

import static org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants.ERROR_INFO;
import static org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants.ERROR_MESSAGE;
import static org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants.ERROR_SEVERITY;
import static org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants.ERROR_TAG;
import static org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants.ERROR_TYPE;
import static org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants.RPC_ERROR;
import static org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants.RPC_REPLY_KEY;
import static org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Checked exception to communicate an error that needs to be sent to the
 * netconf client.
 */
public class NetconfDocumentedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final static Logger LOG = LoggerFactory.getLogger( NetconfDocumentedException.class );

    private static final DocumentBuilderFactory BUILDER_FACTORY;

    static {
        BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
        BUILDER_FACTORY.setNamespaceAware(true);
        BUILDER_FACTORY.setCoalescing(true);
        BUILDER_FACTORY.setIgnoringElementContentWhitespace(true);
        BUILDER_FACTORY.setIgnoringComments(true);
    }

    public enum ErrorType {
        transport, rpc, protocol, application;

        public String getTagValue() {
            return name();
        }

        public static ErrorType from( String text ) {
            try {
                return valueOf( text );
            }
            catch( Exception e ) {
                return application;
            }
        }
    }

    public enum ErrorTag {
        access_denied("access-denied"),
        bad_attribute("bad-attribute"),
        bad_element("bad-element"),
        data_exists("data-exists"),
        data_missing("data-missing"),
        in_use("in-use"),
        invalid_value("invalid-value"),
        lock_denied("lock-denied"),
        malformed_message("malformed-message"),
        missing_attribute("missing-attribute"),
        missing_element("missing-element"),
        operation_failed("operation-failed"),
        operation_not_supported("operation-not-supported"),
        resource_denied("resource-denied"),
        rollback_failed("rollback-failed"),
        too_big("too-big"),
        unknown_attribute("unknown-attribute"),
        unknown_element("unknown-element"),
        unknown_namespace("unknown-namespace");

        private final String tagValue;

        ErrorTag(final String tagValue) {
            this.tagValue = tagValue;
        }

        public String getTagValue() {
            return this.tagValue;
        }

        public static ErrorTag from( String text ) {
            for( ErrorTag e: values() )
            {
                if( e.getTagValue().equals( text ) ) {
                    return e;
                }
            }

            return operation_failed;
        }
    }

    public enum ErrorSeverity {
        error, warning;

        public String getTagValue() {
            return name();
        }

        public static ErrorSeverity from( String text ) {
            try {
                return valueOf( text );
            }
            catch( Exception e ) {
                return error;
            }
        }
    }

    private final ErrorType errorType;
    private final ErrorTag errorTag;
    private final ErrorSeverity errorSeverity;
    private final Map<String, String> errorInfo;

    public NetconfDocumentedException(String message) {
        this(message,
                NetconfDocumentedException.ErrorType.application,
                NetconfDocumentedException.ErrorTag.invalid_value,
                NetconfDocumentedException.ErrorSeverity.error
        );
    }

    public NetconfDocumentedException(final String message, final ErrorType errorType, final ErrorTag errorTag,
            final ErrorSeverity errorSeverity) {
        this(message, errorType, errorTag, errorSeverity, Collections.<String, String> emptyMap());
    }

    public NetconfDocumentedException(final String message, final ErrorType errorType, final ErrorTag errorTag,
            final ErrorSeverity errorSeverity, final Map<String, String> errorInfo) {
        super(message);
        this.errorType = errorType;
        this.errorTag = errorTag;
        this.errorSeverity = errorSeverity;
        this.errorInfo = errorInfo;
    }

    public NetconfDocumentedException(final String message, final Exception cause, final ErrorType errorType,
            final ErrorTag errorTag, final ErrorSeverity errorSeverity) {
        this(message, cause, errorType, errorTag, errorSeverity, Collections.<String, String> emptyMap());
    }

    public NetconfDocumentedException(final String message, final Exception cause, final ErrorType errorType,
            final ErrorTag errorTag, final ErrorSeverity errorSeverity, final Map<String, String> errorInfo) {
        super(message, cause);
        this.errorType = errorType;
        this.errorTag = errorTag;
        this.errorSeverity = errorSeverity;
        this.errorInfo = errorInfo;
    }

    public static <E extends Exception> NetconfDocumentedException wrap(E exception) throws NetconfDocumentedException {
        final Map<String, String> errorInfo = new HashMap<>();
        errorInfo.put(ErrorTag.operation_failed.name(), "Exception thrown");
        throw new NetconfDocumentedException(exception.getMessage(), exception, ErrorType.application, ErrorTag.operation_failed,
                ErrorSeverity.error, errorInfo);
    }
    public static NetconfDocumentedException wrap(ValidationException e) throws NetconfDocumentedException {
        final Map<String, String> errorInfo = new HashMap<>();
        errorInfo.put(ErrorTag.operation_failed.name(), "Validation failed");
        throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_failed,
                ErrorSeverity.error, errorInfo);
    }

    public static NetconfDocumentedException wrap(ConflictingVersionException e) throws NetconfDocumentedException {
        final Map<String, String> errorInfo = new HashMap<>();
        errorInfo.put(ErrorTag.operation_failed.name(), "Optimistic lock failed");
        throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_failed,
                ErrorSeverity.error, errorInfo);
    }

    public static NetconfDocumentedException fromXMLDocument( Document fromDoc ) {

        ErrorType errorType = ErrorType.application;
        ErrorTag errorTag = ErrorTag.operation_failed;
        ErrorSeverity errorSeverity = ErrorSeverity.error;
        Map<String, String> errorInfo = null;
        String errorMessage = "";

        Node rpcReply = fromDoc.getDocumentElement();

        // FIXME: BUG? - we only handle one rpc-error.

        NodeList replyChildren = rpcReply.getChildNodes();
        for( int i = 0; i < replyChildren.getLength(); i++ ) {
            Node replyChild = replyChildren.item( i );
            if( RPC_ERROR.equals( replyChild.getNodeName() ) )
            {
                NodeList rpcErrorChildren = replyChild.getChildNodes();
                for( int j = 0; j < rpcErrorChildren.getLength(); j++ )
                {
                    Node rpcErrorChild = rpcErrorChildren.item( j );
                    if( ERROR_TYPE.equals( rpcErrorChild.getNodeName() ) ) {
                        errorType = ErrorType.from( rpcErrorChild.getTextContent() );
                    }
                    else if( ERROR_TAG.equals( rpcErrorChild.getNodeName() ) ) {
                        errorTag = ErrorTag.from( rpcErrorChild.getTextContent() );
                    }
                    else if( ERROR_SEVERITY.equals( rpcErrorChild.getNodeName() ) ) {
                        errorSeverity = ErrorSeverity.from( rpcErrorChild.getTextContent() );
                    }
                    else if( ERROR_MESSAGE.equals( rpcErrorChild.getNodeName() ) ) {
                        errorMessage = rpcErrorChild.getTextContent();
                    }
                    else if( ERROR_INFO.equals( rpcErrorChild.getNodeName() ) ) {
                        errorInfo = parseErrorInfo( rpcErrorChild );
                    }
                }

                break;
            }
        }

        return new NetconfDocumentedException( errorMessage, errorType, errorTag, errorSeverity, errorInfo );
    }

    private static Map<String, String> parseErrorInfo( Node node ) {
        Map<String, String> infoMap = new HashMap<>();
        NodeList children = node.getChildNodes();
        for( int i = 0; i < children.getLength(); i++ ) {
            Node child = children.item( i );
            if( child.getNodeType() == Node.ELEMENT_NODE ) {
                infoMap.put( child.getNodeName(), child.getTextContent() );
            }
        }

        return infoMap;
    }

    public ErrorType getErrorType() {
        return this.errorType;
    }

    public ErrorTag getErrorTag() {
        return this.errorTag;
    }

    public ErrorSeverity getErrorSeverity() {
        return this.errorSeverity;
    }

    public Map<String, String> getErrorInfo() {
        return this.errorInfo;
    }

    public Document toXMLDocument() {
        Document doc = null;
        try {
            doc = BUILDER_FACTORY.newDocumentBuilder().newDocument();

            Node rpcReply = doc.createElementNS( URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, RPC_REPLY_KEY );
            doc.appendChild( rpcReply );

            Node rpcError = doc.createElementNS( URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, RPC_ERROR );
            rpcReply.appendChild( rpcError );

            rpcError.appendChild( createTextNode( doc, ERROR_TYPE, getErrorType().getTagValue() ) );
            rpcError.appendChild( createTextNode( doc, ERROR_TAG, getErrorTag().getTagValue() ) );
            rpcError.appendChild( createTextNode( doc, ERROR_SEVERITY, getErrorSeverity().getTagValue() ) );
            rpcError.appendChild( createTextNode( doc, ERROR_MESSAGE, getLocalizedMessage() ) );

            Map<String, String> errorInfoMap = getErrorInfo();
            if( errorInfoMap != null && !errorInfoMap.isEmpty() ) {
                /*
                 * <error-info>
                 *   <bad-attribute>message-id</bad-attribute>
                 *   <bad-element>rpc</bad-element>
                 * </error-info>
                 */

                Node errorInfoNode = doc.createElementNS( URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, ERROR_INFO );
                errorInfoNode.setPrefix( rpcReply.getPrefix() );
                rpcError.appendChild( errorInfoNode );

                for ( Entry<String, String> entry : errorInfoMap.entrySet() ) {
                    errorInfoNode.appendChild( createTextNode( doc, entry.getKey(), entry.getValue() ) );
                }
            }
        }
        catch( ParserConfigurationException e ) {
            LOG.error( "Error outputting to XML document", e ); // this shouldn't happen
        }

        return doc;
    }

    private Node createTextNode( Document doc, String tag, String textContent ) {
        Node node = doc.createElementNS( URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, tag );
        node.setTextContent( textContent );
        return node;
    }

    @Override
    public String toString() {
        return "NetconfDocumentedException{" + "message=" + getMessage() + ", errorType=" + this.errorType
                + ", errorTag=" + this.errorTag + ", errorSeverity=" + this.errorSeverity + ", errorInfo="
                + this.errorInfo + '}';
    }
}
