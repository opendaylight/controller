/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import static org.opendaylight.controller.sal.rest.doc.util.RestDocgenUtil.resolvePathArgumentsName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.ws.rs.core.UriInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.sal.rest.doc.model.builder.OperationBuilder;
import org.opendaylight.controller.sal.rest.doc.swagger.Api;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.Operation;
import org.opendaylight.controller.sal.rest.doc.swagger.Parameter;
import org.opendaylight.controller.sal.rest.doc.swagger.Resource;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseYangSwaggerGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(BaseYangSwaggerGenerator.class);

    protected static final String API_VERSION = "1.0.0";
    protected static final String SWAGGER_VERSION = "1.2";
    protected static final String RESTCONF_CONTEXT_ROOT = "restconf";

    static final String MODULE_NAME_SUFFIX = "_module";
    private final ModelGenerator jsonConverter = new ModelGenerator();

    // private Map<String, ApiDeclaration> MODULE_DOC_CACHE = new HashMap<>()
    private final ObjectMapper mapper = new ObjectMapper();

    protected BaseYangSwaggerGenerator() {
        mapper.registerModule(new JsonOrgModule());
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    /**
     *
     * @param uriInfo
     * @param operType
     * @return list of modules converted to swagger compliant resource list.
     */
    public ResourceList getResourceListing(final UriInfo uriInfo, final SchemaContext schemaContext, final String context) {

        final ResourceList resourceList = createResourceList();

        final Set<Module> modules = getSortedModules(schemaContext);

        final List<Resource> resources = new ArrayList<>(modules.size());

        LOG.info("Modules found [{}]", modules.size());

        for (final Module module : modules) {
            final String revisionString = SimpleDateFormatUtil.getRevisionFormat().format(module.getRevision());
            final Resource resource = new Resource();
            LOG.debug("Working on [{},{}]...", module.getName(), revisionString);
            final ApiDeclaration doc = getApiDeclaration(module.getName(), revisionString, uriInfo, schemaContext, context);

            if (doc != null) {
                resource.setPath(generatePath(uriInfo, module.getName(), revisionString));
                resources.add(resource);
            } else {
                LOG.debug("Could not generate doc for {},{}", module.getName(), revisionString);
            }
        }

        resourceList.setApis(resources);

        return resourceList;
    }

    protected ResourceList createResourceList() {
        final ResourceList resourceList = new ResourceList();
        resourceList.setApiVersion(API_VERSION);
        resourceList.setSwaggerVersion(SWAGGER_VERSION);
        return resourceList;
    }

    protected String generatePath(final UriInfo uriInfo, final String name, final String revision) {
        final URI uri = uriInfo.getRequestUriBuilder().path(generateCacheKey(name, revision)).build();
        return uri.toASCIIString();
    }

    public ApiDeclaration getApiDeclaration(final String module, final String revision, final UriInfo uriInfo, final SchemaContext schemaContext, final String context) {
        Date rev = null;

        try {
            if(revision != null && !revision.equals("0000-00-00")) {
                rev = SimpleDateFormatUtil.getRevisionFormat().parse(revision);
            }
        } catch (final ParseException e) {
            throw new IllegalArgumentException(e);
        }

        if(rev != null) {
            final Calendar cal = new GregorianCalendar();

            cal.setTime(rev);

            if(cal.get(Calendar.YEAR) < 1970) {
                rev = null;
            }
        }

        final Module m = schemaContext.findModuleByName(module, rev);
        Preconditions.checkArgument(m != null, "Could not find module by name,revision: " + module + "," + revision);

        return getApiDeclaration(m, rev, uriInfo, context, schemaContext);
    }

    public ApiDeclaration getApiDeclaration(final Module module, final Date revision, final UriInfo uriInfo, final String context, final SchemaContext schemaContext) {
        final String basePath = createBasePathFromUriInfo(uriInfo);

        ApiDeclaration doc = getSwaggerDocSpec(module, basePath, context, schemaContext);
        if (doc != null) {
            return doc;
        }
        return null;
    }

    protected String createBasePathFromUriInfo(final UriInfo uriInfo) {
        String portPart = "";
        final int port = uriInfo.getBaseUri().getPort();
        if (port != -1) {
            portPart = ":" + port;
        }
        final String basePath = new StringBuilder(uriInfo.getBaseUri().getScheme()).append("://")
                .append(uriInfo.getBaseUri().getHost()).append(portPart).append("/").append(RESTCONF_CONTEXT_ROOT)
                .toString();
        return basePath;
    }

    public ApiDeclaration getSwaggerDocSpec(final Module m, final String basePath, final String context, final SchemaContext schemaContext) {
        final ApiDeclaration doc = createApiDeclaration(basePath);

        final List<Api> apis = new ArrayList<Api>();

        final Collection<DataSchemaNode> dataSchemaNodes = m.getChildNodes();
        LOG.debug("child nodes size [{}]", dataSchemaNodes.size());
        for (final DataSchemaNode node : dataSchemaNodes) {
            if ((node instanceof ListSchemaNode) || (node instanceof ContainerSchemaNode)) {

                LOG.debug("Is Configuration node [{}] [{}]", node.isConfiguration(), node.getQName().getLocalName());

                List<Parameter> pathParams = new ArrayList<Parameter>();
                String resourcePath = getDataStorePath("/config/", context);
                addRootPostLink(m, (DataNodeContainer) node, pathParams, resourcePath, apis);
                addApis(node, apis, resourcePath, pathParams, schemaContext, true);

                pathParams = new ArrayList<Parameter>();
                resourcePath = getDataStorePath("/operational/", context);
                addApis(node, apis, resourcePath, pathParams, schemaContext, false);
            }
        }

        final Set<RpcDefinition> rpcs = m.getRpcs();
        for (final RpcDefinition rpcDefinition : rpcs) {
            final String resourcePath = getDataStorePath("/operations/", context);
            addRpcs(rpcDefinition, apis, resourcePath, schemaContext);
        }

        LOG.debug("Number of APIs found [{}]", apis.size());

        if (!apis.isEmpty()) {
            doc.setApis(apis);
            JSONObject models = null;

            try {
                models = jsonConverter.convertToJsonSchema(m, schemaContext);
                doc.setModels(models);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(mapper.writeValueAsString(doc));
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return doc;
        }
        return null;
    }

    private void addRootPostLink(final Module m, final DataNodeContainer node, final List<Parameter> pathParams,
            final String resourcePath, final List<Api> apis) {
        if (containsListOrContainer(m.getChildNodes())) {
            final Api apiForRootPostUri = new Api();
            apiForRootPostUri.setPath(resourcePath);
            apiForRootPostUri.setOperations(operationPost(m.getName()+MODULE_NAME_SUFFIX, m.getDescription(), m, pathParams, true));
            apis.add(apiForRootPostUri);
        }
    }

    protected ApiDeclaration createApiDeclaration(final String basePath) {
        final ApiDeclaration doc = new ApiDeclaration();
        doc.setApiVersion(API_VERSION);
        doc.setSwaggerVersion(SWAGGER_VERSION);
        doc.setBasePath(basePath);
        doc.setProduces(Arrays.asList("application/json", "application/xml"));
        return doc;
    }

    protected String getDataStorePath(final String dataStore, final String context) {
        return dataStore + context;
    }

    private String generateCacheKey(final Module m) {
        return generateCacheKey(m.getName(), SimpleDateFormatUtil.getRevisionFormat().format(m.getRevision()));
    }

    private String generateCacheKey(final String module, final String revision) {
        return module + "(" + revision + ")";
    }

    private void addApis(final DataSchemaNode node, final List<Api> apis, final String parentPath, final List<Parameter> parentPathParams, final SchemaContext schemaContext,
            final boolean addConfigApi) {

        final Api api = new Api();
        final List<Parameter> pathParams = new ArrayList<Parameter>(parentPathParams);

        final String resourcePath = parentPath + createPath(node, pathParams, schemaContext) + "/";
        LOG.debug("Adding path: [{}]", resourcePath);
        api.setPath(resourcePath);

        Iterable<DataSchemaNode> childSchemaNodes = Collections.<DataSchemaNode> emptySet();
        if ((node instanceof ListSchemaNode) || (node instanceof ContainerSchemaNode)) {
            final DataNodeContainer dataNodeContainer = (DataNodeContainer) node;
            childSchemaNodes = dataNodeContainer.getChildNodes();
        }
        api.setOperations(operation(node, pathParams, addConfigApi, childSchemaNodes));
        apis.add(api);

        for (final DataSchemaNode childNode : childSchemaNodes) {
            if (childNode instanceof ListSchemaNode || childNode instanceof ContainerSchemaNode) {
                // keep config and operation attributes separate.
                if (childNode.isConfiguration() == addConfigApi) {
                    addApis(childNode, apis, resourcePath, pathParams, schemaContext, addConfigApi);
                }
            }
        }

    }

    private boolean containsListOrContainer(final Iterable<DataSchemaNode> nodes) {
        for (final DataSchemaNode child : nodes) {
            if (child instanceof ListSchemaNode || child instanceof ContainerSchemaNode) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param node
     * @param pathParams
     * @return
     */
    private List<Operation> operation(final DataSchemaNode node, final List<Parameter> pathParams, final boolean isConfig, final Iterable<DataSchemaNode> childSchemaNodes) {
        final List<Operation> operations = new ArrayList<>();

        final OperationBuilder.Get getBuilder = new OperationBuilder.Get(node, isConfig);
        operations.add(getBuilder.pathParams(pathParams).build());

        if (isConfig) {
            final OperationBuilder.Put putBuilder = new OperationBuilder.Put(node.getQName().getLocalName(),
                    node.getDescription());
            operations.add(putBuilder.pathParams(pathParams).build());

            final OperationBuilder.Delete deleteBuilder = new OperationBuilder.Delete(node);
            operations.add(deleteBuilder.pathParams(pathParams).build());

            if (containsListOrContainer(childSchemaNodes)) {
                operations.addAll(operationPost(node.getQName().getLocalName(), node.getDescription(), (DataNodeContainer) node,
                        pathParams, isConfig));
            }
        }
        return operations;
    }

    /**
     * @param node
     * @param pathParams
     * @return
     */
    private List<Operation> operationPost(final String name, final String description, final DataNodeContainer dataNodeContainer, List<Parameter> pathParams, boolean isConfig) {
        List<Operation> operations = new ArrayList<>();
        if (isConfig) {
            OperationBuilder.Post postBuilder = new OperationBuilder.Post(name, description, dataNodeContainer);
            operations.add(postBuilder.pathParams(pathParams).build());
        }
        return operations;
    }

    private String createPath(final DataSchemaNode schemaNode, List<Parameter> pathParams, SchemaContext schemaContext) {
        ArrayList<LeafSchemaNode> pathListParams = new ArrayList<LeafSchemaNode>();
        StringBuilder path = new StringBuilder();
        String localName = resolvePathArgumentsName(schemaNode, schemaContext);
        path.append(localName);

        if ((schemaNode instanceof ListSchemaNode)) {
            final List<QName> listKeys = ((ListSchemaNode) schemaNode).getKeyDefinition();
            for (final QName listKey : listKeys) {
                DataSchemaNode _dataChildByName = ((DataNodeContainer) schemaNode).getDataChildByName(listKey);
                pathListParams.add(((LeafSchemaNode) _dataChildByName));

                String pathParamIdentifier = new StringBuilder("/{").append(listKey.getLocalName()).append("}")
                        .toString();
                path.append(pathParamIdentifier);

                Parameter pathParam = new Parameter();
                pathParam.setName(listKey.getLocalName());
                pathParam.setDescription(_dataChildByName.getDescription());
                pathParam.setType("string");
                pathParam.setParamType("path");

                pathParams.add(pathParam);
            }
        }
        return path.toString();
    }

    protected void addRpcs(final RpcDefinition rpcDefn, final List<Api> apis, final String parentPath, final SchemaContext schemaContext) {
        final Api rpc = new Api();
        final String resourcePath = parentPath + resolvePathArgumentsName(rpcDefn, schemaContext);
        rpc.setPath(resourcePath);

        final Operation operationSpec = new Operation();
        operationSpec.setMethod("POST");
        operationSpec.setNotes(rpcDefn.getDescription());
        operationSpec.setNickname(rpcDefn.getQName().getLocalName());
        if (rpcDefn.getOutput() != null) {
            operationSpec.setType("(" + rpcDefn.getQName().getLocalName() + ")output");
        }
        if (rpcDefn.getInput() != null) {
            final Parameter payload = new Parameter();
            payload.setParamType("body");
            payload.setType("(" + rpcDefn.getQName().getLocalName() + ")input");
            operationSpec.setParameters(Collections.singletonList(payload));
            operationSpec.setConsumes(OperationBuilder.CONSUMES_PUT_POST);
        }

        rpc.setOperations(Arrays.asList(operationSpec));

        apis.add(rpc);
    }

    protected SortedSet<Module> getSortedModules(final SchemaContext schemaContext) {
        if (schemaContext == null) {
            return new TreeSet<>();
        }

        final Set<Module> modules = schemaContext.getModules();

        final SortedSet<Module> sortedModules = new TreeSet<>(new Comparator<Module>() {
            @Override
            public int compare(final Module module1, final Module module2) {
                int result = module1.getName().compareTo(module2.getName());
                if (result == 0) {
                    Date module1Revision = module1.getRevision() != null ? module1.getRevision() : new Date(0);
                    Date module2Revision = module2.getRevision() != null ? module2.getRevision() : new Date(0);
                    result = module1Revision.compareTo(module2Revision);
                }
                if (result == 0) {
                    result = module1.getNamespace().compareTo(module2.getNamespace());
                }
                return result;
            }
        });
        for (final Module m : modules) {
            if (m != null) {
                sortedModules.add(m);
            }
        }
        return sortedModules;
    }

}
