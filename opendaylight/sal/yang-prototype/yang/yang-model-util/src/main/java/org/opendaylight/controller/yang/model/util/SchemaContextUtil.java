/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.net.URI;
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
import org.opendaylight.controller.yang.model.api.TypeDefinition;

public final class SchemaContextUtil {

    private SchemaContextUtil() {}

    public static DataSchemaNode findDataSchemaNode(final SchemaContext context, final SchemaPath schemaPath) {
        if (schemaPath != null) {
            final Module module = resolveModuleFromSchemaPath(context, schemaPath);
            final Queue<QName> prefixedPath = new LinkedList<>(schemaPath.getPath());

            if ((module != null) && (prefixedPath != null)) {
                return findSchemaNodeForGivenPath(context, module, prefixedPath);
            }
        }
        return null;
    }

    public static DataSchemaNode findDataSchemaNode(final SchemaContext context, final Module module,
            final RevisionAwareXPath nonCondXPath) {
        if (nonCondXPath != null) {
            final String strXPath = nonCondXPath.toString();

            if (strXPath != null) {
                if (strXPath.matches(".*//[.* | .*//].*")) {
                    // TODO: function to escape conditions in path
                }
                if (nonCondXPath.isAbsolute()) {
                    final Queue<QName> qnamedPath = xpathToQNamePath(context, module,
                            strXPath);
                    if (qnamedPath != null) {
                        final DataSchemaNode dataNode = findSchemaNodeForGivenPath(context,
                                module, qnamedPath);
                        return dataNode;
                    }
                }
            }
        }
        return null;
    }

    public static DataSchemaNode findDataSchemaNodeForRelativeXPath(final SchemaContext context,
            final Module module, final SchemaNode actualSchemaNode,
            final RevisionAwareXPath relativeXPath) {
        if ((actualSchemaNode != null) && (relativeXPath != null)
                && !relativeXPath.isAbsolute()) {

            final SchemaPath actualNodePath = actualSchemaNode.getPath();
            if (actualNodePath != null) {
                final Queue<QName> qnamePath = resolveRelativeXPath(context, module,
                        relativeXPath, actualNodePath);

                if (qnamePath != null) {
                    final DataSchemaNode dataNode = findSchemaNodeForGivenPath(context,
                            module, qnamePath);
                    return dataNode;
                }
            }
        }

        return null;
    }

    private static Module resolveModuleFromSchemaPath(final SchemaContext
        context, final SchemaPath schemaPath) {
        if ((schemaPath != null) && (schemaPath.getPath() != null)) {
            final List<QName> path = schemaPath.getPath();
            if (!path.isEmpty()) {
                final QName qname = path.get(path.size() - 1);

                if ((qname != null) && (qname.getNamespace() != null)) {
                    return context.findModuleByNamespace(qname.getNamespace());
                }
            }
        }
        return null;
    }

    public static Module findParentModuleForTypeDefinition(
            final SchemaContext context, final TypeDefinition<?> type) {
        final SchemaPath schemaPath = type.getPath();
        if ((schemaPath != null) && (schemaPath.getPath() != null)) {
            if(type instanceof ExtendedType) {
                List<QName> path = schemaPath.getPath();
                final QName qname = path.get(path.size() - 1);

                if ((qname != null) && (qname.getNamespace() != null)) {
                    return context.findModuleByNamespace(qname.getNamespace());
                }
            } else {
                List<QName> path = schemaPath.getPath();
                final QName qname = path.get(path.size() - 2);

                if ((qname != null) && (qname.getNamespace() != null)) {
                    return context.findModuleByNamespace(qname.getNamespace());
                }
            }

        }
        return null;
    }

    public static Module findParentModule(final SchemaContext context, final SchemaNode schemaNode) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (schemaNode == null) {
            throw new IllegalArgumentException("Schema Node cannot be NULL!");
        }

        final SchemaPath schemaPath = schemaNode.getPath();
        if (schemaPath == null) {
            throw new IllegalStateException("Schema Path for Schema Node is not " +
                    "set properly (Schema Path is NULL)");
        }
        final List<QName> qnamedPath = schemaPath.getPath();
        if (qnamedPath == null || qnamedPath.isEmpty()) {
            throw new IllegalStateException("Schema Path contains invalid state of path parts." +
                    "The Schema Path MUST contain at least ONE QName which defines namespace and Local name" +
                    "of path.");
        }
        final QName qname = qnamedPath.get(qnamedPath.size() - 1);
        return context.findModuleByNamespace(qname.getNamespace());
    }

    private static DataSchemaNode findSchemaNodeForGivenPath(final SchemaContext context, final Module module,
            final Queue<QName> qnamedPath) {
        if ((module != null) && (module.getNamespace() != null)
                && (qnamedPath != null)) {
            DataNodeContainer nextNode = module;
            final URI moduleNamespace = module.getNamespace();

            QName childNodeQName = null;
            DataSchemaNode schemaNode = null;
            while ((nextNode != null) && !qnamedPath.isEmpty()) {
                childNodeQName = qnamedPath.peek();
                if (childNodeQName != null) {
                    final URI childNodeNamespace = childNodeQName.getNamespace();

                    schemaNode = nextNode.getDataChildByName(childNodeQName);
                    if (schemaNode != null) {
                        if (schemaNode instanceof ContainerSchemaNode) {
                            nextNode = (ContainerSchemaNode) schemaNode;
                        } else if (schemaNode instanceof ListSchemaNode) {
                            nextNode = (ListSchemaNode) schemaNode;
                        } else {
                            nextNode = null;
                        }
                    } else if (!childNodeNamespace.equals(moduleNamespace)) {
                        final Module nextModule = context
                                .findModuleByNamespace(childNodeNamespace);
                        schemaNode = findSchemaNodeForGivenPath(context, nextModule,
                                qnamedPath);
                        return schemaNode;
                    }
                    qnamedPath.poll();
                }
            }
            return schemaNode;
        }
        return null;
    }

    private static Queue<QName> xpathToQNamePath(final SchemaContext context, final Module parentModule,
            final String xpath) {
        final Queue<QName> path = new LinkedList<>();
        if (xpath != null) {
            final String[] prefixedPath = xpath.split("/");

            for (int i = 0; i < prefixedPath.length; ++i) {
                if (!prefixedPath[i].isEmpty()) {
                    path.add(stringPathPartToQName(context, parentModule, prefixedPath[i]));
                }
            }
        }
        return path;
    }

    private static QName stringPathPartToQName(final SchemaContext context, final Module parentModule,
            final String prefixedPathPart) {
        if (parentModule != null && prefixedPathPart != null) {
            if (prefixedPathPart.contains(":")) {
                final String[] prefixedName = prefixedPathPart.split(":");
                final Module module = resolveModuleForPrefix(context, parentModule,
                        prefixedName[0]);
                if (module != null) {
                    return new QName(module.getNamespace(), module
                            .getRevision(), prefixedName[1]);
                }
            } else {
                return new QName(parentModule.getNamespace(),
                        parentModule.getRevision(), prefixedPathPart);
            }
        }
        return null;
    }

    private static Module resolveModuleForPrefix(final SchemaContext context, final Module module,
            final String prefix) {
        if ((module != null) && (prefix != null)) {
            if (prefix.equals(module.getPrefix())) {
                return module;
            }

            final Set<ModuleImport> imports = module.getImports();

            for (final ModuleImport mi : imports) {
                if (prefix.equals(mi.getPrefix())) {
                    return context.findModuleByName(mi.getModuleName(),
                            mi.getRevision());
                }
            }
        }
        return null;
    }

    private static Queue<QName> resolveRelativeXPath(final SchemaContext context, final Module module,
            final RevisionAwareXPath relativeXPath,
            final SchemaPath leafrefSchemaPath) {
        final Queue<QName> absolutePath = new LinkedList<>();

        if ((module != null) && (relativeXPath != null) && !relativeXPath.isAbsolute()
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
                        int lenght = path.size() - colCount - 1;
                        for (int i = 0; i < lenght; ++i) {
                            absolutePath.add(path.get(i));
                        }
                        for (int i = colCount; i < xpaths.length; ++i) {
                            absolutePath.add(stringPathPartToQName(context, module, xpaths[i]));
                        }
                    }
                }
            }
        }
        return absolutePath;
    }
}
