/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.base.Preconditions;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.sal.rest.doc.model.builder.OperationBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.opendaylight.controller.sal.rest.doc.util.RestDocgenUtil.resolvePathArgumentsName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.opendaylight.controller.sal.rest.doc.swagger.Api;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.Operation;
import org.opendaylight.controller.sal.rest.doc.swagger.Parameter;
import org.opendaylight.controller.sal.rest.doc.swagger.Resource;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;

public class BaseYangSwaggerGenerator {

  private static Logger _logger = LoggerFactory.getLogger(BaseYangSwaggerGenerator.class);

  protected static final String API_VERSION = "1.0.0";
  protected static final String SWAGGER_VERSION = "1.2";
  protected static final String RESTCONF_CONTEXT_ROOT = "restconf";

  static final String MODULE_NAME_SUFFIX = "_module";
  protected final DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private final ModelGenerator jsonConverter = new ModelGenerator();

  private static final String HIDE_MODULE_YANG_EXT = "module-scope-private";
  private static final String HIDE_CHILDREN_YANG_EXT = "children-scope-private";


  // private Map<String, ApiDeclaration> MODULE_DOC_CACHE = new HashMap<>()
  private final ObjectMapper mapper = new ObjectMapper();

  protected BaseYangSwaggerGenerator() {
    mapper.registerModule(new JsonOrgModule());
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
  }

  /**
   * @param uriInfo
   * @param operType
   * @return list of modules converted to swagger compliant resource list.
   */
  public ResourceList getResourceListing(UriInfo uriInfo, SchemaContext schemaContext, String context) {

    ResourceList resourceList = createResourceList();

    Set<Module> modules = getSortedModules(schemaContext);

    List<Resource> resources = new ArrayList<>(modules.size());

    _logger.info("Modules found [{}]", modules.size());

    for (Module module : modules) {

      //If module is to be hidden, then move on
      if (hideModule(module)) {
        continue;
      }

      String revisionString = SIMPLE_DATE_FORMAT.format(module.getRevision());
      Resource resource = new Resource();
      _logger.debug("Working on [{},{}]...", module.getName(), revisionString);
      ApiDeclaration doc = getApiDeclaration(module.getName(), revisionString, uriInfo, schemaContext, context);

      if (doc != null) {
        resource.setPath(generatePath(uriInfo, module.getName(), revisionString));
        resources.add(resource);
      } else {
        _logger.debug("Could not generate doc for {},{}", module.getName(), revisionString);
      }
    }

    resourceList.setApis(resources);

    return resourceList;
  }

  protected ResourceList createResourceList() {
    ResourceList resourceList = new ResourceList();
    resourceList.setApiVersion(API_VERSION);
    resourceList.setSwaggerVersion(SWAGGER_VERSION);
    return resourceList;
  }

  protected String generatePath(UriInfo uriInfo, String name, String revision) {
    URI uri = uriInfo.getRequestUriBuilder().path(generateCacheKey(name, revision)).build();
    return uri.toASCIIString();
  }

  public ApiDeclaration getApiDeclaration(String module, String revision, UriInfo uriInfo, SchemaContext schemaContext, String context) {
    Date rev = null;
    try {
      rev = SIMPLE_DATE_FORMAT.parse(revision);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
    Module m = schemaContext.findModuleByName(module, rev);
    Preconditions.checkArgument(m != null, "Could not find module by name,revision: " + module + "," + revision);

    return getApiDeclaration(m, rev, uriInfo, context, schemaContext);
  }

  public ApiDeclaration getApiDeclaration(Module module, Date revision, UriInfo uriInfo, String context, SchemaContext schemaContext) {
    String basePath = createBasePathFromUriInfo(uriInfo);

    ApiDeclaration doc = getSwaggerDocSpec(module, basePath, context, schemaContext);
    if (doc != null) {
      return doc;
    }
    return null;
  }

  protected String createBasePathFromUriInfo(UriInfo uriInfo) {
    String portPart = "";
    int port = uriInfo.getBaseUri().getPort();
    if (port != -1) {
      portPart = ":" + port;
    }
    String basePath = new StringBuilder(uriInfo.getBaseUri().getScheme()).append("://")
            .append(uriInfo.getBaseUri().getHost()).append(portPart).append("/").append(RESTCONF_CONTEXT_ROOT)
            .toString();
    return basePath;
  }

  public ApiDeclaration getSwaggerDocSpec(Module m, String basePath, String context, SchemaContext schemaContext) {
    ApiDeclaration doc = createApiDeclaration(basePath);

    List<Api> apis = new ArrayList<Api>();

    Collection<DataSchemaNode> dataSchemaNodes = m.getChildNodes();
    _logger.debug("child nodes size [{}]", dataSchemaNodes.size());
    for (DataSchemaNode node : dataSchemaNodes) {
      if ((node instanceof ListSchemaNode) || (node instanceof ContainerSchemaNode)) {
        _logger.debug("Is Configuration node [{}] [{}]", node.isConfiguration(), node.getQName().getLocalName());

        List<Parameter> pathParams = new ArrayList<Parameter>();
        String resourcePath = getDataStorePath("/config/", context);
        addRootPostLink(m, (DataNodeContainer) node, pathParams, resourcePath, apis);
        addApis(node, apis, resourcePath, pathParams, schemaContext, true);

        pathParams = new ArrayList<Parameter>();
        resourcePath = getDataStorePath("/operational/", context);
        addApis(node, apis, resourcePath, pathParams, schemaContext, false);
      }
    }

    Set<RpcDefinition> rpcs = m.getRpcs();
    for (RpcDefinition rpcDefinition : rpcs) {
      String resourcePath = getDataStorePath("/operations/", context);
      addRpcs(rpcDefinition, apis, resourcePath, schemaContext);
    }

    _logger.debug("Number of APIs found [{}]", apis.size());

    if (!apis.isEmpty()) {
      doc.setApis(apis);
      JSONObject models = null;

      try {
        models = jsonConverter.convertToJsonSchema(m, schemaContext);
        doc.setModels(models);
        if (_logger.isDebugEnabled()) {
          _logger.debug(mapper.writeValueAsString(doc));
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
      apiForRootPostUri.setOperations(operationPost(m.getName() + MODULE_NAME_SUFFIX, m.getDescription(), m, pathParams, true));
      apis.add(apiForRootPostUri);
    }
  }

  protected ApiDeclaration createApiDeclaration(String basePath) {
    ApiDeclaration doc = new ApiDeclaration();
    doc.setApiVersion(API_VERSION);
    doc.setSwaggerVersion(SWAGGER_VERSION);
    doc.setBasePath(basePath);
    doc.setProduces(Arrays.asList("application/json", "application/xml"));
    return doc;
  }

  protected String getDataStorePath(String dataStore, String context) {
    return dataStore + context;
  }

  private String generateCacheKey(Module m) {
    return generateCacheKey(m.getName(), SIMPLE_DATE_FORMAT.format(m.getRevision()));
  }

  private String generateCacheKey(String module, String revision) {
    return module + "(" + revision + ")";
  }

  private void addApis(DataSchemaNode node, List<Api> apis, String parentPath, List<Parameter> parentPathParams, SchemaContext schemaContext,
                       boolean addConfigApi) {

    Api api = new Api();
    List<Parameter> pathParams = new ArrayList<Parameter>(parentPathParams);

    String resourcePath = parentPath + createPath(node, pathParams, schemaContext) + "/";
    _logger.debug("Adding path: [{}]", resourcePath);
    api.setPath(resourcePath);

    Iterable<DataSchemaNode> childSchemaNodes = Collections.<DataSchemaNode>emptySet();
    if ((node instanceof ListSchemaNode) || (node instanceof ContainerSchemaNode)) {
      DataNodeContainer dataNodeContainer = (DataNodeContainer) node;
      childSchemaNodes = dataNodeContainer.getChildNodes();
    }
    api.setOperations(operation(node, pathParams, addConfigApi, childSchemaNodes));
    apis.add(api);


    if (hideChildNodes(node)) {
      //do not include children of this level.
      return;
    }

    Collection<DataSchemaNode> hiddenAugmentedChildNodes = getHiddenAugmentedChildNodes(node);

    for (DataSchemaNode childNode : childSchemaNodes) {

      if (hiddenAugmentedChildNodes.contains(childNode)) {
        continue;
      }

      if (childNode instanceof ListSchemaNode || childNode instanceof ContainerSchemaNode) {
        // keep config and operation attributes separate.
        if (childNode.isConfiguration() == addConfigApi) {
          addApis(childNode, apis, resourcePath, pathParams, schemaContext, addConfigApi);
        }
      }
    }
  }


  private boolean containsListOrContainer(final Iterable<DataSchemaNode> nodes) {
    for (DataSchemaNode child : nodes) {
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
  private List<Operation> operation(DataSchemaNode node, List<Parameter> pathParams, boolean isConfig, Iterable<DataSchemaNode> childSchemaNodes) {
    List<Operation> operations = new ArrayList<>();

    OperationBuilder.Get getBuilder = new OperationBuilder.Get(node, isConfig);
    operations.add(getBuilder.pathParams(pathParams).build());

    if (isConfig) {
      OperationBuilder.Put putBuilder = new OperationBuilder.Put(node.getQName().getLocalName(),
              node.getDescription());
      operations.add(putBuilder.pathParams(pathParams).build());

      OperationBuilder.Delete deleteBuilder = new OperationBuilder.Delete(node);
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

  protected void addRpcs(RpcDefinition rpcDefn, List<Api> apis, String parentPath, SchemaContext schemaContext) {
    Api rpc = new Api();
    String resourcePath = parentPath + resolvePathArgumentsName(rpcDefn, schemaContext);
    rpc.setPath(resourcePath);

    Operation operationSpec = new Operation();
    operationSpec.setMethod("POST");
    operationSpec.setNotes(rpcDefn.getDescription());
    operationSpec.setNickname(rpcDefn.getQName().getLocalName());
    if (rpcDefn.getOutput() != null) {
      operationSpec.setType("(" + rpcDefn.getQName().getLocalName() + ")output");
    }
    if (rpcDefn.getInput() != null) {
      Parameter payload = new Parameter();
      payload.setParamType("body");
      payload.setType("(" + rpcDefn.getQName().getLocalName() + ")input");
      operationSpec.setParameters(Collections.singletonList(payload));
    }

    rpc.setOperations(Arrays.asList(operationSpec));

    apis.add(rpc);
  }

  protected SortedSet<Module> getSortedModules(SchemaContext schemaContext) {
    if (schemaContext == null) {
      return new TreeSet<>();
    }

    Set<Module> modules = schemaContext.getModules();

    SortedSet<Module> sortedModules = new TreeSet<>(new Comparator<Module>() {
      @Override
      public int compare(Module o1, Module o2) {
        int result = o1.getName().compareTo(o2.getName());
        if (result == 0) {
          result = o1.getRevision().compareTo(o2.getRevision());
        }
        if (result == 0) {
          result = o1.getNamespace().compareTo(o2.getNamespace());
        }
        return result;
      }
    });
    for (Module m : modules) {
      if (m != null) {
        //sortedModules.add(m);
        addOrKeepNewest(m, sortedModules);
      }
    }
    return sortedModules;
  }

  /**
   * This method will add the Module m in the set of sorted modules
   * if
   * 1. m is unique
   * OR
   * 2. if m is a newer version of another module same as m.
   * <p/>
   * Else, it will leave the sortedModules set as is.
   *
   * @param m
   * @param sortedModules
   */
  private void addOrKeepNewest(Module m, SortedSet<Module> sortedModules) {

    String moduleName = m.getName();
    Date moduleDate = m.getRevision();
    Module moduleToRemove = null;
    boolean isDuplicate = false;
    for (Module moduleInSet : sortedModules) {
      if (moduleName.equals(moduleInSet.getName())) {
        isDuplicate = true;
        if (moduleDate.after(moduleInSet.getRevision())) {
          sortedModules.add(m);
          moduleToRemove = moduleInSet;
        }
        //break because at a time, there will be only one duplicate max.
        break;
      }
    }

    //remove the older duplicate version
    if (moduleToRemove != null) {
      sortedModules.remove(moduleToRemove);
    }

    //this wasn't a duplicate, so simply add it.
    if (!isDuplicate) {
      sortedModules.add(m);
    }

  }


  /**
   * Returns true if this node is to be hidden,
   * (indicated by the presence of 'children-scope-private' yang ext.
   * @param dataSchemaNode
   * @return
   * @author mayagarw@cisco.com
   */
  private boolean hideChildNodes(DataSchemaNode dataSchemaNode) {

    List<UnknownSchemaNode> unknownNodes = dataSchemaNode.getUnknownSchemaNodes();
    boolean hide = false;
    for (UnknownSchemaNode unknownNode : unknownNodes) {
      if (HIDE_CHILDREN_YANG_EXT.equals(unknownNode.getQName().getLocalName())) {
        hide = true;
      }
    }

    return hide;
  }

  /**
   * Returns true if this module is to be hidden,
   * (indicated by the presence of 'module-scope-private' yang ext.
   * @param moduleNode
   * @return
   * @author mayagarw@cisco.com
   */
  private boolean hideModule(Module moduleNode) {

    List<UnknownSchemaNode> unknownNodes = moduleNode.getUnknownSchemaNodes();
    boolean hide = false;
    for (UnknownSchemaNode unknownNode : unknownNodes) {
      if (HIDE_MODULE_YANG_EXT.equals(unknownNode.getQName().getLocalName())) {
        hide = true;
      }
    }
    return hide;
  }


  /**
   * For this given node, this method returns all the 'augmented' child nodes
   * that are supposed to be hidden in rest-conf doc
   *
   * @param node
   * @return
   * @author mayagarw@cisco.com
   */
  private Collection<DataSchemaNode> getHiddenAugmentedChildNodes(DataSchemaNode node) {
    Collection<DataSchemaNode> hiddenAugmentedChildNodes = new ArrayList<>();

    if (node instanceof ContainerSchemaNode || node instanceof ListSchemaNode) {
      Set<AugmentationSchema> augmentations = ((AugmentationTarget) node).getAvailableAugmentations();
      for (AugmentationSchema augmentation : augmentations) {
        boolean isAugmentationHidden = false;
        List<UnknownSchemaNode> unknownNodes = augmentation.getUnknownSchemaNodes();

        for (UnknownSchemaNode unknownNode : unknownNodes) {
          if (HIDE_CHILDREN_YANG_EXT.equals(unknownNode.getQName().getLocalName())) {
            isAugmentationHidden = true;
          }
        }

        if (isAugmentationHidden) {
          hiddenAugmentedChildNodes.addAll(augmentation.getChildNodes());
        }
      }
    }
    return hiddenAugmentedChildNodes;
  }

}
