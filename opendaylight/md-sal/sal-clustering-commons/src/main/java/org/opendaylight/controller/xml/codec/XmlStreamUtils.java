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
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
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

    for (final Entry<URI, String> e: prefixes.getPrefixes()) {
      writer.writeNamespace(e.getValue(), e.getKey().toString());
    }
    if(LOG.isDebugEnabled()) {
        LOG.debug("Instance identifier with Random prefix is now {}", str);
    }
    writer.writeCharacters(str);
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
        } catch (final ClassCastException e) {
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
