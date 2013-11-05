package org.opendaylight.controller.sal.rest.impl;

import static org.opendaylight.controller.sal.restconf.impl.MediaTypes.API;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;

import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.XmlTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Consumes({API+RestconfService.XML})
public enum XmlToCompositeNodeProvider implements MessageBodyReader<CompositeNode> {
    INSTANCE;
    
    private final static Logger logger = LoggerFactory.getLogger(XmlToCompositeNodeProvider.class);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public CompositeNode readFrom(Class<CompositeNode> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            Node<?> node = XmlTreeBuilder.buildDataTree(entityStream);
            if (node instanceof SimpleNode) {
                logger.info("Node is SimpleNode");
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity("XML should start with XML element that contains 1..N XML child elements.").build());
            }
            return (CompositeNode) node;
        } catch (XMLStreamException e) {
            logger.info("Error during translation of InputStream to Node\n" + e.getMessage());
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).build());
        }
    }

}
