/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.util;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;

class NodeContainerProxy implements ContainerSchemaNode {

    private final Map<QName, DataSchemaNode> childNodes;
    private final QName qName;
    private final Set<AugmentationSchema> availableAugmentations;

    public NodeContainerProxy(final QName qName, final Map<QName, DataSchemaNode> childNodes, final Set<AugmentationSchema> availableAugmentations) {
        this.availableAugmentations = availableAugmentations;
        this.childNodes = Preconditions.checkNotNull(childNodes, "childNodes");
        this.qName = qName;
    }

    public NodeContainerProxy(final QName qName, final Collection<DataSchemaNode> childNodes) {
        this(qName, asMap(childNodes), Collections.<AugmentationSchema>emptySet());
    }

    public NodeContainerProxy(final QName qName, final Collection<DataSchemaNode> childNodes, final Set<AugmentationSchema> availableAugmentations) {
        this(qName, asMap(childNodes), availableAugmentations);
    }

    private static Map<QName, DataSchemaNode> asMap(final Collection<DataSchemaNode> childNodes) {
        return Maps.uniqueIndex(childNodes, new Function<DataSchemaNode, QName>() {
            @Override
            public QName apply(final DataSchemaNode input) {
                return input.getQName();
            }
        });
    }

    @Override
    public Set<TypeDefinition<?>> getTypeDefinitions() {
        return Collections.emptySet();
    }

    @Override
    public Set<DataSchemaNode> getChildNodes() {
        return Sets.newHashSet(childNodes.values());
    }

    @Override
    public Set<GroupingDefinition> getGroupings() {
        return Collections.emptySet();
    }

    @Override
    public DataSchemaNode getDataChildByName(final QName qName) {
        return childNodes.get(qName);
    }

    @Override
    public DataSchemaNode getDataChildByName(final String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<UsesNode> getUses() {
        return Collections.emptySet();
    }

    @Override
    public boolean isPresenceContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<AugmentationSchema> getAvailableAugmentations() {
        return availableAugmentations;
    }

    @Override
    public boolean isAugmenting() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAddedByUses() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintDefinition getConstraints() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QName getQName() {
        return qName;
    }

    @Override
    public SchemaPath getPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getReference() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Status getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return Collections.emptyList();
    }
}
