/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.yang.types;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.DataNodeContainer;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.controller.yang.model.util.Leafref;

public class TypeProviderImpl implements TypeProvider {

    private SchemaContext schemaContext;

    public TypeProviderImpl(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.type.provider.TypeProvider#
     * javaTypeForYangType(java.lang.String)
     */
    @Override
    public Type javaTypeForYangType(String type) {
        Type t = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                .javaTypeForYangType(type);
        return t;
    }

    @Override
    public Type javaTypeForSchemaDefinitionType(final TypeDefinition<?> type) {
        Type returnType = null;
        if (type != null) {
            if (type instanceof Leafref) {
                final LeafrefTypeDefinition leafref = (LeafrefTypeDefinition) type;
                returnType = provideTypeForLeafref(leafref);
            } else if (type instanceof IdentityrefTypeDefinition) {

            } else {
                returnType = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                        .javaTypeForSchemaDefinitionType(type);
            }
        }
        return returnType;
    }

    public Type provideTypeForLeafref(final LeafrefTypeDefinition leafrefType) {
        Type returnType = null;
        if ((leafrefType != null) && (leafrefType.getPathStatement() != null)) {
            final RevisionAwareXPath xpath = leafrefType.getPathStatement();
            final String strXPath = xpath.toString();

            if (strXPath != null) {
                if (strXPath.matches(".*//[.* | .*//].*")) {
                    returnType = Types.typeForClass(Object.class);
                } else {
                    final Module module = resolveModuleFromSchemaContext(leafrefType
                            .getPath());
                    if (module != null) {
                        Queue<String> leafrefPath;
                        if (!xpath.isAbsolute()) {
                            leafrefPath = resolveRelativeXPath(xpath,
                                    leafrefType.getPath());
                        } else {
                            leafrefPath = xpathToPrefixedPath(strXPath, module.getName());
                        }

                        if (leafrefPath != null) {
                            final DataSchemaNode dataNode = findSchemaNodeForGivenPath(
                                    module, leafrefPath);
                            returnType = resolveTypeFromDataSchemaNode(dataNode);
                        }
                    }
                }
            }
        }
        return returnType;
    }

    private Type resolveTypeFromDataSchemaNode(final DataSchemaNode dataNode) {
        Type returnType = null;
        if (dataNode != null) {
            if (dataNode instanceof LeafSchemaNode) {
                final LeafSchemaNode leaf = (LeafSchemaNode) dataNode;
                returnType = javaTypeForSchemaDefinitionType(leaf.getType());
            } else if (dataNode instanceof LeafListSchemaNode) {
                final LeafListSchemaNode leafList = (LeafListSchemaNode) dataNode;
                returnType = javaTypeForSchemaDefinitionType(leafList.getType());
            }
        }
        return returnType;
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
            final String modulePrefix = module.getPrefix();
            String childNodeName = prefixedPath.poll();
            DataNodeContainer nextContainer = null;

            if ((childNodeName != null)
                    && childNodeName.equals(module.getName())) {
                nextContainer = module;
            }

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
                        
                        nextModulePrefixedPath.add(nextModule.getName());
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

    private Module resolveModuleFromSchemaContext(final SchemaPath schemaPath) {
        final Set<Module> modules = schemaContext.getModules();
        final String moduleName = resolveModuleName(schemaPath);
        if ((moduleName != null) && (modules != null)) {
            for (final Module module : modules) {
                if (module.getName().equals(moduleName)) {
                    return module;
                }
            }
        }
        return null;
    }

    private String resolveModuleName(final SchemaPath schemaPath) {
        if ((schemaPath != null) && (schemaPath.getPath() != null)) {
            final QName qname = schemaPath.getPath().get(0);
            if ((qname != null) && (qname.getLocalName() != null)) {
                return qname.getLocalName();
            }
        }
        return "";
    }

    private Queue<String> xpathToPrefixedPath(final String xpath, final String moduleName) {
        final Queue<String> retQueue = new LinkedList<String>();
        if ((xpath != null) && (moduleName != null)) {
            final String[] prefixedPath = xpath.split("/");
            
            retQueue.add(moduleName);
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
        final Set<Module> modules = schemaContext.getModules();

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
