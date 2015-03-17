/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.xml.codec;

import com.google.common.base.Optional;
import java.net.URI;
import java.util.Collection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlDocumentUtils {
  public static final QName OPERATION_ATTRIBUTE_QNAME = QName.create(URI.create("urn:ietf:params:xml:ns:netconf:base:1.0"), null, "operation");

  public static Document getDocument() {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    Document doc = null;
    try {
      final DocumentBuilder bob = dbf.newDocumentBuilder();
      doc = bob.newDocument();
    } catch (final ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    return doc;
  }


  public static QName qNameFromElement(final Element xmlElement) {
    final String namespace = xmlElement.getNamespaceURI();
    final String localName = xmlElement.getLocalName();
    return QName.create(namespace != null ? URI.create(namespace) : null, null, localName);
  }

  public static final Optional<DataSchemaNode> findFirstSchema(final QName qname, final Collection<DataSchemaNode> dataSchemaNode) {
    if (dataSchemaNode != null && !dataSchemaNode.isEmpty() && qname != null) {
      for (final DataSchemaNode dsn : dataSchemaNode) {
        if (qname.isEqualWithoutRevision(dsn.getQName())) {
          return Optional.<DataSchemaNode> of(dsn);
        } else if (dsn instanceof ChoiceSchemaNode) {
          for (final ChoiceCaseNode choiceCase : ((ChoiceSchemaNode) dsn).getCases()) {
            final Optional<DataSchemaNode> foundDsn = findFirstSchema(qname, choiceCase.getChildNodes());
            if (foundDsn != null && foundDsn.isPresent()) {
              return foundDsn;
            }
          }
        }
      }
    }
    return Optional.absent();
  }

  public static final XmlCodecProvider defaultValueCodecProvider() {
    return XmlUtils.DEFAULT_XML_CODEC_PROVIDER;
  }
}
