/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.impl;

import static org.opendaylight.controller.sal.rest.api.Draft02.RestConfModule.ERRORS_CONTAINER_QNAME;
import static org.opendaylight.controller.sal.rest.api.Draft02.RestConfModule.ERROR_APP_TAG_QNAME;
import static org.opendaylight.controller.sal.rest.api.Draft02.RestConfModule.ERROR_INFO_QNAME;
import static org.opendaylight.controller.sal.rest.api.Draft02.RestConfModule.ERROR_LIST_QNAME;
import static org.opendaylight.controller.sal.rest.api.Draft02.RestConfModule.ERROR_MESSAGE_QNAME;
import static org.opendaylight.controller.sal.rest.api.Draft02.RestConfModule.ERROR_TAG_QNAME;
import static org.opendaylight.controller.sal.rest.api.Draft02.RestConfModule.ERROR_TYPE_QNAME;
import static org.opendaylight.controller.sal.rest.api.Draft02.RestConfModule.NAMESPACE;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.stream.JsonWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map.Entry;
import javax.activation.UnsupportedDataTypeException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * This class defines an ExceptionMapper that handles RestconfDocumentedExceptions thrown by resource implementations
 * and translates appropriately to restconf error response as defined in the RESTCONF RFC draft.
 *
 * @author Thomas Pantelis
 */
@Provider
public class RestconfDocumentedExceptionMapper implements ExceptionMapper<RestconfDocumentedException> {

    private final static Logger LOG = LoggerFactory.getLogger(RestconfDocumentedExceptionMapper.class);
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(final RestconfDocumentedException exception) {

        LOG.debug("In toResponse: {}", exception.getMessage());



        List<MediaType> accepts = headers.getAcceptableMediaTypes();
        accepts.remove(MediaType.WILDCARD_TYPE);

        LOG.debug("Accept headers: {}", accepts);

        final MediaType mediaType;
        if (accepts != null && accepts.size() > 0) {
            mediaType = accepts.get(0); // just pick the first one
        } else {
            // Default to the content type if there's no Accept header
            mediaType = MediaType.APPLICATION_JSON_TYPE;
        }

        LOG.debug("Using MediaType: {}", mediaType);

        List<RestconfError> errors = exception.getErrors();
        if (errors.isEmpty()) {
            // We don't actually want to send any content but, if we don't set any content here,
            // the tomcat front-end will send back an html error report. To prevent that, set a
            // single space char in the entity.

            return Response.status(exception.getStatus()).type(MediaType.TEXT_PLAIN_TYPE).entity(" ").build();
        }

        int status = errors.iterator().next().getErrorTag().getStatusCode();

        ControllerContext context = ControllerContext.getInstance();
        DataNodeContainer errorsSchemaNode = (DataNodeContainer) context.getRestconfModuleErrorsSchemaNode();

        if (errorsSchemaNode == null) {
            return Response.status(status).type(MediaType.TEXT_PLAIN_TYPE).entity(exception.getMessage()).build();
        }

        ImmutableList.Builder<Node<?>> errorNodes = ImmutableList.<Node<?>> builder();
        for (RestconfError error : errors) {
            errorNodes.add(toDomNode(error));
        }

        ImmutableCompositeNode errorsNode = ImmutableCompositeNode.create(ERRORS_CONTAINER_QNAME, errorNodes.build());

        Object responseBody;
        if (mediaType.getSubtype().endsWith("json")) {
            responseBody = toJsonResponseBody(errorsNode, errorsSchemaNode);
        } else {
            responseBody = toXMLResponseBody(errorsNode, errorsSchemaNode);
        }

        return Response.status(status).type(mediaType).entity(responseBody).build();
    }

    private Object toJsonResponseBody(final ImmutableCompositeNode errorsNode, final DataNodeContainer errorsSchemaNode) {

        JsonMapper jsonMapper = new JsonMapper(null);

        Object responseBody = null;
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(outStream, Charsets.UTF_8));
            writer.setIndent("    ");

            jsonMapper.write(writer, errorsNode, errorsSchemaNode);
            writer.flush();

            responseBody = outStream.toString("UTF-8");
        } catch (IOException e) {
            LOG.error("Error writing error response body", e);
        }

        return responseBody;
    }

    private Object toXMLResponseBody(final ImmutableCompositeNode errorsNode, final DataNodeContainer errorsSchemaNode) {

        XmlMapper xmlMapper = new XmlMapper();

        Object responseBody = null;
        try {
            Document xmlDoc = xmlMapper.write(errorsNode, errorsSchemaNode);

            responseBody = documentToString(xmlDoc);
        } catch (TransformerException | UnsupportedDataTypeException | UnsupportedEncodingException e) {
            LOG.error("Error writing error response body", e);
        }

        return responseBody;
    }

    private String documentToString(final Document doc) throws TransformerException, UnsupportedEncodingException {
        Transformer transformer = createTransformer();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        transformer.transform(new DOMSource(doc), new StreamResult(outStream));

        return outStream.toString("UTF-8");
    }

    private Transformer createTransformer() throws TransformerFactoryConfigurationError,
            TransformerConfigurationException {
        Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        return transformer;
    }

    private Node<?> toDomNode(final RestconfError error) {

        CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
        builder.setQName(ERROR_LIST_QNAME);

        addLeaf(builder, ERROR_TYPE_QNAME, error.getErrorType().getErrorTypeTag());
        addLeaf(builder, ERROR_TAG_QNAME, error.getErrorTag().getTagValue());
        addLeaf(builder, ERROR_MESSAGE_QNAME, error.getErrorMessage());
        addLeaf(builder, ERROR_APP_TAG_QNAME, error.getErrorAppTag());

        Node<?> errorInfoNode = parseErrorInfo(error.getErrorInfo());
        if (errorInfoNode != null) {
            builder.add(errorInfoNode);
        }

        return builder.build();
    }

    private Node<?> parseErrorInfo(final String errorInfo) {
        if (Strings.isNullOrEmpty(errorInfo)) {
            return null;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);

        // Wrap the error info content in a root <error-info> element so it can be parsed
        // as XML. The error info content may or may not be XML. If not then it will be
        // parsed as text content of the <error-info> element.

        String errorInfoWithRoot = new StringBuilder("<error-info xmlns=\"").append(NAMESPACE).append("\">")
                .append(errorInfo).append("</error-info>").toString();

        Document doc = null;
        try {
            doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(errorInfoWithRoot)));
        } catch (Exception e) {
            // TODO: what if the content is text that happens to contain invalid markup?
            // Could wrap in CDATA and try again.

            LOG.warn("Error parsing restconf error-info, \"{}\", as XML", errorInfo, e);
            return null;
        }

        Node<?> errorInfoNode = XmlDocumentUtils.toDomNode(doc);

        if (errorInfoNode instanceof CompositeNode) {
            CompositeNode compositeNode = (CompositeNode) XmlDocumentUtils.toDomNode(doc);

            // At this point the QName for the "error-info" CompositeNode doesn't contain the revision
            // as it isn't present in the XML. So we'll copy all the child nodes and create a new
            // CompositeNode with the full QName. This is done so the XML/JSON mapping code can
            // locate the schema.

            ImmutableList.Builder<Node<?>> childNodes = ImmutableList.builder();
            for (Entry<QName, List<Node<?>>> entry : compositeNode.entrySet()) {
                childNodes.addAll(entry.getValue());
            }

            errorInfoNode = ImmutableCompositeNode.create(ERROR_INFO_QNAME, childNodes.build());
        }

        return errorInfoNode;
    }

    private void addLeaf(final CompositeNodeBuilder<ImmutableCompositeNode> builder, final QName qname,
            final String value) {
        if (!Strings.isNullOrEmpty(value)) {
            builder.addLeaf(qname, value);
        }
    }
}
