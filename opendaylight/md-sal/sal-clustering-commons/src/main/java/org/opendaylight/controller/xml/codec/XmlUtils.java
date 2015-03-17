/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.xml.codec;

import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

/**
 * Common XML-related utility methods, which are not specific to a particular
 * JAXP API.
 */
public class XmlUtils {

  public static final XmlCodecProvider DEFAULT_XML_CODEC_PROVIDER = new XmlCodecProvider() {
    @Override
    public TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> codecFor(final TypeDefinition<?> baseType) {
      return TypeDefinitionAwareCodec.from(baseType);
    }
  };

  private XmlUtils() {
  }

  public static TypeDefinition<?> resolveBaseTypeFrom(final @Nonnull TypeDefinition<?> type) {
    TypeDefinition<?> superType = type;
    while (superType.getBaseType() != null) {
      superType = superType.getBaseType();
    }
    return superType;
  }

  /**
   * This code is picked from yangtools and modified to add type of instance identifier
   * output of instance identifier something like below for a flow ref composite node of type instance identifier,
   * which has path arguments with predicates, whose value is of type java.lang.short
   * <flow-ref xmlns:bgkj="urn:opendaylight:flow:inventory" xmlns:jdlk="urn:opendaylight:inventory">
   *   /jdlk:nodes/jdlk:node[jdlk:id='openflow:205558455098190@java.lang.String']
   *   /bgkj:table[bgkj:id='3@java.lang.Short']
   *   /bgkj:flow[bgkj:id='156@java.lang.String']
   * </flow-ref>
   *
   */

  public static String encodeIdentifier(final RandomPrefix prefixes, final YangInstanceIdentifier id) {
    final StringBuilder textContent = new StringBuilder();
    for (final PathArgument pathArgument : id.getPathArguments()) {
      textContent.append('/');
      textContent.append(prefixes.encodeQName(pathArgument.getNodeType()));
      if (pathArgument instanceof NodeIdentifierWithPredicates) {
        final Map<QName, Object> predicates = ((NodeIdentifierWithPredicates) pathArgument).getKeyValues();

        for (final QName keyValue : predicates.keySet()) {
          final Object value = predicates.get(keyValue);
          final String type = value.getClass().getName();
          final String predicateValue = String.valueOf(value);
          textContent.append('[');
          textContent.append(prefixes.encodeQName(keyValue));
          textContent.append("='");
          textContent.append(predicateValue);
          textContent.append("@");
          textContent.append(type);
          textContent.append("']");
        }
      } else if (pathArgument instanceof NodeWithValue) {
        textContent.append("[.='");
        textContent.append(((NodeWithValue) pathArgument).getValue());
        textContent.append("']");
      }
    }

    return textContent.toString();
  }
}
