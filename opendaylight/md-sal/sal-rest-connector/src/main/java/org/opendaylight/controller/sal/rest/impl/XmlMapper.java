/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Optional;
import javax.activation.UnsupportedDataTypeException;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.codec.LeafrefCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.Leafref;
import org.w3c.dom.Document;

/**
 * @deprecated class will be removed for lithium release
 */
@Deprecated
public class XmlMapper {
    private static final LeafrefCodecImpl LEAFREF_DEFAULT_CODEC = new LeafrefCodecImpl(
            Optional.<LeafrefTypeDefinition> absent());

    private static class LeafrefCodecImpl extends TypeDefinitionAwareCodec<Object, LeafrefTypeDefinition> implements
            LeafrefCodec<String> {

        protected LeafrefCodecImpl(final Optional<LeafrefTypeDefinition> typeDef) {
            super(typeDef, Object.class);
        }

        @Override
        public String serialize(final Object data) {
            return String.valueOf(data);
        }

        @Override
        public Object deserialize(final String data) {
            return data;
        }
    }

    private static class XmlCodecProviderImpl implements XmlCodecProvider {
        @Override
        public TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> codecFor(final TypeDefinition<?> baseType) {
            final TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> codec = TypeDefinitionAwareCodec
                    .from(baseType);

            if (codec == null) {
                if (baseType instanceof Leafref) {
                    return LEAFREF_DEFAULT_CODEC;
                }
            }
            return codec;
        }
    }

    private static final XmlCodecProvider XML_CODEC_PROVIDER_IMPL = new XmlCodecProviderImpl();

    public Document write(final CompositeNode data, final DataNodeContainer schema) throws UnsupportedDataTypeException {
        return XmlDocumentUtils.toDocument(data, schema, XML_CODEC_PROVIDER_IMPL);
    }
}
