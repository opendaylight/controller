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
import org.opendaylight.controller.yang.model.api.*;

/**
 * The Schema Context Util contains support methods for searching through Schema Context modules for specified schema
 * nodes via Schema Path or Revision Aware XPath. The Schema Context Util is designed as mixin,
 * so it is not instantiable.
 *
 * @author Lukas Sedlak <lsedlak@cisco.com>
 */
public final class SchemaContextUtil {

    private SchemaContextUtil() {
    }

    /**
     * Method attempts to find DataSchemaNode in Schema Context via specified Schema Path. The returned
     * DataSchemaNode from method will be the node at the end of the SchemaPath. If the DataSchemaNode is not present
     * in the Schema Context the method will return <code>null</code>.
     * <br>
     * In case that Schema Context or Schema Path are not specified correctly (i.e. contains <code>null</code>
     * values) the method will return IllegalArgumentException.
     *
     * @throws IllegalArgumentException
     * 
     * @param context
     *            Schema Context
     * @param schemaPath
     *            Schema Path to search for
     * @return DataSchemaNode from the end of the Schema Path or
     *         <code>null</code> if the Node is not present.
     */
    public static DataSchemaNode findDataSchemaNode(final SchemaContext context, final SchemaPath schemaPath) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (schemaPath == null) {
            throw new IllegalArgumentException("Schema Path reference cannot be NULL");
        }

        final Module module = resolveModuleFromSchemaPath(context, schemaPath);
        final Queue<QName> prefixedPath = new LinkedList<>(schemaPath.getPath());

        if ((module != null) && (prefixedPath != null)) {
            return findSchemaNodeForGivenPath(context, module, prefixedPath);
        }
        return null;
    }

    /**
     * Method attempts to find DataSchemaNode inside of provided Schema Context and Yang Module accordingly to
     * Non-conditional Revision Aware XPath. The specified Module MUST be present in Schema Context otherwise the
     * operation would fail and return <code>null</code>.
     * <br>
     * The Revision Aware XPath MUST be specified WITHOUT the conditional statement (i.e. without [cond]) in path,
     * because in this state the Schema Context is completely unaware of data state and will be not able to properly
     * resolve XPath. If the XPath contains condition the method will return IllegalArgumentException.
     * <br>
     * In case that Schema Context or Module or Revision Aware XPath contains <code>null</code> references the method
     * will throw IllegalArgumentException
     * <br>
     * If the Revision Aware XPath is correct and desired Data Schema Node is present in Yang module or in depending
     * module in Schema Context the method will return specified Data Schema Node, otherwise the operation will fail
     * and method will return <code>null</code>.
     *
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param module Yang Module
     * @param nonCondXPath Non Conditional Revision Aware XPath
     * @return Returns Data Schema Node for specified Schema Context for given Non-conditional Revision Aware XPath,
     * or <code>null</code> if the DataSchemaNode is not present in Schema Context.
     */
    public static DataSchemaNode findDataSchemaNode(final SchemaContext context, final Module module,
            final RevisionAwareXPath nonCondXPath) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        if (nonCondXPath == null) {
            throw new IllegalArgumentException("Non Conditional Revision Aware XPath cannot be NULL!");
        }

        final String strXPath = nonCondXPath.toString();
        if (strXPath != null) {
            if (strXPath.contains("[")) {
                throw new IllegalArgumentException("Revision Aware XPath cannot contains condition!");
            }
            if (nonCondXPath.isAbsolute()) {
                final Queue<QName> qnamedPath = xpathToQNamePath(context, module, strXPath);
                if (qnamedPath != null) {
                    final DataSchemaNode dataNode = findSchemaNodeForGivenPath(context, module, qnamedPath);
                    return dataNode;
                }
            }
        }
        return null;
    }

    /**
     * Method attempts to find DataSchemaNode inside of provided Schema Context and Yang Module accordingly to
     * Non-conditional relative Revision Aware XPath. The specified Module MUST be present in Schema Context otherwise
     * the operation would fail and return <code>null</code>.
     * <br>
     * The relative Revision Aware XPath MUST be specified WITHOUT the conditional statement (i.e. without [cond]) in
     * path, because in this state the Schema Context is completely unaware of data state and will be not able to
     * properly resolve XPath. If the XPath contains condition the method will return IllegalArgumentException.
     * <br>
     * The Actual Schema Node MUST be specified correctly because from this Schema Node will search starts. If the
     * Actual Schema Node is not correct the operation will simply fail, because it will be unable to find desired
     * DataSchemaNode.
     * <br>
     * In case that Schema Context or Module or Actual Schema Node or relative Revision Aware XPath contains
     * <code>null</code> references the method will throw IllegalArgumentException
     * <br>
     * If the Revision Aware XPath doesn't have flag <code>isAbsolute == false</code> the method will
     * throw IllegalArgumentException.
     * <br>
     * If the relative Revision Aware XPath is correct and desired Data Schema Node is present in Yang module or in
     * depending module in Schema Context the method will return specified Data Schema Node,
     * otherwise the operation will fail
     * and method will return <code>null</code>.
     *
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param module Yang Module
     * @param actualSchemaNode Actual Schema Node
     * @param relativeXPath Relative Non Conditional Revision Aware XPath
     * @return DataSchemaNode if is present in specified Schema Context for given relative Revision Aware XPath,
     * otherwise will return <code>null</code>.
     */
    public static DataSchemaNode findDataSchemaNodeForRelativeXPath(final SchemaContext context, final Module module,
            final SchemaNode actualSchemaNode, final RevisionAwareXPath relativeXPath) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        if (actualSchemaNode == null) {
            throw new IllegalArgumentException("Actual Schema Node reference cannot be NULL!");
        }
        if (relativeXPath == null) {
            throw new IllegalArgumentException("Non Conditional Revision Aware XPath cannot be NULL!");
        }
        if (relativeXPath.isAbsolute()) {
            throw new IllegalArgumentException("Revision Aware XPath MUST be relative i.e. MUST contains ../, "
                    + "for non relative Revision Aware XPath use findDataSchemaNode method!");
        }

        final SchemaPath actualNodePath = actualSchemaNode.getPath();
        if (actualNodePath != null) {
            final Queue<QName> qnamePath = resolveRelativeXPath(context, module, relativeXPath, actualNodePath);

            if (qnamePath != null) {
                final DataSchemaNode dataNode = findSchemaNodeForGivenPath(context, module, qnamePath);
                return dataNode;
            }
        }
        return null;
    }

    /**
     * Retrieve information from Schema Path and returns the module reference to which Schema Node belongs. The
     * search for correct Module is based on namespace within the last item in Schema Path. If schema context
     * contains module with namespace specified in last item of Schema Path, then operation will returns Module
     * reference, otherwise returns <code>null</code>
     * <br>
     * If Schema Context or Schema Node contains <code>null</code> references the method will throw IllegalArgumentException
     *
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param schemaPath Schema Path
     * @return Module reference for given Schema Path if module is present in Schema Context,
     * otherwise returns <code>null</code>
     */
    private static Module resolveModuleFromSchemaPath(final SchemaContext context, final SchemaPath schemaPath) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (schemaPath == null) {
            throw new IllegalArgumentException("Schema Path reference cannot be NULL");
        }

        final List<QName> path = schemaPath.getPath();
        if (!path.isEmpty()) {
            final QName qname = path.get(path.size() - 1);

            if ((qname != null) && (qname.getNamespace() != null)) {
                return context.findModuleByNamespace(qname.getNamespace());
            }
        }

        return null;
    }

    /**
     * Returns the Yang Module from specified Schema Context in which the TypeDefinition is declared. If the
     * TypeDefinition si not present in Schema Context then the method will return <code>null</code>
     *
     * If Schema Context or TypeDefinition contains <code>null</code> references the method will throw IllegalArgumentException
     *
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param type Type Definition
     * @return Yang Module in which the TypeDefinition is declared, if is not present, returns <code>null</code>.
     */
    public static Module findParentModuleForTypeDefinition(final SchemaContext context, final TypeDefinition<?> type) {
        final SchemaPath schemaPath = type.getPath();
        if (schemaPath == null) {
            throw new IllegalArgumentException("Schema Path reference cannot be NULL");
        }
        final List<QName> qnamedPath = schemaPath.getPath();
        if (qnamedPath == null || qnamedPath.isEmpty()) {
            throw new IllegalStateException("Schema Path contains invalid state of path parts."
                    + "The Schema Path MUST contain at least ONE QName which defines namespace and Local name"
                    + "of path.");
        }

        if (type instanceof ExtendedType) {
            final QName qname = qnamedPath.get(qnamedPath.size() - 1);
            if ((qname != null) && (qname.getNamespace() != null)) {
                return context.findModuleByNamespace(qname.getNamespace());
            }
        } else {
            final QName qname = qnamedPath.get(qnamedPath.size() - 2);
            if ((qname != null) && (qname.getNamespace() != null)) {
                return context.findModuleByNamespace(qname.getNamespace());
            }
        }
        return null;
    }

    /**
     * Returns parent Yang Module for specified Schema Context in which Schema Node is declared. If the Schema Node
     * is not present in Schema Context the operation will return <code>null</code>.
     * <br>
     * If Schema Context or Schema Node contains <code>null</code> references the method will throw IllegalArgumentException
     *
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param schemaNode Schema Node
     * @return Yang Module for specified Schema Context and Schema Node, if Schema Node is NOT present,
     * the method will returns <code>null</code>
     */
    public static Module findParentModule(final SchemaContext context, final SchemaNode schemaNode) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (schemaNode == null) {
            throw new IllegalArgumentException("Schema Node cannot be NULL!");
        }

        final SchemaPath schemaPath = schemaNode.getPath();
        if (schemaPath == null) {
            throw new IllegalStateException("Schema Path for Schema Node is not "
                    + "set properly (Schema Path is NULL)");
        }
        final List<QName> qnamedPath = schemaPath.getPath();
        if (qnamedPath == null || qnamedPath.isEmpty()) {
            throw new IllegalStateException("Schema Path contains invalid state of path parts."
                    + "The Schema Path MUST contain at least ONE QName which defines namespace and Local name"
                    + "of path.");
        }
        final QName qname = qnamedPath.get(qnamedPath.size() - 1);
        return context.findModuleByNamespace(qname.getNamespace());
    }

    /**
     * Method will attempt to find DataSchemaNode from specified Module and Queue of QNames through the Schema
     * Context. The QNamed path could be defined across multiple modules in Schema Context so the method is called
     * recursively. If the QNamed path contains QNames that are not part of any Module or Schema Context Path the
     * operation will fail and returns <code>null</code>
     * <br>
     * If Schema Context, Module or Queue of QNames refers to <code>null</code> values,
     * the method will throws IllegalArgumentException
     *
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param module Yang Module
     * @param qnamedPath Queue of QNames
     * @return DataSchemaNode if is present in Module(s) for specified Schema Context and given QNamed Path,
     * otherwise will return <code>null</code>.
     */
    private static DataSchemaNode findSchemaNodeForGivenPath(final SchemaContext context, final Module module,
            final Queue<QName> qnamedPath) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        if (module.getNamespace() == null) {
            throw new IllegalArgumentException("Namespace for Module cannot contains NULL reference!");
        }
        if (qnamedPath == null || qnamedPath.isEmpty()) {
            throw new IllegalStateException("Schema Path contains invalid state of path parts."
                    + "The Schema Path MUST contain at least ONE QName which defines namespace and Local name"
                    + "of path.");
        }

        DataNodeContainer nextNode = module;
        final URI moduleNamespace = module.getNamespace();

        QName childNodeQName;
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
                    } else if (schemaNode instanceof ChoiceNode) {
                        final ChoiceNode choice = (ChoiceNode) schemaNode;
                        qnamedPath.poll();
                        if (!qnamedPath.isEmpty()) {
                            childNodeQName = qnamedPath.peek();
                            nextNode = choice.getCaseNodeByName(childNodeQName);
                            schemaNode = (DataSchemaNode) nextNode;
                        }
                    } else {
                        nextNode = null;
                    }
                } else if (!childNodeNamespace.equals(moduleNamespace)) {
                    final Module nextModule = context.findModuleByNamespace(childNodeNamespace);
                    schemaNode = findSchemaNodeForGivenPath(context, nextModule, qnamedPath);
                    return schemaNode;
                }
                qnamedPath.poll();
            }
        }
        return schemaNode;
    }

    /**
     * Transforms string representation of XPath to Queue of QNames. The XPath is split by "/" and for each part of
     * XPath is assigned correct module in Schema Path.
     * <br>
     * If Schema Context, Parent Module or XPath string contains <code>null</code> values,
     * the method will throws IllegalArgumentException
     *
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param parentModule Parent Module
     * @param xpath XPath String
     * @return
     */
    private static Queue<QName> xpathToQNamePath(final SchemaContext context, final Module parentModule,
            final String xpath) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (parentModule == null) {
            throw new IllegalArgumentException("Parent Module reference cannot be NULL!");
        }
        if (xpath == null) {
            throw new IllegalArgumentException("XPath string reference cannot be NULL!");
        }

        final Queue<QName> path = new LinkedList<>();
        final String[] prefixedPath = xpath.split("/");
        for (int i = 0; i < prefixedPath.length; ++i) {
            if (!prefixedPath[i].isEmpty()) {
                path.add(stringPathPartToQName(context, parentModule, prefixedPath[i]));
            }
        }
        return path;
    }

    /**
     * Transforms part of Prefixed Path as java String to QName.
     * <br>
     * If the string contains module prefix separated by ":" (i.e. mod:container) this module is provided from from
     * Parent Module list of imports. If the Prefixed module is present in Schema Context the QName can be
     * constructed.
     * <br>
     * If the Prefixed Path Part does not contains prefix the Parent's Module namespace is taken for construction of
     * QName.
     * <br>
     * If Schema Context, Parent Module or Prefixed Path Part refers to <code>null</code> the method will throw
     * IllegalArgumentException
     *
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param parentModule Parent Module
     * @param prefixedPathPart Prefixed Path Part string
     * @return QName from prefixed Path Part String.
     */
    private static QName stringPathPartToQName(final SchemaContext context, final Module parentModule,
            final String prefixedPathPart) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (parentModule == null) {
            throw new IllegalArgumentException("Parent Module reference cannot be NULL!");
        }
        if (prefixedPathPart == null) {
            throw new IllegalArgumentException("Prefixed Path Part cannot be NULL!");
        }

        if (prefixedPathPart.contains(":")) {
            final String[] prefixedName = prefixedPathPart.split(":");
            final Module module = resolveModuleForPrefix(context, parentModule, prefixedName[0]);
            if (module != null) {
                return new QName(module.getNamespace(), module.getRevision(), prefixedName[1]);
            }
        } else {
            return new QName(parentModule.getNamespace(), parentModule.getRevision(), prefixedPathPart);
        }
        return null;
    }

    /**
     * Method will attempt to resolve and provide Module reference for specified module prefix. Each Yang module
     * could contains multiple imports which MUST be associated with corresponding module prefix. The method simply
     * looks into module imports and returns the module that is bounded with specified prefix. If the prefix is not
     * present in module or the prefixed module is not present in specified Schema Context,
     * the method will return <code>null</code>.
     * <br>
     * If String prefix is the same as prefix of the specified Module the reference to this module is returned.
     * <br>
     * If Schema Context, Module or Prefix are referring to <code>null</code> the method will return
     * IllegalArgumentException
     *
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param module Yang Module
     * @param prefix Module Prefix
     * @return Module for given prefix in specified Schema Context if is present, otherwise returns <code>null</code>
     */
    private static Module resolveModuleForPrefix(final SchemaContext context, final Module module, final String prefix) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix string cannot be NULL!");
        }

        if (prefix.equals(module.getPrefix())) {
            return module;
        }

        final Set<ModuleImport> imports = module.getImports();
        for (final ModuleImport mi : imports) {
            if (prefix.equals(mi.getPrefix())) {
                return context.findModuleByName(mi.getModuleName(), mi.getRevision());
            }
        }
        return null;
    }

    /**
     * @throws IllegalArgumentException
     *
     * @param context Schema Context
     * @param module Yang Module
     * @param relativeXPath Non conditional Revision Aware Relative XPath
     * @param leafrefSchemaPath Schema Path for Leafref
     * @return
     */
    private static Queue<QName> resolveRelativeXPath(final SchemaContext context, final Module module,
            final RevisionAwareXPath relativeXPath, final SchemaPath leafrefSchemaPath) {
        final Queue<QName> absolutePath = new LinkedList<>();
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        if (relativeXPath == null) {
            throw new IllegalArgumentException("Non Conditional Revision Aware XPath cannot be NULL!");
        }
        if (relativeXPath.isAbsolute()) {
            throw new IllegalArgumentException("Revision Aware XPath MUST be relative i.e. MUST contains ../, "
                    + "for non relative Revision Aware XPath use findDataSchemaNode method!");
        }
        if (leafrefSchemaPath == null) {
            throw new IllegalArgumentException("Schema Path reference for Leafref cannot be NULL!");
        }

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
        return absolutePath;
    }
}
