/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.xml.codec;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.AttributesContainer;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.data.impl.schema.SchemaUtils;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for bridging JAXP Stream and YANG Data APIs. Note that the definition of this class
 * by no means final and subject to change as more functionality is centralized here.
 */
@Beta
public class XmlStreamUtils {
  private static final Logger LOG = LoggerFactory.getLogger(XmlStreamUtils.class);
  private final XmlCodecProvider codecProvider;

  protected XmlStreamUtils(final XmlCodecProvider codecProvider) {
    this.codecProvider = Preconditions.checkNotNull(codecProvider);
  }

  /**
   * Create a new instance encapsulating a particular codec provider.
   *
   * @param codecProvider XML codec provider
   * @return A new instance
   */
  public static XmlStreamUtils create(final XmlCodecProvider codecProvider) {
    return new XmlStreamUtils(codecProvider);
  }

  /**
   * Check if a particular data element can be emitted as an empty element, bypassing value encoding. This
   * functionality is optional, as valid XML stream is produced even if start/end element is produced unconditionally.
   *
   * @param data Data node
   * @return True if the data node will result in empty element body.
   */
  public static boolean isEmptyElement(final Node<?> data) {
    if (data == null) {
      return true;
    }

    if (data instanceof CompositeNode) {
      return ((CompositeNode) data).getValue().isEmpty();
    }
    if (data instanceof SimpleNode) {
      return data.getValue() == null;
    }

    // Safe default
    return false;
  }

  /**
   * Write an InstanceIdentifier into the output stream. Calling corresponding {@link javax.xml.stream.XMLStreamWriter#writeStartElement(String)}
   * and {@link javax.xml.stream.XMLStreamWriter#writeEndElement()} is the responsibility of the caller.
   *
   * @param writer XML Stream writer
   * @param id InstanceIdentifier
   * @throws javax.xml.stream.XMLStreamException
   */
  public static void write(final @Nonnull XMLStreamWriter writer, final @Nonnull YangInstanceIdentifier id) throws XMLStreamException {
    Preconditions.checkNotNull(writer, "Writer may not be null");
    Preconditions.checkNotNull(id, "Variable should contain instance of instance identifier and can't be null");
    LOG.debug("Writing Instance identifier with Random prefix");
    final RandomPrefix prefixes = new RandomPrefix();
    final String str = XmlUtils.encodeIdentifier(prefixes, id);

    for (Entry<URI, String> e: prefixes.getPrefixes()) {
      writer.writeNamespace(e.getValue(), e.getKey().toString());
    }
    if(LOG.isDebugEnabled()) {
        LOG.debug("Instance identifier with Random prefix is now {}", str);
    }
    writer.writeCharacters(str);
  }

  /**
   * Write a full XML document corresponding to a CompositeNode into an XML stream writer.
   *
   * @param writer XML Stream writer
   * @param data data node
   * @param schema corresponding schema node, may be null
   * @throws javax.xml.stream.XMLStreamException if an encoding problem occurs
   */
  public void writeDocument(final @Nonnull XMLStreamWriter writer, final @Nonnull CompositeNode data, final @Nullable SchemaNode schema) throws XMLStreamException {
    // final Boolean repairing = (Boolean) writer.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
    // Preconditions.checkArgument(repairing == true, "XML Stream Writer has to be repairing namespaces");

    writer.writeStartDocument();
    writeElement(writer, data, schema);
    writer.writeEndDocument();
    writer.flush();
  }


  /**
   * Write an element into a XML stream writer. This includes the element start/end tags and
   * the value of the element.
   *
   * @param writer XML Stream writer
   * @param data data node
   * @param schema Schema node
   * @throws javax.xml.stream.XMLStreamException if an encoding problem occurs
   */
  public void writeElement(final XMLStreamWriter writer, final @Nonnull Node<?> data, final SchemaNode schema) throws XMLStreamException {
    final QName qname = data.getNodeType();
    final String ns = qname.getNamespace() != null ? qname.getNamespace().toString() : "";

    if (isEmptyElement(data)) {
      writer.writeEmptyElement("", qname.getLocalName(), ns);
      return;
    }

    writer.writeStartElement("", qname.getLocalName(), ns);
    if (data instanceof AttributesContainer && ((AttributesContainer) data).getAttributes() != null) {
      for (Entry<QName, String> attribute : ((AttributesContainer) data).getAttributes().entrySet()) {
        writer.writeAttribute(attribute.getKey().getNamespace().toString(), attribute.getKey().getLocalName(), attribute.getValue());
      }
    }

    if (data instanceof SimpleNode<?>) {
      LOG.debug("writeElement : node is of type SimpleNode");
      // Simple node
      if (schema instanceof LeafListSchemaNode) {
        writeValue(writer, ((LeafListSchemaNode) schema).getType(), data.getValue());
      } else if (schema instanceof LeafSchemaNode) {
        writeValue(writer, ((LeafSchemaNode) schema).getType(), data.getValue());
      } else {
        Object value = data.getValue();
        if (value != null) {
          writer.writeCharacters(String.valueOf(value));
        }
      }
    } else {
      LOG.debug("writeElement : node is of type CompositeNode");
      // CompositeNode
      for (Node<?> child : ((CompositeNode) data).getValue()) {
        DataSchemaNode childSchema = null;
        if (schema instanceof DataNodeContainer) {
          childSchema = SchemaUtils.findFirstSchema(child.getNodeType(), ((DataNodeContainer) schema).getChildNodes()).orNull();
          if (childSchema == null && LOG.isDebugEnabled()) {
            LOG.debug("Probably the data node \"{}\" does not conform to schema", child == null ? "" : child.getNodeType().getLocalName());
          }
        }

        writeElement(writer, child, childSchema);
      }
    }

    writer.writeEndElement();
  }

  /**
   * Write a value into a XML stream writer. This method assumes the start and end of element is
   * emitted by the caller.
   *
   * @param writer XML Stream writer
   * @param type type definitions
   * @param value object value
   * @throws javax.xml.stream.XMLStreamException if an encoding problem occurs
   */
  public void writeValue(final @Nonnull XMLStreamWriter writer, final @Nonnull TypeDefinition<?> type, final Object value) throws XMLStreamException {
    if (value == null) {
      if(LOG.isDebugEnabled()){
        LOG.debug("Value of {}:{} is null, not encoding it", type.getQName().getNamespace(), type.getQName().getLocalName());
      }
      return;
    }

    final TypeDefinition<?> baseType = XmlUtils.resolveBaseTypeFrom(type);
    if (baseType instanceof IdentityrefTypeDefinition) {
      write(writer, (IdentityrefTypeDefinition) baseType, value);
    } else if (baseType instanceof InstanceIdentifierTypeDefinition) {
      write(writer, (InstanceIdentifierTypeDefinition) baseType, value);
    } else {
      final TypeDefinitionAwareCodec<Object, ?> codec = codecProvider.codecFor(baseType);
      String text;
      if (codec != null) {
        try {
          text = codec.serialize(value);
        } catch (ClassCastException e) {
          LOG.error("Provided node value {} did not have type {} required by mapping. Using stream instead.", value, baseType, e);
          text = String.valueOf(value);
        }
      } else {
        LOG.error("Failed to find codec for {}, falling back to using stream", baseType);
        text = String.valueOf(value);
      }
      writer.writeCharacters(text);
    }
  }

  private static void write(final @Nonnull XMLStreamWriter writer, final @Nonnull IdentityrefTypeDefinition type, final @Nonnull Object value) throws XMLStreamException {
    if (value instanceof QName) {
      final QName qname = (QName) value;

      writer.writeNamespace("x", qname.getNamespace().toString());
      writer.writeCharacters("x:" + qname.getLocalName());
    } else {
      if(LOG.isDebugEnabled()) {
        LOG.debug("Value of {}:{} is not a QName but {}", type.getQName().getNamespace(), type.getQName().getLocalName(), value.getClass());
      }
      writer.writeCharacters(String.valueOf(value));
    }
  }

  private static void write(final @Nonnull XMLStreamWriter writer, final @Nonnull InstanceIdentifierTypeDefinition type, final @Nonnull Object value) throws XMLStreamException {
    if (value instanceof YangInstanceIdentifier) {
      if(LOG.isDebugEnabled()) {
          LOG.debug("Writing InstanceIdentifier object {}", value);
      }
      write(writer, (YangInstanceIdentifier)value);
    } else {
      if(LOG.isDebugEnabled()) {
          LOG.debug("Value of {}:{} is not an InstanceIdentifier but {}", type.getQName().getNamespace(), type.getQName().getLocalName(), value.getClass());
      }
        writer.writeCharacters(String.valueOf(value));
    }
  }
}
