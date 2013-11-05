package org.opendaylight.controller.sal.rest.impl;

import static org.opendaylight.controller.sal.restconf.impl.MediaTypes.API;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EmptyTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IntegerTypeDefinition;

import com.google.gson.stream.JsonWriter;

@Provider
@Produces({ API + RestconfService.JSON })
public enum StructuredDataToJsonProvider implements MessageBodyWriter<StructuredData> {
    INSTANCE;
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getSize(StructuredData t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(StructuredData t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(entityStream, "UTF-8"));
        writer.setIndent("    ");
        writer.beginObject();
        convertNodeToJsonAccordingToSchema(writer, t.getData(), t.getSchema());
        writer.endObject();
    }

    private void convertNodeToJsonAccordingToSchema(JsonWriter writer, Node<?> node, DataSchemaNode dataSchemaNode) throws IOException {
        if (node instanceof CompositeNode) {
            if (!(dataSchemaNode instanceof DataNodeContainer)) {
                throw new IllegalStateException("CompositeNode should be represented as DataNodeContainer");
            }
            if (dataSchemaNode instanceof ContainerSchemaNode) {
                writer.name(node.getNodeType().getLocalName());
                writer.beginObject();
                String listName = "";
                for (Node<?> n : ((CompositeNode) node).getChildren()) {
                    DataSchemaNode foundDataSchemaNode = findSchemaForNode(n, ((DataNodeContainer) dataSchemaNode).getChildNodes());
                    if (foundDataSchemaNode instanceof ListSchemaNode) {
                        if (listName.equals(n.getNodeType().getLocalName())) {
                            continue;
                        }
                        listName = n.getNodeType().getLocalName();
                    }
                    convertNodeToJsonAccordingToSchema(writer, n, foundDataSchemaNode);
                }
                writer.endObject();
            } else if (dataSchemaNode instanceof ListSchemaNode) {
                writer.name(node.getNodeType().getLocalName());
                writer.beginArray();
                List<Node<?>> nodeSiblings = node.getParent().getChildren();
                for (Node<?> nodeSibling : nodeSiblings) {
                    if (nodeSibling.getNodeType().getLocalName().equals(node.getNodeType().getLocalName())) {
                        DataSchemaNode schemaForNodeSibling = findSchemaForNode(nodeSibling,
                                ((DataNodeContainer) dataSchemaNode.getParent()).getChildNodes());
                        writer.beginObject();
                        for (Node<?> child : ((CompositeNode) nodeSibling).getChildren()) {
                            DataSchemaNode schemaForChild = findSchemaForNode(child,
                                    ((DataNodeContainer) schemaForNodeSibling).getChildNodes());
                            convertNodeToJsonAccordingToSchema(writer, child, schemaForChild);
                        }
                        writer.endObject();
                    }
                }
                writer.endArray();
            }
        } else if (node instanceof SimpleNode<?>) {
            if (!(dataSchemaNode instanceof LeafSchemaNode)) {
                throw new IllegalStateException("SimpleNode should should be represented as LeafSchemaNode");
            }
            writeLeaf(writer, (LeafSchemaNode) dataSchemaNode, (SimpleNode<?>) node);
        }
    }

    private DataSchemaNode findSchemaForNode(Node<?> node, Set<DataSchemaNode> dataSchemaNode) {
        for (DataSchemaNode dsn : dataSchemaNode) {
            if (node.getNodeType().getLocalName().equals(dsn.getQName().getLocalName())) {
                return dsn;
            }
        }
        return null;
    }

    private void writeLeaf(JsonWriter writer, LeafSchemaNode leafSchemaNode, SimpleNode<?> data) throws IOException {
        TypeDefinition<?> type = leafSchemaNode.getType();

        writer.name(data.getNodeType().getLocalName());

        if (type instanceof DecimalTypeDefinition) {
            writer.value((Double.valueOf((String) data.getValue())).doubleValue());
        } else if (type instanceof IntegerTypeDefinition) {
            writer.value((Integer.valueOf((String) data.getValue())).intValue());
        } else if (type instanceof EmptyTypeDefinition) {
            writer.value("[null]");
        } else {
            writer.value(String.valueOf(data.getValue()));
        }
    }

}
