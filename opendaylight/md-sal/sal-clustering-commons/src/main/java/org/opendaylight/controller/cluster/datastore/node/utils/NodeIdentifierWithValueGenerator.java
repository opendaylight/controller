/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeIdentifierWithValueGenerator{
        private final String id;
    private final DataSchemaNode schemaNode;
    private static final Pattern pattern = Pattern.compile("(.*)\\Q[\\E(.*)\\Q]\\E");
        private final Matcher matcher;
        private final boolean doesMatch;

        public NodeIdentifierWithValueGenerator(String id, DataSchemaNode schemaNode){
            this.id = id;
            this.schemaNode = schemaNode;
            matcher = pattern.matcher(this.id);
            doesMatch = matcher.matches();
        }

        public boolean matches(){
            return doesMatch;
        }

        public YangInstanceIdentifier.PathArgument getPathArgument(){
            final String name = matcher.group(1);
            final String value = matcher.group(2);

            return new YangInstanceIdentifier.NodeWithValue<>(
                QNameFactory.create(name), getValue(value));
        }

        private Object getValue(String value){
            if(schemaNode != null){
                if(schemaNode instanceof LeafListSchemaNode){
                    return TypeDefinitionAwareCodec
                        .from(LeafListSchemaNode.class.cast(schemaNode).getType()).deserialize(value);

                }
            }
        return value;
        }

    }
