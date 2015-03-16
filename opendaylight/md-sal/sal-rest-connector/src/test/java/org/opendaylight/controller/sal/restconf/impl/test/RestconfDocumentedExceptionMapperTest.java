/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.sal.rest.impl.RestconfDocumentedExceptionMapper;
import org.opendaylight.controller.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Unit tests for RestconfDocumentedExceptionMapper.
 *
 * @author Thomas Pantelis
 */
public class RestconfDocumentedExceptionMapperTest extends JerseyTest {

    interface ErrorInfoVerifier {
        void verifyXML(Node errorInfoNode);

        void verifyJson(JsonElement errorInfoElement);
    }

    static class ComplexErrorInfoVerifier implements ErrorInfoVerifier {

        Map<String, String> expErrorInfo;

        public ComplexErrorInfoVerifier(final Map<String, String> expErrorInfo) {
            this.expErrorInfo = expErrorInfo;
        }

        @Override
        public void verifyXML(final Node errorInfoNode) {

            final Map<String, String> mutableExpMap = Maps.newHashMap(expErrorInfo);
            final NodeList childNodes = errorInfoNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node child = childNodes.item(i);
                if (child instanceof Element) {
                    final String expValue = mutableExpMap.remove(child.getNodeName());
                    assertNotNull("Found unexpected \"error-info\" child node: " + child.getNodeName(), expValue);
                    assertEquals("Text content for \"error-info\" child node " + child.getNodeName(), expValue,
                            child.getTextContent());
                }
            }

            if (!mutableExpMap.isEmpty()) {
                fail("Missing \"error-info\" child nodes: " + mutableExpMap);
            }
        }

        @Override
        public void verifyJson(final JsonElement errorInfoElement) {

            assertTrue("\"error-info\" Json element is not an Object", errorInfoElement.isJsonObject());

            final Map<String, String> actualErrorInfo = Maps.newHashMap();
            for (final Entry<String, JsonElement> entry : errorInfoElement.getAsJsonObject().entrySet()) {
                final String leafName = entry.getKey();
                final JsonElement leafElement = entry.getValue();
                actualErrorInfo.put(leafName, leafElement.getAsString());
            }

            final Map<String, String> mutableExpMap = Maps.newHashMap(expErrorInfo);
            for (final Entry<String, String> actual : actualErrorInfo.entrySet()) {
                final String expValue = mutableExpMap.remove(actual.getKey());
                assertNotNull("Found unexpected \"error-info\" child node: " + actual.getKey(), expValue);
                assertEquals("Text content for \"error-info\" child node " + actual.getKey(), expValue,
                        actual.getValue());
            }

            if (!mutableExpMap.isEmpty()) {
                fail("Missing \"error-info\" child nodes: " + mutableExpMap);
            }
        }
    }

    static class SimpleErrorInfoVerifier implements ErrorInfoVerifier {

        String expTextContent;

        public SimpleErrorInfoVerifier(final String expErrorInfo) {
            expTextContent = expErrorInfo;
        }

        void verifyContent(final String actualContent) {
            assertNotNull("Actual \"error-info\" text content is null", actualContent);
            assertTrue("", actualContent.contains(expTextContent));
        }

        @Override
        public void verifyXML(final Node errorInfoNode) {
            verifyContent(errorInfoNode.getTextContent());
        }

        @Override
        public void verifyJson(final JsonElement errorInfoElement) {
            verifyContent(errorInfoElement.getAsString());
        }
    }

    static RestconfService mockRestConf = mock(RestconfService.class);

    static XPath XPATH = XPathFactory.newInstance().newXPath();
    static XPathExpression ERROR_LIST;
    static XPathExpression ERROR_TYPE;
    static XPathExpression ERROR_TAG;
    static XPathExpression ERROR_MESSAGE;
    static XPathExpression ERROR_APP_TAG;
    static XPathExpression ERROR_INFO;

    @BeforeClass
    public static void init() throws Exception {
        ControllerContext.getInstance().setGlobalSchema(TestUtils.loadSchemaContext("/modules"));

        final NamespaceContext nsContext = new NamespaceContext() {
            @Override
            public Iterator<?> getPrefixes(final String namespaceURI) {
                return null;
            }

            @Override
            public String getPrefix(final String namespaceURI) {
                return null;
            }

            @Override
            public String getNamespaceURI(final String prefix) {
                return "ietf-restconf".equals(prefix) ? Draft02.RestConfModule.NAMESPACE : null;
            }
        };

        XPATH.setNamespaceContext(nsContext);
        ERROR_LIST = XPATH.compile("ietf-restconf:errors/ietf-restconf:error");
        ERROR_TYPE = XPATH.compile("ietf-restconf:error-type");
        ERROR_TAG = XPATH.compile("ietf-restconf:error-tag");
        ERROR_MESSAGE = XPATH.compile("ietf-restconf:error-message");
        ERROR_APP_TAG = XPATH.compile("ietf-restconf:error-app-tag");
        ERROR_INFO = XPATH.compile("ietf-restconf:error-info");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        reset(mockRestConf);
        super.setUp();
    }

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(mockRestConf, new XmlNormalizedNodeBodyReader(),
                new JsonNormalizedNodeBodyReader(), new NormalizedNodeJsonBodyWriter(), new NormalizedNodeXmlBodyWriter());
        resourceConfig.registerClasses(RestconfDocumentedExceptionMapper.class);
        return resourceConfig;
    }

    void stageMockEx(final RestconfDocumentedException ex) {
        reset(mockRestConf);
        when(mockRestConf.readOperationalData(any(String.class), any(UriInfo.class))).thenThrow(ex);
    }

    void testJsonResponse(final RestconfDocumentedException ex, final Status expStatus, final ErrorType expErrorType,
            final ErrorTag expErrorTag, final String expErrorMessage, final String expErrorAppTag,
            final ErrorInfoVerifier errorInfoVerifier) throws Exception {

        stageMockEx(ex);

        final Response resp = target("/operational/foo").request(MediaType.APPLICATION_JSON).get();

        final InputStream stream = verifyResponse(resp, MediaType.APPLICATION_JSON, expStatus);

        verifyJsonResponseBody(stream, expErrorType, expErrorTag, expErrorMessage, expErrorAppTag, errorInfoVerifier);
    }

    @Test
    public void testToJsonResponseWithMessageOnly() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error"), Status.INTERNAL_SERVER_ERROR,
                ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED, "mock error", null, null);

        // To test verification code
        // String json =
        // "{ errors: {" +
        // "    error: [{" +
        // "      error-tag : \"operation-failed\"" +
        // "      ,error-type : \"application\"" +
        // "      ,error-message : \"An error occurred\"" +
        // "      ,error-info : {" +
        // "        session-id: \"123\"" +
        // "        ,address: \"1.2.3.4\"" +
        // "      }" +
        // "    }]" +
        // "  }" +
        // "}";
        //
        // verifyJsonResponseBody( new java.io.StringBufferInputStream(json ),
        // ErrorType.APPLICATION,
        // ErrorTag.OPERATION_FAILED, "An error occurred", null,
        // com.google.common.collect.ImmutableMap.of( "session-id", "123",
        // "address", "1.2.3.4" ) );
    }

    @Test
    public void testToJsonResponseWithInUseErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.IN_USE),
                Status.CONFLICT, ErrorType.PROTOCOL, ErrorTag.IN_USE, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithInvalidValueErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.RPC, ErrorTag.INVALID_VALUE),
                Status.BAD_REQUEST, ErrorType.RPC, ErrorTag.INVALID_VALUE, "mock error", null, null);

    }

    @Test
    public void testToJsonResponseWithTooBigErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.TRANSPORT, ErrorTag.TOO_BIG),
                Status.REQUEST_ENTITY_TOO_LARGE, ErrorType.TRANSPORT, ErrorTag.TOO_BIG, "mock error", null, null);

    }

    @Test
    public void testToJsonResponseWithMissingAttributeErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.MISSING_ATTRIBUTE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.MISSING_ATTRIBUTE, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithBadAttributeErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.BAD_ATTRIBUTE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.BAD_ATTRIBUTE, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithUnknownAttributeErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ATTRIBUTE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ATTRIBUTE, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithBadElementErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.BAD_ELEMENT),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.BAD_ELEMENT, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithUnknownElementErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithUnknownNamespaceErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithMalformedMessageErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithAccessDeniedErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.ACCESS_DENIED),
                Status.FORBIDDEN, ErrorType.PROTOCOL, ErrorTag.ACCESS_DENIED, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithLockDeniedErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.LOCK_DENIED),
                Status.CONFLICT, ErrorType.PROTOCOL, ErrorTag.LOCK_DENIED, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithResourceDeniedErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.RESOURCE_DENIED),
                Status.CONFLICT, ErrorType.PROTOCOL, ErrorTag.RESOURCE_DENIED, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithRollbackFailedErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.ROLLBACK_FAILED),
                Status.INTERNAL_SERVER_ERROR, ErrorType.PROTOCOL, ErrorTag.ROLLBACK_FAILED, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithDataExistsErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.DATA_EXISTS),
                Status.CONFLICT, ErrorType.PROTOCOL, ErrorTag.DATA_EXISTS, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithDataMissingErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.DATA_MISSING),
                Status.NOT_FOUND, ErrorType.PROTOCOL, ErrorTag.DATA_MISSING, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithOperationNotSupportedErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL,
                ErrorTag.OPERATION_NOT_SUPPORTED), Status.NOT_IMPLEMENTED, ErrorType.PROTOCOL,
                ErrorTag.OPERATION_NOT_SUPPORTED, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithOperationFailedErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.OPERATION_FAILED),
                Status.INTERNAL_SERVER_ERROR, ErrorType.PROTOCOL, ErrorTag.OPERATION_FAILED, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithPartialOperationErrorTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.PARTIAL_OPERATION),
                Status.INTERNAL_SERVER_ERROR, ErrorType.PROTOCOL, ErrorTag.PARTIAL_OPERATION, "mock error", null, null);
    }

    @Test
    public void testToJsonResponseWithErrorAppTag() throws Exception {

        testJsonResponse(new RestconfDocumentedException(new RestconfError(ErrorType.APPLICATION,
                ErrorTag.INVALID_VALUE, "mock error", "mock-app-tag")), Status.BAD_REQUEST, ErrorType.APPLICATION,
                ErrorTag.INVALID_VALUE, "mock error", "mock-app-tag", null);
    }

    @Test
    @Ignore // FIXME : find why it return "error-type" RPC no expected APPLICATION
    public void testToJsonResponseWithMultipleErrors() throws Exception {

        final List<RestconfError> errorList = Arrays.asList(new RestconfError(ErrorType.APPLICATION, ErrorTag.LOCK_DENIED,
                "mock error1"), new RestconfError(ErrorType.RPC, ErrorTag.ROLLBACK_FAILED, "mock error2"));
        stageMockEx(new RestconfDocumentedException("mock", null, errorList));

        final Response resp = target("/operational/foo").request(MediaType.APPLICATION_JSON).get();

        final InputStream stream = verifyResponse(resp, MediaType.APPLICATION_JSON, Status.CONFLICT);

        final JsonArray arrayElement = parseJsonErrorArrayElement(stream);

        assertEquals("\"error\" Json array element length", 2, arrayElement.size());

        verifyJsonErrorNode(arrayElement.get(0), ErrorType.APPLICATION, ErrorTag.LOCK_DENIED, "mock error1", null, null);

        verifyJsonErrorNode(arrayElement.get(1), ErrorType.RPC, ErrorTag.ROLLBACK_FAILED, "mock error2", null, null);
    }

    @Test
    @Ignore // TODO : we are not supported "error-info" element yet
    public void testToJsonResponseWithErrorInfo() throws Exception {

        final String errorInfo = "<address>1.2.3.4</address> <session-id>123</session-id>";
        testJsonResponse(new RestconfDocumentedException(new RestconfError(ErrorType.APPLICATION,
                ErrorTag.INVALID_VALUE, "mock error", "mock-app-tag", errorInfo)), Status.BAD_REQUEST,
                ErrorType.APPLICATION, ErrorTag.INVALID_VALUE, "mock error", "mock-app-tag",
                new ComplexErrorInfoVerifier(ImmutableMap.of("session-id", "123", "address", "1.2.3.4")));
    }

    @Test
    @Ignore //TODO : we are not supporting "error-info" yet
    public void testToJsonResponseWithExceptionCause() throws Exception {

        final Exception cause = new Exception("mock exception cause");
        testJsonResponse(new RestconfDocumentedException("mock error", cause), Status.INTERNAL_SERVER_ERROR,
                ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED, "mock error", null,
                new SimpleErrorInfoVerifier(cause.getMessage()));
    }

    void testXMLResponse(final RestconfDocumentedException ex, final Status expStatus, final ErrorType expErrorType,
            final ErrorTag expErrorTag, final String expErrorMessage, final String expErrorAppTag,
            final ErrorInfoVerifier errorInfoVerifier) throws Exception {
        stageMockEx(ex);

        final Response resp = target("/operational/foo").request(MediaType.APPLICATION_XML).get();

        final InputStream stream = verifyResponse(resp, MediaType.APPLICATION_XML, expStatus);

        verifyXMLResponseBody(stream, expErrorType, expErrorTag, expErrorMessage, expErrorAppTag, errorInfoVerifier);
    }

    @Test
    public void testToXMLResponseWithMessageOnly() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error"), Status.INTERNAL_SERVER_ERROR,
                ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED, "mock error", null, null);

        // To test verification code
        // String xml =
        // "<errors xmlns=\"urn:ietf:params:xml:ns:yang:ietf-restconf\">"+
        // "  <error>" +
        // "    <error-type>application</error-type>"+
        // "    <error-tag>operation-failed</error-tag>"+
        // "    <error-message>An error occurred</error-message>"+
        // "    <error-info>" +
        // "      <session-id>123</session-id>" +
        // "      <address>1.2.3.4</address>" +
        // "    </error-info>" +
        // "  </error>" +
        // "</errors>";
        //
        // verifyXMLResponseBody( new java.io.StringBufferInputStream(xml),
        // ErrorType.APPLICATION,
        // ErrorTag.OPERATION_FAILED, "An error occurred", null,
        // com.google.common.collect.ImmutableMap.of( "session-id", "123",
        // "address", "1.2.3.4" ) );
    }

    @Test
    public void testToXMLResponseWithInUseErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.IN_USE),
                Status.CONFLICT, ErrorType.PROTOCOL, ErrorTag.IN_USE, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithInvalidValueErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.RPC, ErrorTag.INVALID_VALUE),
                Status.BAD_REQUEST, ErrorType.RPC, ErrorTag.INVALID_VALUE, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithTooBigErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.TRANSPORT, ErrorTag.TOO_BIG),
                Status.REQUEST_ENTITY_TOO_LARGE, ErrorType.TRANSPORT, ErrorTag.TOO_BIG, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithMissingAttributeErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.MISSING_ATTRIBUTE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.MISSING_ATTRIBUTE, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithBadAttributeErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.BAD_ATTRIBUTE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.BAD_ATTRIBUTE, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithUnknownAttributeErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ATTRIBUTE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ATTRIBUTE, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithBadElementErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.BAD_ELEMENT),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.BAD_ELEMENT, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithUnknownElementErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithUnknownNamespaceErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithMalformedMessageErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE),
                Status.BAD_REQUEST, ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithAccessDeniedErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.ACCESS_DENIED),
                Status.FORBIDDEN, ErrorType.PROTOCOL, ErrorTag.ACCESS_DENIED, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithLockDeniedErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.LOCK_DENIED),
                Status.CONFLICT, ErrorType.PROTOCOL, ErrorTag.LOCK_DENIED, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithResourceDeniedErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.RESOURCE_DENIED),
                Status.CONFLICT, ErrorType.PROTOCOL, ErrorTag.RESOURCE_DENIED, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithRollbackFailedErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.ROLLBACK_FAILED),
                Status.INTERNAL_SERVER_ERROR, ErrorType.PROTOCOL, ErrorTag.ROLLBACK_FAILED, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithDataExistsErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.DATA_EXISTS),
                Status.CONFLICT, ErrorType.PROTOCOL, ErrorTag.DATA_EXISTS, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithDataMissingErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.DATA_MISSING),
                Status.NOT_FOUND, ErrorType.PROTOCOL, ErrorTag.DATA_MISSING, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithOperationNotSupportedErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL,
                ErrorTag.OPERATION_NOT_SUPPORTED), Status.NOT_IMPLEMENTED, ErrorType.PROTOCOL,
                ErrorTag.OPERATION_NOT_SUPPORTED, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithOperationFailedErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.OPERATION_FAILED),
                Status.INTERNAL_SERVER_ERROR, ErrorType.PROTOCOL, ErrorTag.OPERATION_FAILED, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithPartialOperationErrorTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException("mock error", ErrorType.PROTOCOL, ErrorTag.PARTIAL_OPERATION),
                Status.INTERNAL_SERVER_ERROR, ErrorType.PROTOCOL, ErrorTag.PARTIAL_OPERATION, "mock error", null, null);
    }

    @Test
    public void testToXMLResponseWithErrorAppTag() throws Exception {

        testXMLResponse(new RestconfDocumentedException(new RestconfError(ErrorType.APPLICATION,
                ErrorTag.INVALID_VALUE, "mock error", "mock-app-tag")), Status.BAD_REQUEST, ErrorType.APPLICATION,
                ErrorTag.INVALID_VALUE, "mock error", "mock-app-tag", null);
    }

    @Test
    @Ignore // TODO : we are not supporting "error-info" node yet
    public void testToXMLResponseWithErrorInfo() throws Exception {

        final String errorInfo = "<address>1.2.3.4</address> <session-id>123</session-id>";
        testXMLResponse(new RestconfDocumentedException(new RestconfError(ErrorType.APPLICATION,
                ErrorTag.INVALID_VALUE, "mock error", "mock-app-tag", errorInfo)), Status.BAD_REQUEST,
                ErrorType.APPLICATION, ErrorTag.INVALID_VALUE, "mock error", "mock-app-tag",
                new ComplexErrorInfoVerifier(ImmutableMap.of("session-id", "123", "address", "1.2.3.4")));
    }

    @Test
    @Ignore // TODO : we are not supporting "error-info" node yet
    public void testToXMLResponseWithExceptionCause() throws Exception {

        final Exception cause = new Exception("mock exception cause");
        testXMLResponse(new RestconfDocumentedException("mock error", cause), Status.INTERNAL_SERVER_ERROR,
                ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED, "mock error", null,
                new SimpleErrorInfoVerifier(cause.getMessage()));
    }

    @Test
    @Ignore // FIXME : find why it return error-type as RPC no APPLICATION
    public void testToXMLResponseWithMultipleErrors() throws Exception {

        final List<RestconfError> errorList = Arrays.asList(new RestconfError(ErrorType.APPLICATION, ErrorTag.LOCK_DENIED,
                "mock error1"), new RestconfError(ErrorType.RPC, ErrorTag.ROLLBACK_FAILED, "mock error2"));
        stageMockEx(new RestconfDocumentedException("mock", null, errorList));

        final Response resp = target("/operational/foo").request(MediaType.APPLICATION_XML).get();

        final InputStream stream = verifyResponse(resp, MediaType.APPLICATION_XML, Status.CONFLICT);

        final Document doc = parseXMLDocument(stream);

        final NodeList children = getXMLErrorList(doc, 2);

        verifyXMLErrorNode(children.item(0), ErrorType.APPLICATION, ErrorTag.LOCK_DENIED, "mock error1", null, null);

        verifyXMLErrorNode(children.item(1), ErrorType.RPC, ErrorTag.ROLLBACK_FAILED, "mock error2", null, null);
    }

    @Test
    public void testToResponseWithAcceptHeader() throws Exception {

        stageMockEx(new RestconfDocumentedException("mock error"));

        final Response resp = target("/operational/foo").request().header("Accept", MediaType.APPLICATION_JSON).get();

        final InputStream stream = verifyResponse(resp, MediaType.APPLICATION_JSON, Status.INTERNAL_SERVER_ERROR);

        verifyJsonResponseBody(stream, ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED, "mock error", null, null);
    }

    @Test
    @Ignore
    public void testToResponseWithStatusOnly() throws Exception {

        // The StructuredDataToJsonProvider should throw a
        // RestconfDocumentedException with no data

        when(mockRestConf.readOperationalData(any(String.class), any(UriInfo.class))).thenReturn(
                new NormalizedNodeContext(null, null));

        final Response resp = target("/operational/foo").request(MediaType.APPLICATION_JSON).get();

        verifyResponse(resp, MediaType.TEXT_PLAIN, Status.NOT_FOUND);
    }

    InputStream verifyResponse(final Response resp, final String expMediaType, final Status expStatus) {
        assertEquals("getMediaType", MediaType.valueOf(expMediaType), resp.getMediaType());
        assertEquals("getStatus", expStatus.getStatusCode(), resp.getStatus());

        final Object entity = resp.getEntity();
        assertEquals("Response entity", true, entity instanceof InputStream);
        final InputStream stream = (InputStream) entity;
        return stream;
    }

    void verifyJsonResponseBody(final InputStream stream, final ErrorType expErrorType, final ErrorTag expErrorTag,
            final String expErrorMessage, final String expErrorAppTag, final ErrorInfoVerifier errorInfoVerifier)
            throws Exception {

        final JsonArray arrayElement = parseJsonErrorArrayElement(stream);

        assertEquals("\"error\" Json array element length", 1, arrayElement.size());

        verifyJsonErrorNode(arrayElement.get(0), expErrorType, expErrorTag, expErrorMessage, expErrorAppTag,
                errorInfoVerifier);
    }

    private JsonArray parseJsonErrorArrayElement(final InputStream stream) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteStreams.copy(stream, bos);

        System.out.println("JSON: " + bos.toString());

        final JsonParser parser = new JsonParser();
        JsonElement rootElement;

        try {
            rootElement = parser.parse(new InputStreamReader(new ByteArrayInputStream(bos.toByteArray())));
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid JSON response:\n" + bos.toString(), e);
        }

        assertTrue("Root element of Json is not an Object", rootElement.isJsonObject());

        final Set<Entry<String, JsonElement>> errorsEntrySet = rootElement.getAsJsonObject().entrySet();
        assertEquals("Json Object element set count", 1, errorsEntrySet.size());

        final Entry<String, JsonElement> errorsEntry = errorsEntrySet.iterator().next();
        final JsonElement errorsElement = errorsEntry.getValue();
        assertEquals("First Json element name", "errors", errorsEntry.getKey());
        assertTrue("\"errors\" Json element is not an Object", errorsElement.isJsonObject());

        final Set<Entry<String, JsonElement>> errorListEntrySet = errorsElement.getAsJsonObject().entrySet();
        assertEquals("Root \"errors\" element child count", 1, errorListEntrySet.size());

        final JsonElement errorListElement = errorListEntrySet.iterator().next().getValue();
        assertEquals("\"errors\" child Json element name", "error", errorListEntrySet.iterator().next().getKey());
        assertTrue("\"error\" Json element is not an Array", errorListElement.isJsonArray());

        // As a final check, make sure there aren't multiple "error" array
        // elements. Unfortunately,
        // the call above to getAsJsonObject().entrySet() will out duplicate
        // "error" elements. So
        // we'll use regex on the json string to verify this.

        final Matcher matcher = Pattern.compile("\"error\"[ ]*:[ ]*\\[", Pattern.DOTALL).matcher(bos.toString());
        assertTrue("Expected 1 \"error\" element", matcher.find());
        assertFalse("Found multiple \"error\" elements", matcher.find());

        return errorListElement.getAsJsonArray();
    }

    void verifyJsonErrorNode(final JsonElement errorEntryElement, final ErrorType expErrorType,
            final ErrorTag expErrorTag, final String expErrorMessage, final String expErrorAppTag,
            final ErrorInfoVerifier errorInfoVerifier) {

        JsonElement errorInfoElement = null;
        final Map<String, String> leafMap = Maps.newHashMap();
        for (final Entry<String, JsonElement> entry : errorEntryElement.getAsJsonObject().entrySet()) {
            final String leafName = entry.getKey();
            final JsonElement leafElement = entry.getValue();

            if ("error-info".equals(leafName)) {
                assertNotNull("Found unexpected \"error-info\" element", errorInfoVerifier);
                errorInfoElement = leafElement;
            } else {
                assertTrue("\"error\" leaf Json element " + leafName + " is not a Primitive",
                        leafElement.isJsonPrimitive());

                leafMap.put(leafName, leafElement.getAsString());
            }
        }

        assertEquals("error-type", expErrorType.getErrorTypeTag(), leafMap.remove("error-type"));
        assertEquals("error-tag", expErrorTag.getTagValue(), leafMap.remove("error-tag"));

        verifyOptionalJsonLeaf(leafMap.remove("error-message"), expErrorMessage, "error-message");
        verifyOptionalJsonLeaf(leafMap.remove("error-app-tag"), expErrorAppTag, "error-app-tag");

        if (!leafMap.isEmpty()) {
            fail("Found unexpected Json leaf elements for \"error\" element: " + leafMap);
        }

        if (errorInfoVerifier != null) {
            assertNotNull("Missing \"error-info\" element", errorInfoElement);
            errorInfoVerifier.verifyJson(errorInfoElement);
        }
    }

    void verifyOptionalJsonLeaf(final String actualValue, final String expValue, final String tagName) {
        if (expValue != null) {
            assertEquals(tagName, expValue, actualValue);
        } else {
            assertNull("Found unexpected \"error\" leaf entry for: " + tagName, actualValue);
        }
    }

    void verifyXMLResponseBody(final InputStream stream, final ErrorType expErrorType, final ErrorTag expErrorTag,
            final String expErrorMessage, final String expErrorAppTag, final ErrorInfoVerifier errorInfoVerifier)
            throws Exception {

        final Document doc = parseXMLDocument(stream);

        final NodeList children = getXMLErrorList(doc, 1);

        verifyXMLErrorNode(children.item(0), expErrorType, expErrorTag, expErrorMessage, expErrorAppTag,
                errorInfoVerifier);
    }

    private Document parseXMLDocument(final InputStream stream) throws IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteStreams.copy(stream, bos);

        System.out.println("XML: " + bos.toString());

        Document doc = null;
        try {
            doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bos.toByteArray()));
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid XML response:\n" + bos.toString(), e);
        }
        return doc;
    }

    void verifyXMLErrorNode(final Node errorNode, final ErrorType expErrorType, final ErrorTag expErrorTag,
            final String expErrorMessage, final String expErrorAppTag, final ErrorInfoVerifier errorInfoVerifier)
            throws Exception {

        final String errorType = (String) ERROR_TYPE.evaluate(errorNode, XPathConstants.STRING);
        assertEquals("error-type", expErrorType.getErrorTypeTag(), errorType);

        final String errorTag = (String) ERROR_TAG.evaluate(errorNode, XPathConstants.STRING);
        assertEquals("error-tag", expErrorTag.getTagValue(), errorTag);

        verifyOptionalXMLLeaf(errorNode, ERROR_MESSAGE, expErrorMessage, "error-message");
        verifyOptionalXMLLeaf(errorNode, ERROR_APP_TAG, expErrorAppTag, "error-app-tag");

        final Node errorInfoNode = (Node) ERROR_INFO.evaluate(errorNode, XPathConstants.NODE);
        if (errorInfoVerifier != null) {
            assertNotNull("Missing \"error-info\" node", errorInfoNode);

            errorInfoVerifier.verifyXML(errorInfoNode);
        } else {
            assertNull("Found unexpected \"error-info\" node", errorInfoNode);
        }
    }

    void verifyOptionalXMLLeaf(final Node fromNode, final XPathExpression xpath, final String expValue,
            final String tagName) throws Exception {
        if (expValue != null) {
            final String actual = (String) xpath.evaluate(fromNode, XPathConstants.STRING);
            assertEquals(tagName, expValue, actual);
        } else {
            assertNull("Found unexpected \"error\" leaf entry for: " + tagName,
                    xpath.evaluate(fromNode, XPathConstants.NODE));
        }
    }

    NodeList getXMLErrorList(final Node fromNode, final int count) throws Exception {
        final NodeList errorList = (NodeList) ERROR_LIST.evaluate(fromNode, XPathConstants.NODESET);
        assertNotNull("Root errors node is empty", errorList);
        assertEquals("Root errors node child count", count, errorList.getLength());
        return errorList;
    }
}
