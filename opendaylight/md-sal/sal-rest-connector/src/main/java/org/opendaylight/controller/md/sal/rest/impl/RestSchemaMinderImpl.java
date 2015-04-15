/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.impl;

import static org.opendaylight.controller.md.sal.rest.common.RestconfInternalConstants.MOUNT;
import static org.opendaylight.controller.md.sal.rest.common.RestconfInternalConstants.SLASH_SPLITTER;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.ws.rs.core.Response.Status;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.rest.RestSchemaMinder;
import org.opendaylight.controller.md.sal.rest.common.RestconfInternalConstants;
import org.opendaylight.controller.md.sal.rest.common.RestconfParsingUtils;
import org.opendaylight.controller.md.sal.rest.common.RestconfValidationUtils;
import org.opendaylight.controller.sal.rest.impl.RestUtil;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 22, 2015
 */
public class RestSchemaMinderImpl implements RestSchemaMinder {
    private static final Logger LOG = LoggerFactory.getLogger(RestSchemaMinderImpl.class);

    private final DOMMountPointService mountService;
    private SchemaContext globalSchema;

    public RestSchemaMinderImpl(@Nonnull final DOMMountPointService mountServ,
            @Nonnull final SchemaContext schemaContext) {
        mountService = Preconditions.checkNotNull(mountServ);
        globalSchema = Preconditions.checkNotNull(schemaContext);
    }

    @Override
    public synchronized void tell(@Nonnull final SchemaContext schemaContext) {
        globalSchema = Preconditions.checkNotNull(schemaContext);
    }

    @Override
    public InstanceIdentifierContext<?> makeFrom(final String identifier) {
        final List<String> pathArgs = RestconfParsingUtils.urlPathArgsDecode(SLASH_SPLITTER.split(identifier));
        RestconfParsingUtils.omitFirstAndLastEmptyString(pathArgs);
        if (pathArgs.isEmpty()) {
            return null;
        }

        final String first = pathArgs.iterator().next();
        final String startModule = RestconfParsingUtils.toModuleName(first);
        RestconfValidationUtils.checkDocumentedError(startModule == null, ErrorType.PROTOCOL,
                ErrorTag.INVALID_VALUE, "First node in URI has to be in format \"moduleName:nodeName\"");

        final Module latestModule = globalSchema.findModuleByName(startModule, null);
        final InstanceIdentifierContext<?> iiWithSchemaNode =
                collectPathArguments(null, pathArgs, latestModule, null, globalSchema);
        RestconfValidationUtils.checkDocumentedError(iiWithSchemaNode == null, ErrorType.PROTOCOL,
                ErrorTag.INVALID_VALUE, "URI has bad format");

        return iiWithSchemaNode;
    }

    private InstanceIdentifierContext<?> collectPathArguments(final InstanceIdentifierBuilder inBuilder,
                                            final List<String> pathArgs, final DataNodeContainer latestModule,
                                            final DOMMountPoint mountPoint, final SchemaContext schemaContext) {
        RestconfValidationUtils.checkDocumentedError(schemaContext == null, Status.SERVICE_UNAVAILABLE);
        RestconfValidationUtils.checkDocumentedError(pathArgs == null || latestModule == null,
                ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE, "URI has bad format");
        final InstanceIdentifierBuilder builder = inBuilder != null ? inBuilder : YangInstanceIdentifier.builder();
        if (pathArgs.isEmpty()) {
            return new InstanceIdentifierContext<DataSchemaNode>(builder.build(),
                    (DataSchemaNode) latestModule, null, schemaContext);
        }

        final String head = pathArgs.iterator().next();
        final String nodeName = RestconfParsingUtils.toNodeName(head);
        final String moduleName = RestconfParsingUtils.toModuleName(head);

        DataSchemaNode targetNode = null;
        if (Strings.isNullOrEmpty(moduleName)) {
            if (Objects.equal(moduleName, RestconfInternalConstants.MOUNT_MODULE)
                    && Objects.equal(nodeName, RestconfInternalConstants.MOUNT_NODE)) {
                mountPointDefine(pathArgs, mountPoint, builder.build(), moduleName);
            }
            final Module module = schemaContext.findModuleByName(moduleName, null);
            RestconfValidationUtils.checkDocumentedError(module == null, ErrorType.PROTOCOL,
                    ErrorTag.UNKNOWN_ELEMENT, "\"" + moduleName + "\" module does not exist.");

            targetNode = findInstanceDataChildByNameAndNamespace(latestModule, nodeName, module.getNamespace());
        } else {
            final List<DataSchemaNode> potentialSchemaNodes = findInstanceDataChildrenByName(latestModule, nodeName);
            targetNode = potentialSchemaNodes.iterator().next();
        }

        if (targetNode == null && latestModule instanceof Module) {
            final QName rpcQName = QName.create(((Module)latestModule).getQNameModule(), nodeName);
//            final RpcDefinition rpc = ControllerContext.getInstance().getRpcDeflinition(rpcQName);
            // FIXME : thing about cache RpcDefinition (do we really need it ?
            final RpcDefinition rpc = globalSchema.getOperations().iterator().next();
            return new InstanceIdentifierContext<RpcDefinition>(builder.build(), rpc, mountPoint,
                    mountPoint != null ? mountPoint.getSchemaContext() : schemaContext);
        }

        RestconfValidationUtils.checkDocumentedError(targetNode == null, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                "URI has bad format. Possible reasons:\n" + " 1. \"" + head + "\" was not found in parent data node.\n"
                + " 2. \"" + head + "\" is behind mount point. Then it should be in format \"/" + MOUNT + "/" + head + "\".");

        RestconfValidationUtils.checkDocumentedError(isListOrContainer(targetNode), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                "URI has bad format. Node \"" + head + "\" must be Container or List yang type.");

        int consumed = 1;
        if ((targetNode instanceof ListSchemaNode)) {
            final ListSchemaNode listNode = ((ListSchemaNode) targetNode);
            final int keysSize = listNode.getKeyDefinition().size();
            RestconfValidationUtils.checkDocumentedError(((pathArgs.size() - consumed) < keysSize), ErrorType.PROTOCOL,
                    ErrorTag.DATA_MISSING, "Missing key for list \"" + listNode.getQName().getLocalName() + "\".");

            final List<String> uriKeyValues = pathArgs.subList(consumed, consumed + keysSize);
            final HashMap<QName, Object> keyValues = new HashMap<QName, Object>();
            int i = 0;
            for (final QName key : listNode.getKeyDefinition()) {
                {
                    final String uriKeyValue = uriKeyValues.get(i);
                    RestconfValidationUtils.checkDocumentedError(uriKeyValue.equals("null"), ErrorType.PROTOCOL,
                            ErrorTag.INVALID_VALUE, "URI has bad format. List \"" + listNode.getQName().getLocalName()
                            + "\" cannot contain \"null\" value as a key.");

                    addKeyValue(keyValues, listNode.getDataChildByName(key), uriKeyValue, mountPoint);
                    i++;
                }
            }

            consumed = consumed + i;
            builder.nodeWithKey(targetNode.getQName(), keyValues);
        } else {
            builder.node(targetNode.getQName());
        }

        if ((targetNode instanceof DataNodeContainer)) {
            final List<String> remaining = pathArgs.subList(consumed, pathArgs.size());
            return collectPathArguments(builder, remaining, ((DataNodeContainer) targetNode), mountPoint,
                    mountPoint != null ? mountPoint.getSchemaContext() : schemaContext);
        }

        return new InstanceIdentifierContext(builder.build(), targetNode, mountPoint,
                mountPoint != null ? mountPoint.getSchemaContext() : schemaContext);
    }

    private static void addKeyValue(final HashMap<QName, Object> map, final DataSchemaNode node, final String uriValue,
            final DOMMountPoint mountPoint) {
        Preconditions.<String> checkNotNull(uriValue);
        Preconditions.checkArgument((node instanceof LeafSchemaNode));

        final String urlDecoded = RestconfParsingUtils.urlPathArgDecode(uriValue);
        final TypeDefinition<? extends Object> typedef = ((LeafSchemaNode) node).getType();
        final Codec<Object, Object> codec = RestCodec.from(typedef, mountPoint);

        Object decoded = codec == null ? null : codec.deserialize(urlDecoded);
        String additionalInfo = "";
        if (decoded == null) {
            final TypeDefinition<? extends Object> baseType = RestUtil.resolveBaseTypeFrom(typedef);
            if ((baseType instanceof IdentityrefTypeDefinition)) {
                decoded = RestconfParsingUtils.toQName(urlDecoded);
                additionalInfo = "For key which is of type identityref it should be in format module_name:identity_name.";
            }
        }

        RestconfValidationUtils.checkDocumentedError(decoded == null, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                uriValue + " from URI can't be resolved. " + additionalInfo);

        map.put(node.getQName(), decoded);
    }

    private static boolean isListOrContainer(final DataSchemaNode node) {
        return node instanceof ListSchemaNode || node instanceof ContainerSchemaNode;
    }

    // TODO find better place (? RestconvSchemaNodeUtils ?)
    private static DataSchemaNode findInstanceDataChildByNameAndNamespace(final DataNodeContainer container, final String name,
            final URI namespace) {
        Preconditions.<URI> checkNotNull(namespace);

        final List<DataSchemaNode> potentialSchemaNodes = findInstanceDataChildrenByName(container, name);

        final Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply(final DataSchemaNode node) {
                return Objects.equal(node.getQName().getNamespace(), namespace);
            }
        };

        final Iterable<DataSchemaNode> result = Iterables.filter(potentialSchemaNodes, filter);
        return Iterables.getFirst(result, null);
    }

    // TODO find better place (? RestconfSchemaNodeUtils ?)
    private static List<DataSchemaNode> findInstanceDataChildrenByName(final DataNodeContainer container, final String nodeName) {
        Preconditions.<DataNodeContainer> checkNotNull(container);
        Preconditions.<String> checkNotNull(nodeName);

        final List<DataSchemaNode> instantiatedDataNodeContainers = new ArrayList<DataSchemaNode>();
        collectInstanceDataNodeContainers(instantiatedDataNodeContainers, container, nodeName);

        if (instantiatedDataNodeContainers.size() > 1) {
            final StringBuilder errMsgBuilder = new StringBuilder("URI has bad format. Node \"");
            errMsgBuilder.append(nodeName).append("\" is added as augment from more than one module. ")
            .append("\" is added as augment from more than one module. ")
            .append("Therefore the node must have module name and it has to be in format \"moduleName:nodeName\".")
            .append("\nThe node is added as augment from modules with namespaces:\n");
            for (final DataSchemaNode potentialNodeSchema : instantiatedDataNodeContainers) {
                errMsgBuilder.append("   ").append(potentialNodeSchema.getQName().getNamespace()).append("\n");
            }
            throw new RestconfDocumentedException(errMsgBuilder.toString(), ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
        }
        RestconfValidationUtils.checkDocumentedError(instantiatedDataNodeContainers.isEmpty(), ErrorType.PROTOCOL,
                ErrorTag.UNKNOWN_ELEMENT, "\"" + nodeName + "\" in URI was not found in parent data node");
        return instantiatedDataNodeContainers;
    }

    private static void collectInstanceDataNodeContainers(final List<DataSchemaNode> potentialSchemaNodes,
            final DataNodeContainer container, final String name) {
        final Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply(final DataSchemaNode node) {
                return Objects.equal(node.getQName().getLocalName(), name);
            }
        };
        final Iterable<DataSchemaNode> nodes = Iterables.filter(container.getChildNodes(), filter);
        // Can't combine this loop with the filter above because the filter is
        // lazily-applied by Iterables.filter.
        for (final DataSchemaNode potentialNode : nodes) {
            if (isInstantiatedDataSchema(potentialNode)) {
                potentialSchemaNodes.add(potentialNode);
            }
        }
        final Function<ChoiceSchemaNode, Set<ChoiceCaseNode>> choiceFunction = new Function<ChoiceSchemaNode, Set<ChoiceCaseNode>>() {
            @Override
            public Set<ChoiceCaseNode> apply(final ChoiceSchemaNode node) {
                return node.getCases();
            }
        };
        final Iterable<ChoiceSchemaNode> choiceNodes = Iterables.filter(container.getChildNodes(), ChoiceSchemaNode.class);
        final Iterable<Set<ChoiceCaseNode>> map = Iterables.transform(choiceNodes, choiceFunction);

        final Iterable<ChoiceCaseNode> allCases = Iterables.<ChoiceCaseNode> concat(map);
        for (final ChoiceCaseNode caze : allCases) {
            collectInstanceDataNodeContainers(potentialSchemaNodes, caze, name);
        }
    }

    private static boolean isInstantiatedDataSchema(final DataSchemaNode node) {
        return node instanceof LeafSchemaNode || node instanceof LeafListSchemaNode
                || node instanceof ContainerSchemaNode || node instanceof ListSchemaNode
                || node instanceof AnyXmlSchemaNode;
    }

    // TODO find better place (? RestconfSchemaNodeUtils ?)
    private InstanceIdentifierContext<?> mountPointDefine(final List<String> pathArgs,
            final DOMMountPoint mount, final YangInstanceIdentifier partialPath, final String moduleName) {
        RestconfValidationUtils.checkDocumentedError(mount != null, ErrorType.APPLICATION,
                ErrorTag.OPERATION_NOT_SUPPORTED, "Restconf supports just one mount point in URI.");

        RestconfValidationUtils.checkDocumentedError(mountService == null, ErrorType.APPLICATION,
                ErrorTag.OPERATION_NOT_SUPPORTED, "MountService was not found. Finding behind mount points does not work.");

        final Optional<DOMMountPoint> mountOpt = mountService.getMountPoint(partialPath);
        RestconfValidationUtils.checkDocumentedError(( ! mountOpt.isPresent()), ErrorType.PROTOCOL,
                ErrorTag.UNKNOWN_ELEMENT, "Mount point does not exist.");

        final DOMMountPoint mountPoint = mountOpt.get();
        final SchemaContext mountPointSchema = mountPoint.getSchemaContext();
        RestconfValidationUtils.checkDocumentedError(mountPointSchema == null, ErrorType.APPLICATION,
                ErrorTag.UNKNOWN_ELEMENT, "Mount point does not contain any schema with modules.");

        if (pathArgs.size() == 1) {
            final YangInstanceIdentifier instance = YangInstanceIdentifier.builder().build();
            return new InstanceIdentifierContext(instance, mountPointSchema, mountPoint, mountPointSchema);
        }

        final String moduleNameBehindMountPoint = RestconfParsingUtils.toModuleName(pathArgs.get(1));
        RestconfValidationUtils.checkDocumentedError(moduleNameBehindMountPoint == null, ErrorType.PROTOCOL,
                ErrorTag.INVALID_VALUE, "First node after mount point in URI has to be in format \"moduleName:nodeName\"");

        final Module moduleBehindMountPoint = mountPointSchema.findModuleByName(moduleNameBehindMountPoint, null);
        RestconfValidationUtils.checkDocumentedError(moduleBehindMountPoint == null, ErrorType.PROTOCOL,
                ErrorTag.UNKNOWN_ELEMENT, "\"" + moduleName + "\" module does not exist in mount point.");

        final List<String> subList = pathArgs.subList(1, pathArgs.size());
        return collectPathArguments(null, subList, moduleBehindMountPoint, mount, mount.getSchemaContext());
    }
}
