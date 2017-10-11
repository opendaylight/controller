/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.dom.broker.util;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ExtensionDefinition;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;

/**
 * ProxySchema Context for SchemaContextProviders
 */
public class ProxySchemaContext implements SchemaContext {

    private final SchemaContextProvider schemaProvider;

    public ProxySchemaContext(final SchemaContextProvider schemaProvider) {
        this.schemaProvider = schemaProvider;
    }

    private SchemaContext getCurrentSchema() {
        Preconditions.checkState(schemaProvider.getSchemaContext() != null, "Schema context unavailable from %s", schemaProvider);
        return schemaProvider.getSchemaContext();
    }

    @Override
    public Set<DataSchemaNode> getDataDefinitions() {
        return getCurrentSchema().getDataDefinitions();
    }

    @Override
    public Set<Module> getModules() {
        return getCurrentSchema().getModules();
    }

    @Override
    public Set<NotificationDefinition> getNotifications() {
        return getCurrentSchema().getNotifications();
    }

    @Override
    public Set<RpcDefinition> getOperations() {
        return getCurrentSchema().getOperations();
    }

    @Override
    public Set<ExtensionDefinition> getExtensions() {
        return getCurrentSchema().getExtensions();
    }

    @Override
    public boolean isPresenceContainer() {
        return getCurrentSchema().isPresenceContainer();
    }

    @Override
    public Set<TypeDefinition<?>> getTypeDefinitions() {
        return getCurrentSchema().getTypeDefinitions();
    }

    @Override
    public Collection<DataSchemaNode> getChildNodes() {
        return getCurrentSchema().getChildNodes();
    }

    @Override
    public Set<GroupingDefinition> getGroupings() {
        return getCurrentSchema().getGroupings();
    }

    @Override
    public Optional<DataSchemaNode> findDataChildByName(final QName name) {
        return getCurrentSchema().findDataChildByName(name);
    }

    @Override
    public Set<UsesNode> getUses() {
        return getCurrentSchema().getUses();
    }

    @Override
    public Set<AugmentationSchemaNode> getAvailableAugmentations() {
        return getCurrentSchema().getAvailableAugmentations();
    }

    @Override
    public boolean isAugmenting() {
        return getCurrentSchema().isAugmenting();
    }

    @Override
    public boolean isAddedByUses() {
        return getCurrentSchema().isAddedByUses();
    }

    @Override
    public boolean isConfiguration() {
        return getCurrentSchema().isConfiguration();
    }

    @Override
    public ConstraintDefinition getConstraints() {
        return getCurrentSchema().getConstraints();
    }

    @Override
    public QName getQName() {
        return getCurrentSchema().getQName();
    }

    @Override
    public SchemaPath getPath() {
        return getCurrentSchema().getPath();
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return getCurrentSchema().getUnknownSchemaNodes();
    }

    @Override
    public Optional<String> getDescription() {
        return getCurrentSchema().getDescription();
    }

    @Override
    public Optional<String> getReference() {
        return getCurrentSchema().getReference();
    }

    @Override
    public Status getStatus() {
        return getCurrentSchema().getStatus();
    }

    @Override
    public Optional<Module> findModule(final String name, final Optional<Revision> revision) {
        return getCurrentSchema().findModule(name, revision);
    }
}
