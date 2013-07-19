/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.impl;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.NotificationDefinition;
import org.opendaylight.controller.yang.model.api.RpcDefinition;
import org.opendaylight.controller.yang.model.api.SchemaContext;

final class SchemaContextImpl implements SchemaContext {
    private final Set<Module> modules;

    SchemaContextImpl(final Set<Module> modules) {
        this.modules = modules;
    }

    @Override
    public Set<DataSchemaNode> getDataDefinitions() {
        final Set<DataSchemaNode> dataDefs = new HashSet<DataSchemaNode>();
        for (Module m : modules) {
            dataDefs.addAll(m.getChildNodes());
        }
        return dataDefs;
    }

    @Override
    public Set<Module> getModules() {
        return modules;
    }

    @Override
    public Set<NotificationDefinition> getNotifications() {
        final Set<NotificationDefinition> notifications = new HashSet<NotificationDefinition>();
        for (Module m : modules) {
            notifications.addAll(m.getNotifications());
        }
        return notifications;
    }

    @Override
    public Set<RpcDefinition> getOperations() {
        final Set<RpcDefinition> rpcs = new HashSet<RpcDefinition>();
        for (Module m : modules) {
            rpcs.addAll(m.getRpcs());
        }
        return rpcs;
    }

    @Override
    public Set<ExtensionDefinition> getExtensions() {
        final Set<ExtensionDefinition> extensions = new HashSet<ExtensionDefinition>();
        for (Module m : modules) {
            extensions.addAll(m.getExtensionSchemaNodes());
        }
        return extensions;
    }

    @Override
    public Module findModuleByName(final String name, final Date revision) {
        if (name != null) {
            for (final Module module : modules) {
                if (revision == null) {
                    if (module.getName().equals(name)) {
                        return module;
                    }
                } else if (module.getName().equals(name) && module.getRevision().equals(revision)) {
                    return module;
                }
            }
        }
        return null;
    }

    @Override
    public Module findModuleByNamespace(final URI namespace) {
        if (namespace != null) {
            for (final Module module : modules) {
                if (module.getNamespace().equals(namespace)) {
                    return module;
                }
            }
        }
        return null;
    }

}
