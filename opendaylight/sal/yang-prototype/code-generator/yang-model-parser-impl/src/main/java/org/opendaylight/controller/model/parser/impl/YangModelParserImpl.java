/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.impl;

import static org.opendaylight.controller.model.parser.util.YangModelBuilderHelper.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.tree.ParseTree;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.antlrv4.code.gen.YangParserBaseListener;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Config_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Config_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Container_stmtContext;
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
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Ordered_by_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Ordered_by_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Prefix_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Presence_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Reference_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_date_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.StringContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Type_body_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Yang_version_stmtContext;
import org.opendaylight.controller.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.model.api.type.LengthConstraint;
import org.opendaylight.controller.model.api.type.PatternConstraint;
import org.opendaylight.controller.model.api.type.RangeConstraint;
import org.opendaylight.controller.model.parser.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.model.parser.api.GroupingBuilder;
import org.opendaylight.controller.model.parser.builder.ContainerSchemaNodeBuilder;
import org.opendaylight.controller.model.parser.builder.DeviationBuilder;
import org.opendaylight.controller.model.parser.builder.FeatureBuilder;
import org.opendaylight.controller.model.parser.builder.LeafListSchemaNodeBuilder;
import org.opendaylight.controller.model.parser.builder.LeafSchemaNodeBuilder;
import org.opendaylight.controller.model.parser.builder.ListSchemaNodeBuilder;
import org.opendaylight.controller.model.parser.builder.ModuleBuilder;
import org.opendaylight.controller.model.parser.builder.MustDefinitionBuilder;
import org.opendaylight.controller.model.parser.builder.NotificationBuilder;
import org.opendaylight.controller.model.parser.builder.RpcDefinitionBuilder;
import org.opendaylight.controller.model.parser.builder.TypedefBuilder;
import org.opendaylight.controller.model.parser.util.YangModelBuilderHelper;
import org.opendaylight.controller.model.util.BitsType;
import org.opendaylight.controller.model.util.EnumerationType;
import org.opendaylight.controller.model.util.Leafref;
import org.opendaylight.controller.model.util.StringType;
import org.opendaylight.controller.model.util.YangTypesConverter;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModelParserImpl extends YangParserBaseListener {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelParserImpl.class);

    private ModuleBuilder moduleBuilder;

    private String moduleName;
    private URI namespace;
    private String yangModelPrefix;
    private Date revision;

    private final DateFormat simpleDateFormat = new SimpleDateFormat(
            "yyyy-mm-dd");
    private final Stack<String> actualPath = new Stack<String>();

    @Override
    public void enterModule_stmt(YangParser.Module_stmtContext ctx) {
        moduleName = stringFromNode(ctx);
        actualPath.push(moduleName);
        moduleBuilder = new ModuleBuilder(moduleName);
    }

    @Override
    public void exitModule_stmt(YangParser.Module_stmtContext ctx) {
        final String moduleName = actualPath.pop();
        logger.debug("Exiting module " + moduleName);
    }

    @Override
    public void enterModule_header_stmts(final Module_header_stmtsContext ctx) {
        super.enterModule_header_stmts(ctx);

        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree treeNode = ctx.getChild(i);
            if (treeNode instanceof Namespace_stmtContext) {
                String namespaceStr = stringFromNode(treeNode);
                try {
                    this.namespace = new URI(namespaceStr);
                    moduleBuilder.setNamespace(namespace);
                } catch (URISyntaxException e) {
                    logger.warn("Failed to parse module namespace", e);
                }
            } else if (treeNode instanceof Prefix_stmtContext) {
                yangModelPrefix = stringFromNode(treeNode);
                moduleBuilder.setPrefix(yangModelPrefix);
            } else if (treeNode instanceof Yang_version_stmtContext) {
                final String yangVersion = stringFromNode(treeNode);
                moduleBuilder.setYangVersion(yangVersion);
            }
        }
    }

    // TODO: resolve submodule parsing
    @Override
    public void enterSubmodule_header_stmts(
            YangParser.Submodule_header_stmtsContext ctx) {
        String submoduleName = stringFromNode(ctx);
        QName submoduleQName = new QName(namespace, revision, yangModelPrefix,
                submoduleName);
        moduleBuilder.addSubmodule(submoduleQName);
        updatePath(submoduleName);
    }

    @Override
    public void exitSubmodule_header_stmts(
            YangParser.Submodule_header_stmtsContext ctx) {
        final String submodule = actualPath.pop();
        logger.debug("exiting submodule " + submodule);
    }

    @Override
    public void enterOrganization_stmt(YangParser.Organization_stmtContext ctx) {
        final String organization = stringFromNode(ctx);
        moduleBuilder.setOrganization(organization);
    }

    @Override
    public void enterContact_stmt(YangParser.Contact_stmtContext ctx) {
        String contact = stringFromNode(ctx);
        moduleBuilder.setContact(contact);
    }

    @Override
    public void enterRevision_stmts(Revision_stmtsContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree treeNode = ctx.getChild(i);
            if (treeNode instanceof Revision_stmtContext) {
                final String revisionDateStr = stringFromNode(treeNode);
                try {
                    revision = simpleDateFormat.parse(revisionDateStr);
                } catch (ParseException e) {
                    logger.warn("Failed to parse revision string: "
                            + revisionDateStr);
                }
            }
        }
    }

    @Override
    public void enterDescription_stmt(YangParser.Description_stmtContext ctx) {
        // if this is module description...
        if (actualPath.size() == 1) {
            moduleBuilder.setDescription(stringFromNode(ctx));
        }
    }

    @Override
    public void enterImport_stmt(Import_stmtContext ctx) {
        super.enterImport_stmt(ctx);

        final String importName = stringFromNode(ctx);
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
                } catch (Exception e) {
                    logger.warn("Failed to parse import revision-date.", e);
                }
            }
        }
        moduleBuilder.addModuleImport(importName, importRevision, importPrefix);
    }

    @Override
    public void enterAugment_stmt(YangParser.Augment_stmtContext ctx) {
        final String augmentPath = stringFromNode(ctx);
        AugmentationSchemaBuilder builder = moduleBuilder.addAugment(
                augmentPath, actualPath);
        updatePath(augmentPath);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Description_stmtContext) {
                String desc = stringFromNode(child);
                builder.setDescription(desc);
            } else if (child instanceof Reference_stmtContext) {
                String ref = stringFromNode(child);
                builder.setReference(ref);
            } else if (child instanceof Status_stmtContext) {
                Status status = getStatus((Status_stmtContext) child);
                builder.setStatus(status);
            }
        }
    }

    @Override
    public void exitAugment_stmt(YangParser.Augment_stmtContext ctx) {
        final String augment = actualPath.pop();
        logger.debug("exiting augment " + augment);
    }

    @Override
    public void enterMust_stmt(YangParser.Must_stmtContext ctx) {
        String mustText = "";
        String description = null;
        String reference = null;
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof StringContext) {
                final StringContext context = (StringContext) child;
                for (int j = 0; j < context.getChildCount(); j++) {
                    String mustPart = context.getChild(j).getText();
                    if (j == 0) {
                        mustText += mustPart
                                .substring(0, mustPart.length() - 1);
                        continue;
                    }
                    if (j % 2 == 0) {
                        mustText += mustPart.substring(1);
                    }
                }
            } else if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            }
        }
        MustDefinitionBuilder builder = moduleBuilder.addMustDefinition(
                mustText, actualPath);
        builder.setDescription(description);
        builder.setReference(reference);
    }

    @Override
    public void enterTypedef_stmt(YangParser.Typedef_stmtContext ctx) {
        String typedefName = stringFromNode(ctx);
        QName typedefQName = new QName(namespace, revision, yangModelPrefix,
                typedefName);
        TypedefBuilder builder = moduleBuilder.addTypedef(typedefQName,
                actualPath);
        updatePath(typedefName);

        builder.setPath(getActualSchemaPath(actualPath, namespace, revision,
                yangModelPrefix));
        parseSchemaNodeArgs(ctx, builder);
    }

    @Override
    public void exitTypedef_stmt(YangParser.Typedef_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterType_stmt(YangParser.Type_stmtContext ctx) {
        String typeName = stringFromNode(ctx);
        QName typeQName;
        if (typeName.contains(":")) {
            String[] splittedName = typeName.split(":");
            // if this type contains prefix, it means that it point to type in
            // external module
            typeQName = new QName(null, null, splittedName[0], splittedName[1]);
        } else {
            typeQName = new QName(namespace, revision, yangModelPrefix,
                    typeName);
        }

        TypeDefinition<?> type = null;

        if (!YangTypesConverter.isBaseYangType(typeName)) {
            if (typeName.equals("leafref")) {
                // TODO: RevisionAwareXPath implementation
                type = new Leafref(new RevisionAwareXPath() {
                });
            } else {
                type = parseUnknownType(typeQName, ctx);
                // mark parent node of this type statement as dirty
                moduleBuilder.addDirtyNode(actualPath);
            }
        } else {

            Type_body_stmtsContext typeBody = null;
            for (int i = 0; i < ctx.getChildCount(); i++) {
                if (ctx.getChild(i) instanceof Type_body_stmtsContext) {
                    typeBody = (Type_body_stmtsContext) ctx.getChild(i);
                    break;
                }
            }

            if (typeBody == null) {
                // if there are no constraints, just grab default base yang type
                type = YangTypesConverter.javaTypeForBaseYangType(typeName);
            } else {
                List<RangeConstraint> rangeStatements = getRangeConstraints(typeBody);
                Integer fractionDigits = getFractionDigits(typeBody);
                List<LengthConstraint> lengthStatements = getLengthConstraints(typeBody);
                List<PatternConstraint> patternStatements = getPatternConstraint(typeBody);
                List<EnumTypeDefinition.EnumPair> enumConstants = YangModelBuilderHelper
                        .getEnumConstants(typeBody);

                if (typeName.equals("decimal64")) {
                    type = YangTypesConverter.javaTypeForBaseYangDecimal64Type(
                            rangeStatements, fractionDigits);
                } else if (typeName.startsWith("int")
                        || typeName.startsWith("uint")) {
                    type = YangTypesConverter.javaTypeForBaseYangIntegerType(
                            typeName, rangeStatements);
                } else if (typeName.equals("enumeration")) {
                    type = new EnumerationType(enumConstants);
                } else if (typeName.equals("string")) {
                    type = new StringType(lengthStatements, patternStatements);
                } else if (typeName.equals("bits")) {
                    type = new BitsType(getBits(typeBody, actualPath,
                            namespace, revision, yangModelPrefix));
                } else {
                    // TODO: implement binary + instance-identifier types
                }
            }

        }

        moduleBuilder.setType(type, actualPath);
        updatePath(typeName);
    }

    @Override
    public void exitType_stmt(YangParser.Type_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterGrouping_stmt(YangParser.Grouping_stmtContext ctx) {
        final String groupName = stringFromNode(ctx);
        QName groupQName = new QName(namespace, revision, yangModelPrefix,
                groupName);
        GroupingBuilder groupBuilder = moduleBuilder.addGrouping(groupQName,
                actualPath);
        updatePath("grouping");
        updatePath(groupName);
        parseSchemaNodeArgs(ctx, groupBuilder);
    }

    @Override
    public void exitGrouping_stmt(YangParser.Grouping_stmtContext ctx) {
        String actContainer = actualPath.pop();
        actContainer += "-" + actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterContainer_stmt(Container_stmtContext ctx) {
        super.enterContainer_stmt(ctx);
        String containerName = stringFromNode(ctx);
        QName containerQName = new QName(namespace, revision, yangModelPrefix,
                containerName);
        ContainerSchemaNodeBuilder containerBuilder = moduleBuilder
                .addContainerNode(containerQName, actualPath);
        updatePath(containerName);

        containerBuilder.setPath(getActualSchemaPath(actualPath, namespace,
                revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, containerBuilder);

        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree childNode = ctx.getChild(i);
            if (childNode instanceof Presence_stmtContext) {
                containerBuilder.setPresenceContainer(true);
            } else if (childNode instanceof Config_stmtContext) {
                for (int j = 0; j < childNode.getChildCount(); j++) {
                    ParseTree configArg = childNode.getChild(j);
                    if (configArg instanceof Config_argContext) {
                        String config = stringFromNode(configArg);
                        if (config.equals("true")) {
                            containerBuilder.setConfiguration(true);
                        } else {
                            containerBuilder.setConfiguration(false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void exitContainer_stmt(Container_stmtContext ctx) {
        super.exitContainer_stmt(ctx);
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    private boolean isLeafReadOnly(final ParseTree leaf) {
        if (leaf != null) {
            for (int i = 0; i < leaf.getChildCount(); ++i) {
                final ParseTree configContext = leaf.getChild(i);
                if (configContext instanceof Config_argContext) {
                    final String value = stringFromNode(configContext);
                    if (value.equals("true")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void enterLeaf_stmt(Leaf_stmtContext ctx) {
        super.enterLeaf_stmt(ctx);

        final String leafName = stringFromNode(ctx);
        QName leafQName = new QName(namespace, revision, yangModelPrefix,
                leafName);
        LeafSchemaNodeBuilder leafBuilder = moduleBuilder.addLeafNode(
                leafQName, actualPath);
        updatePath(leafName);

        leafBuilder.setPath(getActualSchemaPath(actualPath, namespace,
                revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, leafBuilder);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Config_stmtContext) {
                leafBuilder.setConfiguration(isLeafReadOnly(child));
            }
        }
    }

    @Override
    public void exitLeaf_stmt(YangParser.Leaf_stmtContext ctx) {
        final String actLeaf = actualPath.pop();
        logger.debug("exiting " + actLeaf);
    }

    @Override
    public void enterUses_stmt(YangParser.Uses_stmtContext ctx) {
        final String groupingPathStr = stringFromNode(ctx);
        moduleBuilder.addUsesNode(groupingPathStr, actualPath);
        updatePath(groupingPathStr);
    }

    @Override
    public void exitUses_stmt(YangParser.Uses_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterLeaf_list_stmt(Leaf_list_stmtContext ctx) {
        super.enterLeaf_list_stmt(ctx);

        final String leafListName = stringFromNode(ctx);
        QName leafListQName = new QName(namespace, revision, yangModelPrefix,
                leafListName);
        LeafListSchemaNodeBuilder leafListBuilder = moduleBuilder
                .addLeafListNode(leafListQName, actualPath);
        updatePath(leafListName);

        parseSchemaNodeArgs(ctx, leafListBuilder);

        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree childNode = ctx.getChild(i);
            if (childNode instanceof Config_stmtContext) {
                leafListBuilder.setConfiguration(isLeafReadOnly(childNode));
            } else if (childNode instanceof Ordered_by_stmtContext) {
                final Ordered_by_stmtContext orderedBy = (Ordered_by_stmtContext) childNode;
                final boolean userOrdered = parseUserOrdered(orderedBy);
                leafListBuilder.setUserOrdered(userOrdered);
            }
        }
    }

    @Override
    public void exitLeaf_list_stmt(YangParser.Leaf_list_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterList_stmt(List_stmtContext ctx) {
        super.enterList_stmt(ctx);

        final String containerName = stringFromNode(ctx);
        QName containerQName = new QName(namespace, revision, yangModelPrefix,
                containerName);
        ListSchemaNodeBuilder listBuilder = moduleBuilder.addListNode(
                containerQName, actualPath);
        updatePath(containerName);

        listBuilder.setPath(getActualSchemaPath(actualPath, namespace,
                revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, listBuilder);

        String keyDefinition = "";
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            ParseTree childNode = ctx.getChild(i);

            if (childNode instanceof Ordered_by_stmtContext) {
                final Ordered_by_stmtContext orderedBy = (Ordered_by_stmtContext) childNode;
                final boolean userOrdered = parseUserOrdered(orderedBy);
                listBuilder.setUserOrdered(userOrdered);
            } else if (childNode instanceof Key_stmtContext) {
                List<QName> key = createListKey(keyDefinition, namespace,
                        revision, keyDefinition);
                listBuilder.setKeyDefinition(key);
            }
        }
    }

    @Override
    public void exitList_stmt(List_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterNotification_stmt(YangParser.Notification_stmtContext ctx) {
        final String notificationName = stringFromNode(ctx);
        QName notificationQName = new QName(namespace, revision,
                yangModelPrefix, notificationName);
        NotificationBuilder notificationBuilder = moduleBuilder
                .addNotification(notificationQName, actualPath);
        updatePath(notificationName);

        notificationBuilder.setPath(getActualSchemaPath(actualPath, namespace,
                revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, notificationBuilder);
    }

    @Override
    public void exitNotification_stmt(YangParser.Notification_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterRpc_stmt(YangParser.Rpc_stmtContext ctx) {
        final String rpcName = stringFromNode(ctx);
        QName rpcQName = new QName(namespace, revision, yangModelPrefix,
                rpcName);
        RpcDefinitionBuilder rpcBuilder = moduleBuilder.addRpc(rpcQName,
                actualPath);
        updatePath(rpcName);

        rpcBuilder.setPath(getActualSchemaPath(actualPath, namespace, revision,
                yangModelPrefix));
        parseSchemaNodeArgs(ctx, rpcBuilder);
    }

    @Override
    public void exitRpc_stmt(YangParser.Rpc_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterInput_stmt(YangParser.Input_stmtContext ctx) {
        updatePath("input");
    }

    @Override
    public void exitInput_stmt(YangParser.Input_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterOutput_stmt(YangParser.Output_stmtContext ctx) {
        updatePath("output");
    }

    @Override
    public void exitOutput_stmt(YangParser.Output_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterFeature_stmt(YangParser.Feature_stmtContext ctx) {
        final String featureName = stringFromNode(ctx);
        QName featureQName = new QName(namespace, revision, yangModelPrefix,
                featureName);
        FeatureBuilder featureBuilder = moduleBuilder.addFeature(featureQName,
                actualPath);
        updatePath(featureName);

        featureBuilder.setPath(getActualSchemaPath(actualPath, namespace,
                revision, yangModelPrefix));
        parseSchemaNodeArgs(ctx, featureBuilder);
    }

    @Override
    public void exitFeature_stmt(YangParser.Feature_stmtContext ctx) {
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    @Override
    public void enterDeviation_stmt(YangParser.Deviation_stmtContext ctx) {
        final String targetPath = stringFromNode(ctx);
        String reference = null;
        String deviate = null;
        DeviationBuilder builder = moduleBuilder.addDeviation(targetPath);
        updatePath(targetPath);

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
        final String actContainer = actualPath.pop();
        logger.debug("exiting " + actContainer);
    }

    public ModuleBuilder getModuleBuilder() {
        return moduleBuilder;
    }

    private void updatePath(String containerName) {
        actualPath.push(containerName);
    }

    /**
     * Parse ordered-by statement.
     * 
     * @param childNode
     *            Ordered_by_stmtContext
     * @return true, if ordered-by contains value 'user' or false otherwise
     */
    private boolean parseUserOrdered(Ordered_by_stmtContext childNode) {
        boolean result = false;
        for (int j = 0; j < childNode.getChildCount(); j++) {
            ParseTree orderArg = childNode.getChild(j);
            if (orderArg instanceof Ordered_by_argContext) {
                String orderStr = stringFromNode(orderArg);
                if (orderStr.equals("system")) {
                    result = false;
                } else if (orderStr.equals("user")) {
                    result = true;
                } else {
                    logger.warn("Invalid 'ordered-by' statement.");
                }
            }
        }
        return result;
    }

}
