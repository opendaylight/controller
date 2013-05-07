/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.DataNodeContainer;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;

public final class SchemaContextUtil {

    private final SchemaContext context;

    public SchemaContextUtil(final SchemaContext context) {
        this.context = context;
    }
    
    public SchemaContext getContext() {
        return context;
    }
    
    public DataSchemaNode findDataSchemaNode(final SchemaPath schemaPath) {
        if (schemaPath != null) {
            final Module module = resolveModuleFromSchemaPath(schemaPath);
            final Queue<String> prefixedPath = schemaPathToQueuedPath(schemaPath);
            
            if ((module != null) && (prefixedPath != null)) {
                return findSchemaNodeForGivenPath(module, prefixedPath);
            }
        }
        return null;
    }
    
    public DataSchemaNode findDataSchemaNode(final Module module,
            final RevisionAwareXPath nonCondXPath) {
        if (nonCondXPath != null) {
            final String strXPath = nonCondXPath.toString();

            if (strXPath != null) {
                if (strXPath.matches(".*//[.* | .*//].*")) {
                    // TODO: function to escape conditions in path   
                }
                if (nonCondXPath.isAbsolute()) {
                    final Queue<String> queuedPath = xpathToQueuedPath(strXPath);
                    if (queuedPath != null) {
                        final DataSchemaNode dataNode = findSchemaNodeForGivenPath(
                                module, queuedPath);
                        return dataNode;
                    }
                }
            }
        }
        return null;
    }

    public DataSchemaNode findDataSchemaNodeForRelativeXPath(
            final Module module, final SchemaNode actualSchemaNode,
            final RevisionAwareXPath relativeXPath) {
        if ((actualSchemaNode != null) && (relativeXPath != null)
                && !relativeXPath.isAbsolute()) {

            final SchemaPath actualNodePath = actualSchemaNode.getPath();
            if (actualNodePath != null) {
                final Queue<String> queuedPath = resolveRelativeXPath(
                        relativeXPath, actualNodePath);

                if (queuedPath != null) {
                    final DataSchemaNode dataNode = findSchemaNodeForGivenPath(
                            module, queuedPath);
                    return dataNode;
                }
            }
        }

        return null;
    }
    
    public Module resolveModuleFromSchemaPath(final SchemaPath schemaPath) {
        if ((schemaPath != null) && (schemaPath.getPath() != null)) {
            final QName qname = schemaPath.getPath().get(0);

            if ((qname != null) && (qname.getNamespace() != null)) {
                return context
                        .findModuleByNamespace(qname.getNamespace());
            }
        }
        return null;
    }
    
    /**
     * Search which starts from root of Module.
     * 
     * @param module
     * @param prefixedPath
     * @return
     */
    private DataSchemaNode findSchemaNodeForGivenPath(final Module module,
            final Queue<String> prefixedPath) {
        if ((module != null) && (prefixedPath != null)) {
            DataNodeContainer nextContainer = module;
            final String modulePrefix = module.getPrefix();

            String childNodeName = null;
            DataSchemaNode schemaNode = null;
            while ((nextContainer != null) && (prefixedPath.size() > 0)) {
                childNodeName = prefixedPath.poll();
                if (childNodeName.contains(":")) {
                    final String[] prefixedChildNode = childNodeName.split(":");
                    if ((modulePrefix != null)
                            && modulePrefix.equals(prefixedChildNode[0])) {

                        childNodeName = prefixedChildNode[1];
                    } else {
                        final Module nextModule = resolveModuleForPrefix(
                                prefixedChildNode[0], module);
                        final Queue<String> nextModulePrefixedPath = new LinkedList<String>();
                        nextModulePrefixedPath.add(childNodeName);
                        nextModulePrefixedPath.addAll(prefixedPath);
                        prefixedPath.clear();

                        schemaNode = findSchemaNodeForGivenPath(nextModule,
                                nextModulePrefixedPath);

                        return schemaNode;
                    }
                }

                schemaNode = nextContainer.getDataChildByName(childNodeName);
                if (schemaNode instanceof ContainerSchemaNode) {
                    nextContainer = (ContainerSchemaNode) schemaNode;
                } else if (schemaNode instanceof ListSchemaNode) {
                    nextContainer = (ListSchemaNode) schemaNode;
                } else {
                    return schemaNode;
                }
            }
        }
        return null;
    }

    private Module resolveModuleForPrefix(final String prefix,
            final Module parent) {
        if ((prefix != null) && (parent != null)) {
            final Set<ModuleImport> imports = parent.getImports();

            if (imports != null) {
                for (final ModuleImport impModule : imports) {
                    final String impModPrefix = impModule.getPrefix();
                    if ((impModPrefix != null) && prefix.equals(impModPrefix)) {
                        return resolveModuleFromContext(prefix,
                                impModule.getModuleName());
                    }
                }
            }
        }
        return null;
    }

    private Module resolveModuleFromContext(final String prefix,
            final String moduleName) {
        final Set<Module> modules = context.getModules();

        if ((prefix != null) && (moduleName != null) && (modules != null)) {
            for (Module module : modules) {
                if ((module != null) && prefix.equals(module.getPrefix())
                        && moduleName.equals(module.getName())) {
                    return module;
                }
            }
        }
        return null;
    }

    private Queue<String> xpathToQueuedPath(final String xpath) {
        final Queue<String> retQueue = new LinkedList<String>();
        if ((xpath != null)) {
            final String[] prefixedPath = xpath.split("/");

            if (prefixedPath != null) {
                for (int i = 0; i < prefixedPath.length; ++i) {
                    if (!prefixedPath[i].isEmpty()) {
                        retQueue.add(prefixedPath[i]);
                    }
                }
            }
        }
        return retQueue;
    }
    
    private Queue<String> schemaPathToQueuedPath(final SchemaPath schemaPath) {
        final Queue<String> retQueue = new LinkedList<String>();
        if ((schemaPath != null) && (schemaPath.getPath() != null)) {
            final List<QName> listPath = schemaPath.getPath();
            
            for (final QName qname : listPath) {
                if (qname != null) {
                    final String prefix = qname.getPrefix();
                    final String localName = qname.getLocalName();
                    
                    final StringBuilder builder = new StringBuilder();
                    if (prefix != null) {
                        builder.append(prefix);
                        builder.append(":");
                    }
                    builder.append(localName);
                    retQueue.add(builder.toString());
                }
            }
        }
        return retQueue;
    }
    
    private Queue<String> resolveRelativeXPath(
            final RevisionAwareXPath relativeXPath,
            final SchemaPath leafrefSchemaPath) {
        final Queue<String> absolutePath = new LinkedList<String>();

        if ((relativeXPath != null) && !relativeXPath.isAbsolute()
                && (leafrefSchemaPath != null)) {
            final String strXPath = relativeXPath.toString();
            if (strXPath != null) {
                final String[] xpaths = strXPath.split("/");

                if (xpaths != null) {
                    int colCount = 0;
                    while (xpaths[colCount].contains("..")) {
                        ++colCount;
                    }
                    final List<QName> path = leafrefSchemaPath.getPath();
                    if (path != null) {
                        int lenght = path.size() - colCount;
                        for (int i = 0; i < lenght; ++i) {
                            absolutePath.add(path.get(i).getLocalName());
                        }
                        for (int i = colCount; i < xpaths.length; ++i) {
                            absolutePath.add(xpaths[i]);
                        }
                    }
                }
            }
        }
        return absolutePath;
    }
}
