/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.impl;

import static org.opendaylight.controller.yang.parser.util.ParserListenerUtils.*;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.tree.ParseTree;
import org.opendaylight.controller.antlrv4.code.gen.*;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Argument_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Base_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Contact_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Container_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Default_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Description_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Deviate_add_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Deviate_delete_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Deviate_not_supported_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Deviate_replace_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Import_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Key_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Leaf_list_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Leaf_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.List_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_header_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Namespace_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Ordered_by_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Organization_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Prefix_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Presence_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Reference_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_date_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Type_body_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Units_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.When_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Yang_version_stmtContext;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.util.YangTypesConverter;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.AnyXmlBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceCaseBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.DeviationBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ExtensionBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.FeatureBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.IdentitySchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.LeafListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.LeafSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.NotificationBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.RpcDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.UnionTypeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.util.RefineHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YangParserListenerImpl extends YangParserBaseListener {
    private static final Logger logger = LoggerFactory.getLogger(YangParserListenerImpl.class);

    private ModuleBuilder moduleBuilder;
    private String moduleName;
    private URI namespace;
    private String yangModelPrefix;
    private Date revision = new Date(0L);

    public final static DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final Stack<String> actualPath = new Stack<String>();

    @Override
    public void enterModule_stmt(YangParser.Module_stmtContext ctx) {
        moduleName = stringFromNode(ctx);
        logger.debug("enter module " + moduleName);
        actualPath.push(moduleName);
        moduleBuilder = new ModuleBuilder(moduleName);

        String description = null;
        String reference = null;

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            } else {
                if (description != null && reference != null) {
                    break;
                }
            }
        }
        moduleBuilder.setDescription(description);
        moduleBuilder.setReference(reference);
    }

    @Override
    public void exitModule_stmt(YangParser.Module_stmtContext ctx) {
        exitLog("module", actualPath.pop());
    }

    @Override
    public void enterModule_header_stmts(Module_header_stmtsContext ctx) {
        enterLog("module_header", "", ctx.getStart().getLine());
        String yangVersion = null;
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree treeNode = ctx.getChild(i);
            if (treeNode instanceof Namespace_stmtContext) {
                final String namespaceStr = stringFromNode(treeNode);
                namespace = URI.create(namespaceStr);
                moduleBuilder.setNamespace(namespace);
                setLog("namespace", namespaceStr);
            } else if (treeNode instanceof Prefix_stmtContext) {
                yangModelPrefix = stringFromNode(treeNode);
                moduleBuilder.setPrefix(yangModelPrefix);
                setLog("prefix", yangModelPrefix);
            } else if (treeNode instanceof Yang_version_stmtContext) {
                yangVersion = stringFromNode(treeNode);
                setLog("yang-version", yangVersion);
            }
        }

        if (yangVersion == null) {
            yangVersion = "1";
        }
        moduleBuilder.setYangVersion(yangVersion);
    }

    @Override
    public void exitModule_header_stmts(Module_header_stmtsContext ctx) {
        exitLog("module_header", "");
    }

    @Override
    public void enterMeta_stmts(YangParser.Meta_stmtsContext ctx) {
        enterLog("meta_stmt", "", ctx.getStart().getLine());
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Organization_stmtContext) {
                final String organization = stringFromNode(child);
                moduleBuilder.setOrganization(organization);
                setLog("organization", organization);
            } else if (child instanceof Contact_stmtContext) {
                final String contact = stringFromNode(child);
                moduleBuilder.setContact(contact);
                setLog("contact", contact);
            } else if (child instanceof Description_stmtContext) {
                final String description = stringFromNode(child);
                moduleBuilder.setDescription(description);
                setLog("description", description);
            } else if (child instanceof Reference_stmtContext) {
                final String reference = stringFromNode(child);
                moduleBuilder.setReference(reference);
                setLog("reference", reference);
            }
        }
    }

    @Override
    public void exitMeta_stmts(YangParser.Meta_stmtsContext ctx) {
        exitLog("meta_stmt", "");
    }

    @Override
    public void enterRevision_stmts(Revision_stmtsContext ctx) {
        enterLog("revisions", "", ctx.getStart().getLine());
        if (ctx != null) {
            for (int i = 0; i < ctx.getChildCount(); ++i) {
                final ParseTree treeNode = ctx.getChild(i);
                if (treeNode instanceof Revision_stmtContext) {
                    updateRevisionForRevisionStatement(treeNode);
                }
            }
        }
    }

    @Override
    public void exitRevision_stmts(Revision_stmtsContext ctx) {
        exitLog("revisions", "");
    }

    private void updateRevisionForRevisionStatement(final ParseTree treeNode) {
        final String revisionDateStr = stringFromNode(treeNode);
        try {
            final Date revision = simpleDateFormat.parse(revisionDateStr);
            if ((revision != null) && (this.revision.compareTo(revision) < 0)) {
                this.revision = revision;
                moduleBuilder.setRevision(this.revision);
                setLog("revision", this.revision.toString());
                for (int i = 0; i < treeNode.getChildCount(); ++i) {
                    ParseTree child = treeNode.getChild(i);
                    if (child instanceof Reference_stmtContext) {
                        moduleBuilder.setReference(stringFromNode(child));
                    }
                }
            }
        } catch (ParseException e) {
            final String message = "Failed to parse revision string: " + revisionDateStr;
            logger.warn(message);
        }
    }

    @Override
    public void enterImport_stmt(Import_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String importName = stringFromNode(ctx);
        enterLog("import", importName, line);

        String importPrefix = null;
        Date importRevision = null;

        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree treeNode = ctx.getChild(i);
            if (treeNode instanceof Prefix_stmtContext) {
                importPrefix = stringFromNode(treeNode);
            }
            if (treeNode instanceof Revision_date_stmtContext) {
                String importRevisionStr = stringFromNode(treeNode);
                try {
                    importRevision = simpleDateFormat.parse(importRevisionStr);
                } catch (ParseException e) {
                    logger.warn("Failed to parse import revision-date at line " + line + ": " + importRevisionStr);
                }
            }
        }
        moduleBuilder.addModuleImport(importName, importRevision, importPrefix);
        setLog("import", "(" + importName + "; " + importRevision + "; " + importPrefix + ")");
    }

    @Override
    public void exitImport_stmt(Import_stmtContext ctx) {
        exitLog("import", "");
    }

    @Override
    public void enterAugment_stmt(YangParser.Augment_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String augmentPath = stringFromNode(ctx);
        enterLog("augment", augmentPath, line);

        AugmentationSchemaBuilder builder = moduleBuilder.addAugment(line, augmentPath);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Description_stmtContext) {
                builder.setDescription(stringFromNode(child));
            } else if (child instanceof Reference_stmtContext) {
                builder.setReference(stringFromNode(child));
            } else if (child instanceof Status_stmtContext) {
                builder.setStatus(parseStatus((Status_stmtContext) child));
            } else if (child instanceof When_stmtContext) {
                builder.addWhenCondition(stringFromNode(child));
            }
        }

        moduleBuilder.enterNode(builder);
        actualPath.push(augmentPath);
    }

    @Override
    public void exitAugment_stmt(YangParser.Augment_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("augment", actualPath.pop());
    }

    @Override
    public void enterExtension_stmt(YangParser.Extension_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String extName = stringFromNode(ctx);
        enterLog("extension", extName, line);

        QName qname = new QName(namespace, revision, yangModelPrefix, extName);
        ExtensionBuilder builder = moduleBuilder.addExtension(qname, line);
        parseSchemaNodeArgs(ctx, builder);

        String argument = null;
        boolean yin = false;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Argument_stmtContext) {
                argument = stringFromNode(child);
                yin = parseYinValue((Argument_stmtContext) child);
                break;
            }
        }
        builder.setArgument(argument);
        builder.setYinElement(yin);

        moduleBuilder.enterNode(builder);
        actualPath.push(extName);
    }

    @Override
    public void exitExtension_stmt(YangParser.Extension_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("extension", actualPath.pop());
    }

    @Override
    public void enterTypedef_stmt(YangParser.Typedef_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String typedefName = stringFromNode(ctx);
        enterLog("typedef", typedefName, line);

        QName typedefQName = new QName(namespace, revision, yangModelPrefix, typedefName);
        TypeDefinitionBuilder builder = moduleBuilder.addTypedef(line, typedefQName);
        moduleBuilder.enterNode(builder);
        actualPath.push(typedefName);

        builder.setPath(createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, builder);
        builder.setUnits(parseUnits(ctx));
        builder.setDefaultValue(parseDefault(ctx));
    }

    @Override
    public void exitTypedef_stmt(YangParser.Typedef_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("typedef", actualPath.pop());
    }

    @Override
    public void enterType_stmt(YangParser.Type_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String typeName = stringFromNode(ctx);
        enterLog("type", typeName, line);

        final QName typeQName = parseQName(typeName);

        TypeDefinition<?> type = null;
        Type_body_stmtsContext typeBody = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof Type_body_stmtsContext) {
                typeBody = (Type_body_stmtsContext) ctx.getChild(i);
                break;
            }
        }

        // if this is base yang type...
        if (YangTypesConverter.isBaseYangType(typeName)) {
            if (typeBody == null) {
                // check for types which must have body
                checkMissingBody(typeName, moduleName, line);
                // if there are no constraints, just grab default base yang type
                type = YangTypesConverter.javaTypeForBaseYangType(actualPath, namespace, revision, typeName);
                moduleBuilder.setType(type);
            } else {
                if ("union".equals(typeName)) {
                    SchemaPath p = createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix, typeName);
                    UnionTypeBuilder unionBuilder = moduleBuilder.addUnionType(actualPath, namespace, revision, line);
                    moduleBuilder.enterNode(unionBuilder);
                    unionBuilder.setPath(p);
                } else if ("identityref".equals(typeName)) {
                    SchemaPath path = createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix, typeName);
                    moduleBuilder.addIdentityrefType(getIdentityrefBase(typeBody), actualPath, path, line);
                } else {
                    type = parseTypeWithBody(moduleName, typeName, typeBody, actualPath, namespace, revision,
                            yangModelPrefix, moduleBuilder.getActualNode());
                    moduleBuilder.setType(type);
                }
            }
        } else {
            type = parseUnknownTypeWithBody(typeQName, typeBody, actualPath, namespace, revision, yangModelPrefix,
                    moduleBuilder.getActualNode());
            // add parent node of this type statement to dirty nodes
            moduleBuilder.addDirtyNode(actualPath);
            moduleBuilder.setType(type);
        }

        actualPath.push(typeName);
    }

    private QName parseQName(String typeName) {
        QName typeQName;
        if (typeName.contains(":")) {
            String[] splittedName = typeName.split(":");
            String prefix = splittedName[0];
            String name = splittedName[1];
            if (prefix.equals(yangModelPrefix)) {
                typeQName = new QName(namespace, revision, prefix, name);
            } else {
                typeQName = new QName(null, null, prefix, name);
            }
        } else {
            typeQName = new QName(namespace, revision, yangModelPrefix, typeName);
        }
        return typeQName;
    }

    @Override
    public void exitType_stmt(YangParser.Type_stmtContext ctx) {
        final String typeName = stringFromNode(ctx);
        if ("union".equals(typeName)) {
            moduleBuilder.exitNode();
        }
        exitLog("type", actualPath.pop());
    }

    @Override
    public void enterGrouping_stmt(YangParser.Grouping_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String groupName = stringFromNode(ctx);
        enterLog("grouping", groupName, line);

        QName groupQName = new QName(namespace, revision, yangModelPrefix, groupName);
        GroupingBuilder builder = moduleBuilder.addGrouping(ctx.getStart().getLine(), groupQName);
        moduleBuilder.enterNode(builder);
        actualPath.push(groupName);

        builder.setPath(createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, builder);
    }

    @Override
    public void exitGrouping_stmt(YangParser.Grouping_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("grouping", actualPath.pop());
    }

    @Override
    public void enterContainer_stmt(Container_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String containerName = stringFromNode(ctx);
        enterLog("container", containerName, line);

        QName containerQName = new QName(namespace, revision, yangModelPrefix, containerName);
        SchemaPath path = createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix, containerName);

        ContainerSchemaNodeBuilder builder = moduleBuilder.addContainerNode(line, containerQName, path);
        moduleBuilder.enterNode(builder);
        actualPath.push(containerName);

        parseSchemaNodeArgs(ctx, builder);
        parseConstraints(ctx, builder.getConstraints());
        builder.setConfiguration(getConfig(ctx, moduleBuilder.getActualParent(), moduleName, line));

        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree childNode = ctx.getChild(i);
            if (childNode instanceof Presence_stmtContext) {
                builder.setPresence(true);
                break;
            }
        }
    }

    @Override
    public void exitContainer_stmt(Container_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("container", actualPath.pop());
    }

    @Override
    public void enterLeaf_stmt(Leaf_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String leafName = stringFromNode(ctx);
        enterLog("leaf", leafName, line);

        QName leafQName = new QName(namespace, revision, yangModelPrefix, leafName);
        SchemaPath schemaPath = createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix, leafName);

        LeafSchemaNodeBuilder builder = moduleBuilder.addLeafNode(line, leafQName, schemaPath);
        moduleBuilder.enterNode(builder);
        actualPath.push(leafName);

        parseSchemaNodeArgs(ctx, builder);
        parseConstraints(ctx, builder.getConstraints());
        builder.setConfiguration(getConfig(ctx, moduleBuilder.getActualParent(), moduleName, line));

        String defaultStr = null;
        String unitsStr = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Default_stmtContext) {
                defaultStr = stringFromNode(child);
            } else if (child instanceof Units_stmtContext) {
                unitsStr = stringFromNode(child);
            }
        }
        builder.setDefaultStr(defaultStr);
        builder.setUnits(unitsStr);
    }

    @Override
    public void exitLeaf_stmt(YangParser.Leaf_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("leaf", actualPath.pop());
    }

    @Override
    public void enterUses_stmt(YangParser.Uses_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String groupingPathStr = stringFromNode(ctx);
        enterLog("uses", groupingPathStr, line);

        UsesNodeBuilder builder = moduleBuilder.addUsesNode(line, groupingPathStr);

        moduleBuilder.enterNode(builder);
        actualPath.push(groupingPathStr);
    }

    @Override
    public void exitUses_stmt(YangParser.Uses_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("uses", actualPath.pop());
    }

    @Override
    public void enterRefine_stmt(YangParser.Refine_stmtContext ctx) {
        final String refineString = stringFromNode(ctx);
        enterLog("refine", refineString, ctx.getStart().getLine());

        RefineHolder refine = parseRefine(ctx);
        moduleBuilder.addRefine(refine, actualPath);
        moduleBuilder.enterNode(refine);
        actualPath.push(refineString);
    }

    @Override
    public void exitRefine_stmt(YangParser.Refine_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("refine", actualPath.pop());
    }

    @Override
    public void enterLeaf_list_stmt(Leaf_list_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String leafListName = stringFromNode(ctx);
        enterLog("leaf-list", leafListName, line);

        QName leafListQName = new QName(namespace, revision, yangModelPrefix, leafListName);
        SchemaPath schemaPath = createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix, leafListName);

        LeafListSchemaNodeBuilder builder = moduleBuilder.addLeafListNode(line, leafListQName, schemaPath);
        moduleBuilder.enterNode(builder);
        actualPath.push(leafListName);

        parseSchemaNodeArgs(ctx, builder);
        parseConstraints(ctx, builder.getConstraints());
        builder.setConfiguration(getConfig(ctx, moduleBuilder.getActualParent(), moduleName, ctx.getStart().getLine()));

        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree childNode = ctx.getChild(i);
            if (childNode instanceof Ordered_by_stmtContext) {
                final Ordered_by_stmtContext orderedBy = (Ordered_by_stmtContext) childNode;
                final boolean userOrdered = parseUserOrdered(orderedBy);
                builder.setUserOrdered(userOrdered);
                break;
            }
        }
    }

    @Override
    public void exitLeaf_list_stmt(YangParser.Leaf_list_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("leaf-list", actualPath.pop());
    }

    @Override
    public void enterList_stmt(List_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String listName = stringFromNode(ctx);
        enterLog("list", listName, line);

        QName listQName = new QName(namespace, revision, yangModelPrefix, listName);
        SchemaPath schemaPath = createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix, listName);

        ListSchemaNodeBuilder builder = moduleBuilder.addListNode(line, listQName, schemaPath);
        moduleBuilder.enterNode(builder);
        actualPath.push(listName);

        parseSchemaNodeArgs(ctx, builder);
        parseConstraints(ctx, builder.getConstraints());
        builder.setConfiguration(getConfig(ctx, moduleBuilder.getActualParent(), moduleName, line));

        String keyDefinition = "";
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            ParseTree childNode = ctx.getChild(i);
            if (childNode instanceof Ordered_by_stmtContext) {
                final Ordered_by_stmtContext orderedBy = (Ordered_by_stmtContext) childNode;
                final boolean userOrdered = parseUserOrdered(orderedBy);
                builder.setUserOrdered(userOrdered);
            } else if (childNode instanceof Key_stmtContext) {
                keyDefinition = stringFromNode(childNode);
                List<QName> key = createListKey(keyDefinition, namespace, revision, yangModelPrefix);
                builder.setKeyDefinition(key);
            }
        }
    }

    @Override
    public void exitList_stmt(List_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("list", actualPath.pop());
    }

    @Override
    public void enterAnyxml_stmt(YangParser.Anyxml_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String anyXmlName = stringFromNode(ctx);
        enterLog("anyxml", anyXmlName, line);

        QName anyXmlQName = new QName(namespace, revision, yangModelPrefix, anyXmlName);
        SchemaPath schemaPath = createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix, anyXmlName);

        AnyXmlBuilder builder = moduleBuilder.addAnyXml(line, anyXmlQName, schemaPath);
        moduleBuilder.enterNode(builder);
        actualPath.push(anyXmlName);

        parseSchemaNodeArgs(ctx, builder);
        parseConstraints(ctx, builder.getConstraints());
        builder.setConfiguration(getConfig(ctx, moduleBuilder.getActualParent(), moduleName, line));
    }

    @Override
    public void exitAnyxml_stmt(YangParser.Anyxml_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("anyxml", actualPath.pop());
    }

    @Override
    public void enterChoice_stmt(YangParser.Choice_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String choiceName = stringFromNode(ctx);
        enterLog("choice", choiceName, line);

        QName choiceQName = new QName(namespace, revision, yangModelPrefix, choiceName);

        ChoiceBuilder builder = moduleBuilder.addChoice(line, choiceQName);
        moduleBuilder.enterNode(builder);
        actualPath.push(choiceName);

        builder.setPath(createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, builder);
        parseConstraints(ctx, builder.getConstraints());
        builder.setConfiguration(getConfig(ctx, moduleBuilder.getActualParent(), moduleName, line));

        // set 'default' case
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Default_stmtContext) {
                String defaultCase = stringFromNode(child);
                builder.setDefaultCase(defaultCase);
                break;
            }
        }
    }

    @Override
    public void exitChoice_stmt(YangParser.Choice_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("choice", actualPath.pop());
    }

    @Override
    public void enterCase_stmt(YangParser.Case_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String caseName = stringFromNode(ctx);
        enterLog("case", caseName, line);

        QName caseQName = new QName(namespace, revision, yangModelPrefix, caseName);
        ChoiceCaseBuilder builder = moduleBuilder.addCase(line, caseQName);
        moduleBuilder.enterNode(builder);
        actualPath.push(caseName);

        builder.setPath(createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, builder);
        parseConstraints(ctx, builder.getConstraints());
    }

    @Override
    public void exitCase_stmt(YangParser.Case_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("case", actualPath.pop());
    }

    @Override
    public void enterNotification_stmt(YangParser.Notification_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String notificationName = stringFromNode(ctx);
        enterLog("notification", notificationName, line);

        QName notificationQName = new QName(namespace, revision, yangModelPrefix, notificationName);
        NotificationBuilder builder = moduleBuilder.addNotification(notificationQName, actualPath, line);
        moduleBuilder.enterNode(builder);
        actualPath.push(notificationName);

        builder.setPath(createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, builder);
    }

    @Override
    public void exitNotification_stmt(YangParser.Notification_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("notification", actualPath.pop());
    }

    // Unknown nodes
    @Override
    public void enterIdentifier_stmt(YangParser.Identifier_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String nodeParameter = stringFromNode(ctx);
        enterLog("unknown-node", nodeParameter, line);

        QName nodeType = null;

        final String nodeTypeStr = ctx.getChild(0).getText();
        final String[] splittedElement = nodeTypeStr.split(":");
        if (splittedElement.length == 1) {
            nodeType = new QName(null, null, yangModelPrefix, splittedElement[0]);
        } else {
            nodeType = new QName(null, null, splittedElement[0], splittedElement[1]);
        }

        QName qname;
        if (nodeParameter != null) {
            String[] splittedName = nodeParameter.split(":");
            if (splittedName.length == 2) {
                qname = new QName(null, null, splittedName[0], splittedName[1]);
            } else {
                qname = new QName(namespace, revision, yangModelPrefix, splittedName[0]);
            }
        } else {
            qname = new QName(namespace, revision, yangModelPrefix, nodeParameter);
        }

        UnknownSchemaNodeBuilder builder = moduleBuilder.addUnknownSchemaNode(qname, actualPath, line);
        builder.setNodeType(nodeType);
        builder.setNodeParameter(nodeParameter);
        actualPath.push(nodeParameter);
        builder.setPath(createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, builder);
        moduleBuilder.enterNode(builder);
    }

    @Override
    public void exitIdentifier_stmt(YangParser.Identifier_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("unknown-node", actualPath.pop());
    }

    @Override
    public void enterRpc_stmt(YangParser.Rpc_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String rpcName = stringFromNode(ctx);
        enterLog("rpc", rpcName, line);

        QName rpcQName = new QName(namespace, revision, yangModelPrefix, rpcName);
        RpcDefinitionBuilder rpcBuilder = moduleBuilder.addRpc(line, rpcQName);
        moduleBuilder.enterNode(rpcBuilder);
        actualPath.push(rpcName);

        rpcBuilder.setPath(createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, rpcBuilder);
    }

    @Override
    public void exitRpc_stmt(YangParser.Rpc_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("rpc", actualPath.pop());
    }

    @Override
    public void enterInput_stmt(YangParser.Input_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String input = "input";
        enterLog(input, input, line);

        QName rpcQName = new QName(namespace, revision, yangModelPrefix, input);
        SchemaPath path = createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix, input);

        ContainerSchemaNodeBuilder builder = moduleBuilder.addRpcInput(path, rpcQName, line);
        moduleBuilder.enterNode(builder);
        actualPath.push(input);

        parseSchemaNodeArgs(ctx, builder);
        parseConstraints(ctx, builder.getConstraints());
    }

    @Override
    public void exitInput_stmt(YangParser.Input_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("input", actualPath.pop());
    }

    @Override
    public void enterOutput_stmt(YangParser.Output_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String output = "output";
        enterLog(output, output, line);

        QName rpcQName = new QName(namespace, revision, yangModelPrefix, output);
        SchemaPath path = createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix, output);

        ContainerSchemaNodeBuilder builder = moduleBuilder.addRpcOutput(path, rpcQName, line);
        moduleBuilder.enterNode(builder);
        actualPath.push(output);

        parseSchemaNodeArgs(ctx, builder);
        parseConstraints(ctx, builder.getConstraints());
    }

    @Override
    public void exitOutput_stmt(YangParser.Output_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("output", actualPath.pop());
    }

    @Override
    public void enterFeature_stmt(YangParser.Feature_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String featureName = stringFromNode(ctx);
        enterLog("feature", featureName, line);

        QName featureQName = new QName(namespace, revision, yangModelPrefix, featureName);
        FeatureBuilder featureBuilder = moduleBuilder.addFeature(line, featureQName);
        moduleBuilder.enterNode(featureBuilder);
        actualPath.push(featureName);

        featureBuilder.setPath(createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, featureBuilder);
    }

    @Override
    public void exitFeature_stmt(YangParser.Feature_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("feature", actualPath.pop());
    }

    @Override
    public void enterDeviation_stmt(YangParser.Deviation_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String targetPath = stringFromNode(ctx);
        enterLog("deviation", targetPath, line);

        String reference = null;
        String deviate = null;
        DeviationBuilder builder = moduleBuilder.addDeviation(line, targetPath);
        moduleBuilder.enterNode(builder);
        actualPath.push(targetPath);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            } else if (child instanceof Deviate_not_supported_stmtContext) {
                deviate = stringFromNode(child);
            } else if (child instanceof Deviate_add_stmtContext) {
                deviate = stringFromNode(child);
            } else if (child instanceof Deviate_replace_stmtContext) {
                deviate = stringFromNode(child);
            } else if (child instanceof Deviate_delete_stmtContext) {
                deviate = stringFromNode(child);
            }
        }
        builder.setReference(reference);
        builder.setDeviate(deviate);
    }

    @Override
    public void exitDeviation_stmt(YangParser.Deviation_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("deviation", actualPath.pop());
    }

    @Override
    public void enterIdentity_stmt(YangParser.Identity_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        final String identityName = stringFromNode(ctx);
        enterLog("identity", identityName, line);

        final QName identityQName = new QName(namespace, revision, yangModelPrefix, identityName);
        IdentitySchemaNodeBuilder builder = moduleBuilder.addIdentity(identityQName, actualPath, line);
        moduleBuilder.enterNode(builder);
        actualPath.push(identityName);

        builder.setPath(createActualSchemaPath(actualPath, namespace, revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, builder);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Base_stmtContext) {
                String baseIdentityName = stringFromNode(child);
                builder.setBaseIdentityName(baseIdentityName);
            }
        }
    }

    @Override
    public void exitIdentity_stmt(YangParser.Identity_stmtContext ctx) {
        moduleBuilder.exitNode();
        exitLog("identity", actualPath.pop());
    }

    public ModuleBuilder getModuleBuilder() {
        return moduleBuilder;
    }

    private void enterLog(String p1, String p2, int line) {
        logger.debug("entering " + p1 + " " + p2 + " (" + line + ")");
    }

    private void exitLog(String p1, String p2) {
        logger.debug("exiting " + p1 + " " + p2);
    }

    private void setLog(String p1, String p2) {
        logger.debug("setting " + p1 + " " + p2);
    }

}
