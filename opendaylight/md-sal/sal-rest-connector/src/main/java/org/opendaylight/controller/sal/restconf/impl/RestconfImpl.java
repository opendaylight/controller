
package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.StringExtensions;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.rpc.impl.BrokerRpcExecutor;
import org.opendaylight.controller.sal.restconf.rpc.impl.MountPointRpcExecutor;
import org.opendaylight.controller.sal.restconf.rpc.impl.RpcExecutor;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.FeatureDefinition;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.EmptyType;
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.LeafSchemaNodeBuilder;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * RestconfImpl provides an implementation for the Restconf northbound API which provides REST web services
 * for all items stored in MD-SAL datastore. This implementation should follow the standards laid out in the
 * Restconf standard IETF draft.
 *
 *<br><br><b>NOTE: The majority of this code was auto generated via xtend. We are in the process of cleaning it up and
 *porting the xtend implementation to pure java. As modifications are made, please refactor the logic in the given
 *method to clean it up!</b>
 *
 * @author Devin Avery
 * @author xtend generate code
 *

 */
@SuppressWarnings("all")
public class RestconfImpl implements RestconfService {
  private static final int CHAR_NOT_FOUND = -1;

private final static RestconfImpl INSTANCE = new Function0<RestconfImpl>() {
    @Override
    public RestconfImpl apply() {
      RestconfImpl _restconfImpl = new RestconfImpl();
      return _restconfImpl;
    }
  }.apply();

  private final static String MOUNT_POINT_MODULE_NAME = "ietf-netconf";

  private final static SimpleDateFormat REVISION_FORMAT = new Function0<SimpleDateFormat>() {
    @Override
    public SimpleDateFormat apply() {
      SimpleDateFormat _simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
      return _simpleDateFormat;
    }
  }.apply();

  private final static String RESTCONF_MODULE_DRAFT02_REVISION = "2013-10-19";

  private final static String RESTCONF_MODULE_DRAFT02_NAME = "ietf-restconf";

  private final static String RESTCONF_MODULE_DRAFT02_NAMESPACE = "urn:ietf:params:xml:ns:yang:ietf-restconf";

  private final static String RESTCONF_MODULE_DRAFT02_RESTCONF_GROUPING_SCHEMA_NODE = "restconf";

  private final static String RESTCONF_MODULE_DRAFT02_RESTCONF_CONTAINER_SCHEMA_NODE = "restconf";

  private final static String RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE = "modules";

  private final static String RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE = "module";

  private final static String RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE = "streams";

  private final static String RESTCONF_MODULE_DRAFT02_STREAM_LIST_SCHEMA_NODE = "stream";

  private final static String RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE = "operations";

  private final static String SAL_REMOTE_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote";

  private final static String SAL_REMOTE_RPC_SUBSRCIBE = "create-data-change-event-subscription";

  private final static Map<String,Status> RPC_ERROR_TAG_TO_HTTP_STATUS =
                          Collections.unmodifiableMap( new HashMap<String,Status>(){
      {
          this.put( "in-use", Status.fromStatusCode(409));
          this.put( "invalid-value", Status.fromStatusCode(400));
          this.put( "too-big", Status.fromStatusCode(413));
          this.put( "missing-attribute", Status.fromStatusCode(400));
          this.put( "bad-attribute", Status.fromStatusCode(400));
          this.put( "unknown-attribute", Status.fromStatusCode(400));
          this.put( "bad-element", Status.fromStatusCode(400));
          this.put( "unknown-element", Status.fromStatusCode(400));
          this.put( "unknown-namespace", Status.fromStatusCode(400));
          this.put( "access-denied", Status.fromStatusCode(403));
          this.put( "lock-denied", Status.fromStatusCode(409));
          this.put( "resource-denied", Status.fromStatusCode(409));
          this.put( "rollback-failed", Status.fromStatusCode(500));
          this.put( "data-exists", Status.fromStatusCode(409));
          this.put( "data-missing", Status.fromStatusCode(409));
          this.put( "operation-not-supported", Status.fromStatusCode(501));
          this.put( "operation-failed", Status.fromStatusCode(500));
          this.put( "partial-operation", Status.fromStatusCode(500));
          this.put( "malformed-message", Status.fromStatusCode(400));
      }
  });

  private BrokerFacade _broker;

  public BrokerFacade getBroker() {
    return this._broker;
  }

  public void setBroker(final BrokerFacade broker) {
    this._broker = broker;
  }

  @Extension
  private ControllerContext controllerContext;

  public ControllerContext getControllerContext() {
    return this.controllerContext;
  }

  public void setControllerContext(final ControllerContext controllerContext) {
    this.controllerContext = controllerContext;
  }

  private RestconfImpl() {
    boolean _tripleNotEquals = (RestconfImpl.INSTANCE != null);
    if (_tripleNotEquals) {
      IllegalStateException _illegalStateException = new IllegalStateException("Already instantiated");
      throw _illegalStateException;
    }
  }

  public static RestconfImpl getInstance() {
    return RestconfImpl.INSTANCE;
  }

  @Override
public StructuredData getModules() {
    final Module restconfModule = this.getRestconfModule();
    ArrayList<Node<? extends Object>> _arrayList = new ArrayList<Node<?>>();
    final List<Node<?>> modulesAsData = _arrayList;
    final DataSchemaNode moduleSchemaNode = this.getSchemaNode(restconfModule, RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE);
    Set<Module> _allModules = this.controllerContext.getAllModules();
    for (final Module module : _allModules) {
      CompositeNode _moduleCompositeNode = this.toModuleCompositeNode(module, moduleSchemaNode);
      modulesAsData.add(_moduleCompositeNode);
    }
    final DataSchemaNode modulesSchemaNode = this.getSchemaNode(restconfModule, RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE);
    QName _qName = modulesSchemaNode.getQName();
    final CompositeNode modulesNode = NodeFactory.createImmutableCompositeNode(_qName, null, modulesAsData);
    StructuredData _structuredData = new StructuredData(modulesNode, modulesSchemaNode, null);
    return _structuredData;
  }

  @Override
public StructuredData getAvailableStreams() {
    Set<String> availableStreams = Notificator.getStreamNames();
    ArrayList<Node<? extends Object>> _arrayList = new ArrayList<Node<?>>();
    final List<Node<?>> streamsAsData = _arrayList;
    Module _restconfModule = this.getRestconfModule();
    final DataSchemaNode streamSchemaNode = this.getSchemaNode(_restconfModule, RestconfImpl.RESTCONF_MODULE_DRAFT02_STREAM_LIST_SCHEMA_NODE);
    for (final String streamName : availableStreams) {
      CompositeNode _streamCompositeNode = this.toStreamCompositeNode(streamName, streamSchemaNode);
      streamsAsData.add(_streamCompositeNode);
    }
    Module _restconfModule_1 = this.getRestconfModule();
    final DataSchemaNode streamsSchemaNode = this.getSchemaNode(_restconfModule_1, RestconfImpl.RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE);
    QName _qName = streamsSchemaNode.getQName();
    final CompositeNode streamsNode = NodeFactory.createImmutableCompositeNode(_qName, null, streamsAsData);
    StructuredData _structuredData = new StructuredData(streamsNode, streamsSchemaNode, null);
    return _structuredData;
  }

  @Override
public StructuredData getModules(final String identifier) {
    Set<Module> modules = null;
    MountInstance mountPoint = null;
    boolean _contains = identifier.contains(ControllerContext.MOUNT);
    if (_contains) {
      InstanceIdWithSchemaNode _mountPointIdentifier = this.controllerContext.toMountPointIdentifier(identifier);
      MountInstance _mountPoint = _mountPointIdentifier.getMountPoint();
      mountPoint = _mountPoint;
      Set<Module> _allModules = this.controllerContext.getAllModules(mountPoint);
      modules = _allModules;
    } else {
      String _plus = ("URI has bad format. If modules behind mount point should be showed, URI has to end with " + ControllerContext.MOUNT);
      ResponseException _responseException = new ResponseException(Status.BAD_REQUEST, _plus);
      throw _responseException;
    }
    ArrayList<Node<? extends Object>> _arrayList = new ArrayList<Node<?>>();
    final List<Node<?>> modulesAsData = _arrayList;
    Module _restconfModule = this.getRestconfModule();
    final DataSchemaNode moduleSchemaNode = this.getSchemaNode(_restconfModule, RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE);
    for (final Module module : modules) {
      CompositeNode _moduleCompositeNode = this.toModuleCompositeNode(module, moduleSchemaNode);
      modulesAsData.add(_moduleCompositeNode);
    }
    Module _restconfModule_1 = this.getRestconfModule();
    final DataSchemaNode modulesSchemaNode = this.getSchemaNode(_restconfModule_1, RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE);
    QName _qName = modulesSchemaNode.getQName();
    final CompositeNode modulesNode = NodeFactory.createImmutableCompositeNode(_qName, null, modulesAsData);
    StructuredData _structuredData = new StructuredData(modulesNode, modulesSchemaNode, mountPoint);
    return _structuredData;
  }

  @Override
public StructuredData getModule(final String identifier) {
    final QName moduleNameAndRevision = this.getModuleNameAndRevision(identifier);
    Module module = null;
    MountInstance mountPoint = null;
    boolean _contains = identifier.contains(ControllerContext.MOUNT);
    if (_contains) {
      InstanceIdWithSchemaNode _mountPointIdentifier = this.controllerContext.toMountPointIdentifier(identifier);
      MountInstance _mountPoint = _mountPointIdentifier.getMountPoint();
      mountPoint = _mountPoint;
      Module _findModuleByNameAndRevision = this.controllerContext.findModuleByNameAndRevision(mountPoint, moduleNameAndRevision);
      module = _findModuleByNameAndRevision;
    } else {
      Module _findModuleByNameAndRevision_1 = this.controllerContext.findModuleByNameAndRevision(moduleNameAndRevision);
      module = _findModuleByNameAndRevision_1;
    }
    boolean _tripleEquals = (module == null);
    if (_tripleEquals) {
      String _localName = moduleNameAndRevision.getLocalName();
      String _plus = ("Module with name \'" + _localName);
      String _plus_1 = (_plus + "\' and revision \'");
      Date _revision = moduleNameAndRevision.getRevision();
      String _plus_2 = (_plus_1 + _revision);
      String _plus_3 = (_plus_2 + "\' was not found.");
      ResponseException _responseException = new ResponseException(Status.BAD_REQUEST, _plus_3);
      throw _responseException;
    }
    Module _restconfModule = this.getRestconfModule();
    final DataSchemaNode moduleSchemaNode = this.getSchemaNode(_restconfModule, RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE);
    final CompositeNode moduleNode = this.toModuleCompositeNode(module, moduleSchemaNode);
    StructuredData _structuredData = new StructuredData(moduleNode, moduleSchemaNode, mountPoint);
    return _structuredData;
  }

  @Override
public StructuredData getOperations() {
    Set<Module> _allModules = this.controllerContext.getAllModules();
    return this.operationsFromModulesToStructuredData(_allModules, null);
  }

  @Override
public StructuredData getOperations(final String identifier) {
    Set<Module> modules = null;
    MountInstance mountPoint = null;
    boolean _contains = identifier.contains(ControllerContext.MOUNT);
    if (_contains) {
      InstanceIdWithSchemaNode _mountPointIdentifier = this.controllerContext.toMountPointIdentifier(identifier);
      MountInstance _mountPoint = _mountPointIdentifier.getMountPoint();
      mountPoint = _mountPoint;
      Set<Module> _allModules = this.controllerContext.getAllModules(mountPoint);
      modules = _allModules;
    } else {
      String _plus = ("URI has bad format. If operations behind mount point should be showed, URI has to end with " + ControllerContext.MOUNT);
      ResponseException _responseException = new ResponseException(Status.BAD_REQUEST, _plus);
      throw _responseException;
    }
    return this.operationsFromModulesToStructuredData(modules, mountPoint);
  }

  private StructuredData operationsFromModulesToStructuredData(final Set<Module> modules, final MountInstance mountPoint) {
    ArrayList<Node<? extends Object>> _arrayList = new ArrayList<Node<?>>();
    final List<Node<?>> operationsAsData = _arrayList;
    Module _restconfModule = this.getRestconfModule();
    final DataSchemaNode operationsSchemaNode = this.getSchemaNode(_restconfModule, RestconfImpl.RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE);
    QName _qName = operationsSchemaNode.getQName();
    SchemaPath _path = operationsSchemaNode.getPath();
    ContainerSchemaNodeBuilder _containerSchemaNodeBuilder = new ContainerSchemaNodeBuilder(RestconfImpl.RESTCONF_MODULE_DRAFT02_NAME, 0, _qName, _path);
    final ContainerSchemaNodeBuilder fakeOperationsSchemaNode = _containerSchemaNodeBuilder;
    for (final Module module : modules) {
      Set<RpcDefinition> _rpcs = module.getRpcs();
      for (final RpcDefinition rpc : _rpcs) {
        {
          QName _qName_1 = rpc.getQName();
          SimpleNode<Object> _createImmutableSimpleNode = NodeFactory.<Object>createImmutableSimpleNode(_qName_1, null, null);
          operationsAsData.add(_createImmutableSimpleNode);
          String _name = module.getName();
          QName _qName_2 = rpc.getQName();
          LeafSchemaNodeBuilder _leafSchemaNodeBuilder = new LeafSchemaNodeBuilder(_name, 0, _qName_2, null);
          final LeafSchemaNodeBuilder fakeRpcSchemaNode = _leafSchemaNodeBuilder;
          fakeRpcSchemaNode.setAugmenting(true);
          EmptyType _instance = EmptyType.getInstance();
          fakeRpcSchemaNode.setType(_instance);
          LeafSchemaNode _build = fakeRpcSchemaNode.build();
          fakeOperationsSchemaNode.addChildNode(_build);
        }
      }
    }
    QName _qName_1 = operationsSchemaNode.getQName();
    final CompositeNode operationsNode = NodeFactory.createImmutableCompositeNode(_qName_1, null, operationsAsData);
    ContainerSchemaNode _build = fakeOperationsSchemaNode.build();
    StructuredData _structuredData = new StructuredData(operationsNode, _build, mountPoint);
    return _structuredData;
  }

  private Module getRestconfModule() {
    QName _create = QName.create(RestconfImpl.RESTCONF_MODULE_DRAFT02_NAMESPACE, RestconfImpl.RESTCONF_MODULE_DRAFT02_REVISION,
      RestconfImpl.RESTCONF_MODULE_DRAFT02_NAME);
    final Module restconfModule = this.controllerContext.findModuleByNameAndRevision(_create);
    boolean _tripleEquals = (restconfModule == null);
    if (_tripleEquals) {
      ResponseException _responseException = new ResponseException(Status.INTERNAL_SERVER_ERROR, "Restconf module was not found.");
      throw _responseException;
    }
    return restconfModule;
  }

  private QName getModuleNameAndRevision(final String identifier) {
    final int indexOfMountPointFirstLetter = identifier.indexOf(ControllerContext.MOUNT);
    String moduleNameAndRevision = "";
    int _minus = CHAR_NOT_FOUND;
    boolean _tripleNotEquals = (indexOfMountPointFirstLetter != _minus);
    if (_tripleNotEquals) {
      int _length = ControllerContext.MOUNT.length();
      int _plus = (indexOfMountPointFirstLetter + _length);
      String _substring = identifier.substring(_plus);
      moduleNameAndRevision = _substring;
    } else {
      moduleNameAndRevision = identifier;
    }
    Splitter _on = Splitter.on("/");
    Splitter _omitEmptyStrings = _on.omitEmptyStrings();
    Iterable<String> _split = _omitEmptyStrings.split(moduleNameAndRevision);
    final ArrayList<String> pathArgs = Lists.<String>newArrayList(_split);
    int _length_1 = ((Object[])Conversions.unwrapArray(pathArgs, Object.class)).length;
    boolean _lessThan = (_length_1 < 2);
    if (_lessThan) {
      ResponseException _responseException = new ResponseException(Status.BAD_REQUEST,
        "URI has bad format. End of URI should be in format \'moduleName/yyyy-MM-dd\'");
      throw _responseException;
    }
    try {
      final String moduleName = IterableExtensions.<String>head(pathArgs);
      String _get = pathArgs.get(1);
      final Date moduleRevision = RestconfImpl.REVISION_FORMAT.parse(_get);
      return QName.create(null, moduleRevision, moduleName);
    } catch (final Throwable _t) {
      if (_t instanceof ParseException) {
        final ParseException e = (ParseException)_t;
        ResponseException _responseException_1 = new ResponseException(Status.BAD_REQUEST, "URI has bad format. It should be \'moduleName/yyyy-MM-dd\'");
        throw _responseException_1;
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }

  private CompositeNode toStreamCompositeNode(final String streamName, final DataSchemaNode streamSchemaNode) {
    ArrayList<Node<? extends Object>> _arrayList = new ArrayList<Node<?>>();
    final List<Node<?>> streamNodeValues = _arrayList;
    List<DataSchemaNode> _findInstanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) streamSchemaNode), "name");
    final DataSchemaNode nameSchemaNode = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName);
    QName _qName = nameSchemaNode.getQName();
    SimpleNode<String> _createImmutableSimpleNode = NodeFactory.<String>createImmutableSimpleNode(_qName, null, streamName);
    streamNodeValues.add(_createImmutableSimpleNode);
    List<DataSchemaNode> _findInstanceDataChildrenByName_1 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) streamSchemaNode), "description");
    final DataSchemaNode descriptionSchemaNode = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_1);
    QName _qName_1 = descriptionSchemaNode.getQName();
    SimpleNode<String> _createImmutableSimpleNode_1 = NodeFactory.<String>createImmutableSimpleNode(_qName_1, null, "DESCRIPTION_PLACEHOLDER");
    streamNodeValues.add(_createImmutableSimpleNode_1);
    List<DataSchemaNode> _findInstanceDataChildrenByName_2 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) streamSchemaNode), "replay-support");
    final DataSchemaNode replaySupportSchemaNode = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_2);
    QName _qName_2 = replaySupportSchemaNode.getQName();
    SimpleNode<Boolean> _createImmutableSimpleNode_2 = NodeFactory.<Boolean>createImmutableSimpleNode(_qName_2, null, Boolean.valueOf(true));
    streamNodeValues.add(_createImmutableSimpleNode_2);
    List<DataSchemaNode> _findInstanceDataChildrenByName_3 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) streamSchemaNode), "replay-log-creation-time");
    final DataSchemaNode replayLogCreationTimeSchemaNode = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_3);
    QName _qName_3 = replayLogCreationTimeSchemaNode.getQName();
    SimpleNode<String> _createImmutableSimpleNode_3 = NodeFactory.<String>createImmutableSimpleNode(_qName_3, null, "");
    streamNodeValues.add(_createImmutableSimpleNode_3);
    List<DataSchemaNode> _findInstanceDataChildrenByName_4 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) streamSchemaNode), "events");
    final DataSchemaNode eventsSchemaNode = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_4);
    QName _qName_4 = eventsSchemaNode.getQName();
    SimpleNode<String> _createImmutableSimpleNode_4 = NodeFactory.<String>createImmutableSimpleNode(_qName_4, null, "");
    streamNodeValues.add(_createImmutableSimpleNode_4);
    QName _qName_5 = streamSchemaNode.getQName();
    return NodeFactory.createImmutableCompositeNode(_qName_5, null, streamNodeValues);
  }

  private CompositeNode toModuleCompositeNode(final Module module, final DataSchemaNode moduleSchemaNode) {
    ArrayList<Node<? extends Object>> _arrayList = new ArrayList<Node<?>>();
    final List<Node<?>> moduleNodeValues = _arrayList;
    List<DataSchemaNode> _findInstanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) moduleSchemaNode), "name");
    final DataSchemaNode nameSchemaNode = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName);
    QName _qName = nameSchemaNode.getQName();
    String _name = module.getName();
    SimpleNode<String> _createImmutableSimpleNode = NodeFactory.<String>createImmutableSimpleNode(_qName, null, _name);
    moduleNodeValues.add(_createImmutableSimpleNode);
    List<DataSchemaNode> _findInstanceDataChildrenByName_1 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) moduleSchemaNode), "revision");
    final DataSchemaNode revisionSchemaNode = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_1);
    QName _qName_1 = revisionSchemaNode.getQName();
    Date _revision = module.getRevision();
    String _format = RestconfImpl.REVISION_FORMAT.format(_revision);
    SimpleNode<String> _createImmutableSimpleNode_1 = NodeFactory.<String>createImmutableSimpleNode(_qName_1, null, _format);
    moduleNodeValues.add(_createImmutableSimpleNode_1);
    List<DataSchemaNode> _findInstanceDataChildrenByName_2 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) moduleSchemaNode), "namespace");
    final DataSchemaNode namespaceSchemaNode = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_2);
    QName _qName_2 = namespaceSchemaNode.getQName();
    URI _namespace = module.getNamespace();
    String _string = _namespace.toString();
    SimpleNode<String> _createImmutableSimpleNode_2 = NodeFactory.<String>createImmutableSimpleNode(_qName_2, null, _string);
    moduleNodeValues.add(_createImmutableSimpleNode_2);
    List<DataSchemaNode> _findInstanceDataChildrenByName_3 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) moduleSchemaNode), "feature");
    final DataSchemaNode featureSchemaNode = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_3);
    Set<FeatureDefinition> _features = module.getFeatures();
    for (final FeatureDefinition feature : _features) {
      QName _qName_3 = featureSchemaNode.getQName();
      QName _qName_4 = feature.getQName();
      String _localName = _qName_4.getLocalName();
      SimpleNode<String> _createImmutableSimpleNode_3 = NodeFactory.<String>createImmutableSimpleNode(_qName_3, null, _localName);
      moduleNodeValues.add(_createImmutableSimpleNode_3);
    }
    QName _qName_5 = moduleSchemaNode.getQName();
    return NodeFactory.createImmutableCompositeNode(_qName_5, null, moduleNodeValues);
  }

  private DataSchemaNode getSchemaNode(final Module restconfModule, final String schemaNodeName) {
    Set<GroupingDefinition> _groupings = restconfModule.getGroupings();
    final Function1<GroupingDefinition,Boolean> _function = new Function1<GroupingDefinition,Boolean>() {
      @Override
    public Boolean apply(final GroupingDefinition g) {
        QName _qName = g.getQName();
        String _localName = _qName.getLocalName();
        boolean _equals = Objects.equal(_localName, RestconfImpl.RESTCONF_MODULE_DRAFT02_RESTCONF_GROUPING_SCHEMA_NODE);
        return Boolean.valueOf(_equals);
      }
    };
    Iterable<GroupingDefinition> _filter = IterableExtensions.<GroupingDefinition>filter(_groupings, _function);
    final GroupingDefinition restconfGrouping = IterableExtensions.<GroupingDefinition>head(_filter);
    List<DataSchemaNode> _findInstanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(restconfGrouping, RestconfImpl.RESTCONF_MODULE_DRAFT02_RESTCONF_CONTAINER_SCHEMA_NODE);
    final DataSchemaNode restconfContainer = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName);
    boolean _equals = Objects.equal(schemaNodeName, RestconfImpl.RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE);
    if (_equals) {
      List<DataSchemaNode> _findInstanceDataChildrenByName_1 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer), RestconfImpl.RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE);
      return IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_1);
    } else {
      boolean _equals_1 = Objects.equal(schemaNodeName, RestconfImpl.RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE);
      if (_equals_1) {
        List<DataSchemaNode> _findInstanceDataChildrenByName_2 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer), RestconfImpl.RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE);
        return IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_2);
      } else {
        boolean _equals_2 = Objects.equal(schemaNodeName, RestconfImpl.RESTCONF_MODULE_DRAFT02_STREAM_LIST_SCHEMA_NODE);
        if (_equals_2) {
          List<DataSchemaNode> _findInstanceDataChildrenByName_3 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer), RestconfImpl.RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE);
          final DataSchemaNode modules = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_3);
          List<DataSchemaNode> _findInstanceDataChildrenByName_4 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) modules), RestconfImpl.RESTCONF_MODULE_DRAFT02_STREAM_LIST_SCHEMA_NODE);
          return IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_4);
        } else {
          boolean _equals_3 = Objects.equal(schemaNodeName, RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE);
          if (_equals_3) {
            List<DataSchemaNode> _findInstanceDataChildrenByName_5 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer), RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE);
            return IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_5);
          } else {
            boolean _equals_4 = Objects.equal(schemaNodeName, RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE);
            if (_equals_4) {
              List<DataSchemaNode> _findInstanceDataChildrenByName_6 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer), RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE);
              final DataSchemaNode modules_1 = IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_6);
              List<DataSchemaNode> _findInstanceDataChildrenByName_7 = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) modules_1), RestconfImpl.RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE);
              return IterableExtensions.<DataSchemaNode>head(_findInstanceDataChildrenByName_7);
            }
          }
        }
      }
    }
    return null;
  }

  @Override
  public Object getRoot() {
    return null;
  }

  @Override
    public StructuredData invokeRpc(final String identifier, final CompositeNode payload) {
        final RpcExecutor rpc = this.resolveIdentifierInInvokeRpc(identifier);
        QName rpcName = rpc.getRpcDefinition().getQName();
        URI _namespace = rpcName.getNamespace();
        if ( Objects.equal(_namespace.toString(), RestconfImpl.SAL_REMOTE_NAMESPACE) ) {
            if( Objects.equal(rpcName.getLocalName(), RestconfImpl.SAL_REMOTE_RPC_SUBSRCIBE) ){
                return invokeSalRemoteRpcSubscriveRPC(payload, rpc.getRpcDefinition(), rpcName);
            }
        }
        return callRpc(rpc, payload);
    }

    private StructuredData invokeSalRemoteRpcSubscriveRPC(final CompositeNode payload,
            final RpcDefinition rpc, QName rpcName) {
        ContainerSchemaNode _input = rpc.getInput();
        final CompositeNode value = this.normalizeNode(payload, _input, null);
        SimpleNode<? extends Object> _firstSimpleByName = null;
        if (value != null) {
            QName _create = QName.create(rpcName, "path");
            _firstSimpleByName = value.getFirstSimpleByName(_create);
        }
        final SimpleNode<? extends Object> pathNode = _firstSimpleByName;
        Object _value = null;
        if (pathNode != null) {
            _value = pathNode.getValue();
        }
        final Object pathValue = _value;
        boolean _and_1 = false;
        boolean _tripleEquals = (pathValue == null);
        if (!_tripleEquals) {
            _and_1 = false;
        } else {
            boolean _not = (!(pathValue instanceof InstanceIdentifier));
            _and_1 = (_tripleEquals && _not);
        }
        if (_and_1) {
            ResponseException _responseException = new ResponseException(
                    Status.INTERNAL_SERVER_ERROR,
                    "Instance identifier was not normalized correctly.");
            throw _responseException;
        }
        final InstanceIdentifier pathIdentifier = ((InstanceIdentifier) pathValue);
        String streamName = null;
        List<PathArgument> _path = pathIdentifier.getPath();
        boolean _isNullOrEmpty = IterableExtensions.isNullOrEmpty(_path);
        boolean _not_1 = (!_isNullOrEmpty);
        if (_not_1) {
            String _fullRestconfIdentifier = this.controllerContext
                    .toFullRestconfIdentifier(pathIdentifier);
            String _createStreamNameFromUri = Notificator
                    .createStreamNameFromUri(_fullRestconfIdentifier);
            streamName = _createStreamNameFromUri;
        }
        boolean _isNullOrEmpty_1 = StringExtensions.isNullOrEmpty(streamName);
        if (_isNullOrEmpty_1) {
            ResponseException _responseException_1 = new ResponseException(Status.BAD_REQUEST,
                    "Path is empty or contains data node which is not Container or List build-in type.");
            throw _responseException_1;
        }
        ContainerSchemaNode _output = rpc.getOutput();
        QName _qName_3 = _output.getQName();
        QName _create_1 = QName.create(_qName_3, "stream-name");
        final SimpleNode<String> streamNameNode = NodeFactory.<String> createImmutableSimpleNode(
                _create_1, null, streamName);
        ArrayList<Node<? extends Object>> _arrayList = new ArrayList<Node<?>>();
        final List<Node<?>> output = _arrayList;
        output.add(streamNameNode);
        ContainerSchemaNode _output_1 = rpc.getOutput();
        QName _qName_4 = _output_1.getQName();
        final MutableCompositeNode responseData = NodeFactory.createMutableCompositeNode(_qName_4,
                null, output, null, null);
        boolean _existListenerFor = Notificator.existListenerFor(pathIdentifier);
        boolean _not_2 = (!_existListenerFor);
        if (_not_2) {
            Notificator.createListener(pathIdentifier, streamName);
        }
        ContainerSchemaNode _output_2 = rpc.getOutput();
        StructuredData _structuredData = new StructuredData(responseData, _output_2, null);
        return _structuredData;
    }

    @Override
    public StructuredData invokeRpc(final String identifier, final String noPayload) {
        if (StringUtils.isNotBlank(noPayload)) {
            ResponseException _responseException = new ResponseException(
                    Status.UNSUPPORTED_MEDIA_TYPE, "Content-Type contains unsupported Media Type.");
            throw _responseException;
        }
        final RpcExecutor rpc = resolveIdentifierInInvokeRpc(identifier);
        return callRpc(rpc, null);
    }

    private RpcExecutor resolveIdentifierInInvokeRpc(final String identifier) {
        String identifierEncoded = null;
        MountInstance mountPoint = null;
        if (identifier.contains(ControllerContext.MOUNT)) {
            // mounted RPC call - look up mount instance.
            InstanceIdWithSchemaNode _mountPointIdentifier = controllerContext
                    .toMountPointIdentifier(identifier);
            mountPoint = _mountPointIdentifier.getMountPoint();

            int startOfRemoteRpcName = identifier.lastIndexOf(ControllerContext.MOUNT)
                    + ControllerContext.MOUNT.length() + 1;
            String remoteRpcName = identifier.substring(startOfRemoteRpcName);
            identifierEncoded = remoteRpcName;

        } else if (identifier.indexOf("/") != CHAR_NOT_FOUND) {
            final String slashErrorMsg = String
                    .format("Identifier %n%s%ncan\'t contain slash "
                            + "character (/).%nIf slash is part of identifier name then use %%2F placeholder.",
                            identifier);
            throw new ResponseException(Status.NOT_FOUND, slashErrorMsg);
        } else {
            identifierEncoded = identifier;
        }

        final String identifierDecoded = controllerContext.urlPathArgDecode(identifierEncoded);
        RpcDefinition rpc = controllerContext.getRpcDefinition(identifierDecoded);

        if (rpc == null) {
            ResponseException _responseException = new ResponseException(Status.NOT_FOUND,
                    "RPC does not exist.");
            throw _responseException;
        }

        if (mountPoint == null) {
            return new BrokerRpcExecutor(rpc, _broker);
        } else {
            return new MountPointRpcExecutor(rpc, mountPoint);
        }

    }

    private StructuredData callRpc(final RpcExecutor rpcExecutor, final CompositeNode payload) {
        if (rpcExecutor == null) {
            ResponseException _responseException = new ResponseException(Status.NOT_FOUND,
                    "RPC does not exist.");
            throw _responseException;
        }

        CompositeNode rpcRequest = null;
        RpcDefinition rpc = rpcExecutor.getRpcDefinition();
        QName rpcName = rpc.getQName();

        if (payload == null) {
            rpcRequest = NodeFactory.createMutableCompositeNode(rpcName, null, null, null, null);
        } else {
            final CompositeNode value = this.normalizeNode(payload, rpc.getInput(), null);
            List<Node<?>> input = Collections.<Node<?>> singletonList(value);
            rpcRequest = NodeFactory.createMutableCompositeNode(rpcName, null, input, null, null);
        }

        RpcResult<CompositeNode> rpcResult = rpcExecutor.invokeRpc(rpcRequest);

        checkRpcSuccessAndThrowException(rpcResult);

        if (rpcResult.getResult() == null) {
            return null;
        }

        if( rpc.getOutput() == null )
        {
            return null; //no output, nothing to send back.
        }

        return new StructuredData(rpcResult.getResult(), rpc.getOutput(), null);
    }

    private void checkRpcSuccessAndThrowException(RpcResult<CompositeNode> rpcResult) {
        if (rpcResult.isSuccessful() == false) {

            ResponseException exception = null;
            Collection<RpcError> errors = rpcResult.getErrors();
            if( errors != null ){
                for( RpcError error : errors ) {
                    String errorTag = error.getTag();
                    Status errorStatus = RPC_ERROR_TAG_TO_HTTP_STATUS.get( errorTag );
                    if( errorStatus != null ){
                        exception = new ResponseException( errorStatus, error.getMessage() );
                        break;
                    }
                }
            }

            if( exception == null ){
                exception = new ResponseException(Status.INTERNAL_SERVER_ERROR,
                        "The operation was not successful and there were no RPC errors returned");
            }
            throw exception;
        }
    }

  @Override
public StructuredData readConfigurationData(final String identifier) {
    final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
    CompositeNode data = null;
    MountInstance _mountPoint = iiWithData.getMountPoint();
    boolean _tripleNotEquals = (_mountPoint != null);
    if (_tripleNotEquals) {
      BrokerFacade _broker = this.getBroker();
      MountInstance _mountPoint_1 = iiWithData.getMountPoint();
      InstanceIdentifier _instanceIdentifier = iiWithData.getInstanceIdentifier();
      CompositeNode _readConfigurationDataBehindMountPoint = _broker.readConfigurationDataBehindMountPoint(_mountPoint_1, _instanceIdentifier);
      data = _readConfigurationDataBehindMountPoint;
    } else {
      BrokerFacade _broker_1 = this.getBroker();
      InstanceIdentifier _instanceIdentifier_1 = iiWithData.getInstanceIdentifier();
      CompositeNode _readConfigurationData = _broker_1.readConfigurationData(_instanceIdentifier_1);
      data = _readConfigurationData;
    }
    DataSchemaNode _schemaNode = iiWithData.getSchemaNode();
    MountInstance _mountPoint_2 = iiWithData.getMountPoint();
    StructuredData _structuredData = new StructuredData(data, _schemaNode, _mountPoint_2);
    return _structuredData;
  }

  @Override
public StructuredData readOperationalData(final String identifier) {
    final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
    CompositeNode data = null;
    MountInstance _mountPoint = iiWithData.getMountPoint();
    boolean _tripleNotEquals = (_mountPoint != null);
    if (_tripleNotEquals) {
      BrokerFacade _broker = this.getBroker();
      MountInstance _mountPoint_1 = iiWithData.getMountPoint();
      InstanceIdentifier _instanceIdentifier = iiWithData.getInstanceIdentifier();
      CompositeNode _readOperationalDataBehindMountPoint = _broker.readOperationalDataBehindMountPoint(_mountPoint_1, _instanceIdentifier);
      data = _readOperationalDataBehindMountPoint;
    } else {
      BrokerFacade _broker_1 = this.getBroker();
      InstanceIdentifier _instanceIdentifier_1 = iiWithData.getInstanceIdentifier();
      CompositeNode _readOperationalData = _broker_1.readOperationalData(_instanceIdentifier_1);
      data = _readOperationalData;
    }
    DataSchemaNode _schemaNode = iiWithData.getSchemaNode();
    MountInstance _mountPoint_2 = iiWithData.getMountPoint();
    StructuredData _structuredData = new StructuredData(data, _schemaNode, _mountPoint_2);
    return _structuredData;
  }

  @Override
public Response updateConfigurationData(final String identifier, final CompositeNode payload) {
    try {
      Response _xblockexpression = null;
      {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
        DataSchemaNode _schemaNode = iiWithData.getSchemaNode();
        MountInstance _mountPoint = iiWithData.getMountPoint();
        final CompositeNode value = this.normalizeNode(payload, _schemaNode, _mountPoint);
        RpcResult<TransactionStatus> status = null;
        MountInstance _mountPoint_1 = iiWithData.getMountPoint();
        boolean _tripleNotEquals = (_mountPoint_1 != null);
        if (_tripleNotEquals) {
          BrokerFacade _broker = this.getBroker();
          MountInstance _mountPoint_2 = iiWithData.getMountPoint();
          InstanceIdentifier _instanceIdentifier = iiWithData.getInstanceIdentifier();
          Future<RpcResult<TransactionStatus>> _commitConfigurationDataPutBehindMountPoint = _broker.commitConfigurationDataPutBehindMountPoint(_mountPoint_2, _instanceIdentifier, value);
          RpcResult<TransactionStatus> _get = _commitConfigurationDataPutBehindMountPoint.get();
          status = _get;
        } else {
          BrokerFacade _broker_1 = this.getBroker();
          InstanceIdentifier _instanceIdentifier_1 = iiWithData.getInstanceIdentifier();
          Future<RpcResult<TransactionStatus>> _commitConfigurationDataPut = _broker_1.commitConfigurationDataPut(_instanceIdentifier_1, value);
          RpcResult<TransactionStatus> _get_1 = _commitConfigurationDataPut.get();
          status = _get_1;
        }
        Response _switchResult = null;
        TransactionStatus _result = status.getResult();
        final TransactionStatus _switchValue = _result;
        boolean _matched = false;
        if (!_matched) {
          if (Objects.equal(_switchValue,TransactionStatus.COMMITED)) {
            _matched=true;
            ResponseBuilder _status = Response.status(Status.OK);
            Response _build = _status.build();
            _switchResult = _build;
          }
        }
        if (!_matched) {
          ResponseBuilder _status_1 = Response.status(Status.INTERNAL_SERVER_ERROR);
          Response _build_1 = _status_1.build();
          _switchResult = _build_1;
        }
        _xblockexpression = (_switchResult);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  @Override
public Response createConfigurationData(final String identifier, final CompositeNode payload) {
    try {
      Response _xblockexpression = null;
      {
        URI _namespace = this.namespace(payload);
        boolean _tripleEquals = (_namespace == null);
        if (_tripleEquals) {
          ResponseException _responseException = new ResponseException(Status.BAD_REQUEST,
            "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)");
          throw _responseException;
        }
        InstanceIdWithSchemaNode iiWithData = null;
        CompositeNode value = null;
        boolean _representsMountPointRootData = this.representsMountPointRootData(payload);
        if (_representsMountPointRootData) {
          boolean _endsWithMountPoint = this.endsWithMountPoint(identifier);
          if (_endsWithMountPoint) {
            String _plus = ("URI has bad format. URI should be without \"" + ControllerContext.MOUNT);
            String _plus_1 = (_plus + "\" for POST operation.");
            ResponseException _responseException_1 = new ResponseException(Status.BAD_REQUEST, _plus_1);
            throw _responseException_1;
          }
          final String completIdentifier = this.addMountPointIdentifier(identifier);
          InstanceIdWithSchemaNode _instanceIdentifier = this.controllerContext.toInstanceIdentifier(completIdentifier);
          iiWithData = _instanceIdentifier;
          DataSchemaNode _schemaNode = iiWithData.getSchemaNode();
          MountInstance _mountPoint = iiWithData.getMountPoint();
          CompositeNode _normalizeNode = this.normalizeNode(payload, _schemaNode, _mountPoint);
          value = _normalizeNode;
        } else {
          final InstanceIdWithSchemaNode uncompleteInstIdWithData = this.controllerContext.toInstanceIdentifier(identifier);
          DataSchemaNode _schemaNode_1 = uncompleteInstIdWithData.getSchemaNode();
          final DataNodeContainer parentSchema = ((DataNodeContainer) _schemaNode_1);
          MountInstance _mountPoint_1 = uncompleteInstIdWithData.getMountPoint();
          final Module module = this.findModule(_mountPoint_1, payload);
          boolean _tripleEquals_1 = (module == null);
          if (_tripleEquals_1) {
            URI _namespace_1 = this.namespace(payload);
            String _plus_2 = ("Module was not found for \"" + _namespace_1);
            String _plus_3 = (_plus_2 + "\"");
            ResponseException _responseException_2 = new ResponseException(Status.BAD_REQUEST, _plus_3);
            throw _responseException_2;
          }
          String _name = this.getName(payload);
          URI _namespace_2 = module.getNamespace();
          final DataSchemaNode schemaNode = this.controllerContext.findInstanceDataChildByNameAndNamespace(parentSchema, _name, _namespace_2);
          MountInstance _mountPoint_2 = uncompleteInstIdWithData.getMountPoint();
          CompositeNode _normalizeNode_1 = this.normalizeNode(payload, schemaNode, _mountPoint_2);
          value = _normalizeNode_1;
          InstanceIdWithSchemaNode _addLastIdentifierFromData = this.addLastIdentifierFromData(uncompleteInstIdWithData, value, schemaNode);
          iiWithData = _addLastIdentifierFromData;
        }
        RpcResult<TransactionStatus> status = null;
        MountInstance _mountPoint_3 = iiWithData.getMountPoint();
        boolean _tripleNotEquals = (_mountPoint_3 != null);
        if (_tripleNotEquals) {
          BrokerFacade _broker = this.getBroker();
          MountInstance _mountPoint_4 = iiWithData.getMountPoint();
          InstanceIdentifier _instanceIdentifier_1 = iiWithData.getInstanceIdentifier();
          Future<RpcResult<TransactionStatus>> _commitConfigurationDataPostBehindMountPoint = _broker.commitConfigurationDataPostBehindMountPoint(_mountPoint_4, _instanceIdentifier_1, value);
          RpcResult<TransactionStatus> _get = null;
          if (_commitConfigurationDataPostBehindMountPoint!=null) {
            _get=_commitConfigurationDataPostBehindMountPoint.get();
          }
          status = _get;
        } else {
          BrokerFacade _broker_1 = this.getBroker();
          InstanceIdentifier _instanceIdentifier_2 = iiWithData.getInstanceIdentifier();
          Future<RpcResult<TransactionStatus>> _commitConfigurationDataPost = _broker_1.commitConfigurationDataPost(_instanceIdentifier_2, value);
          RpcResult<TransactionStatus> _get_1 = null;
          if (_commitConfigurationDataPost!=null) {
            _get_1=_commitConfigurationDataPost.get();
          }
          status = _get_1;
        }
        boolean _tripleEquals_2 = (status == null);
        if (_tripleEquals_2) {
          ResponseBuilder _status = Response.status(Status.ACCEPTED);
          return _status.build();
        }
        Response _switchResult = null;
        TransactionStatus _result = status.getResult();
        final TransactionStatus _switchValue = _result;
        boolean _matched = false;
        if (!_matched) {
          if (Objects.equal(_switchValue,TransactionStatus.COMMITED)) {
            _matched=true;
            ResponseBuilder _status_1 = Response.status(Status.NO_CONTENT);
            Response _build = _status_1.build();
            _switchResult = _build;
          }
        }
        if (!_matched) {
          ResponseBuilder _status_2 = Response.status(Status.INTERNAL_SERVER_ERROR);
          Response _build_1 = _status_2.build();
          _switchResult = _build_1;
        }
        _xblockexpression = (_switchResult);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  @Override
public Response createConfigurationData(final CompositeNode payload) {
    try {
      Response _xblockexpression = null;
      {
        URI _namespace = this.namespace(payload);
        boolean _tripleEquals = (_namespace == null);
        if (_tripleEquals) {
          ResponseException _responseException = new ResponseException(Status.BAD_REQUEST,
            "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)");
          throw _responseException;
        }
        final Module module = this.findModule(null, payload);
        boolean _tripleEquals_1 = (module == null);
        if (_tripleEquals_1) {
          ResponseException _responseException_1 = new ResponseException(Status.BAD_REQUEST,
            "Data has bad format. Root element node has incorrect namespace (XML format) or module name(JSON format)");
          throw _responseException_1;
        }
        String _name = this.getName(payload);
        URI _namespace_1 = module.getNamespace();
        final DataSchemaNode schemaNode = this.controllerContext.findInstanceDataChildByNameAndNamespace(module, _name, _namespace_1);
        final CompositeNode value = this.normalizeNode(payload, schemaNode, null);
        final InstanceIdWithSchemaNode iiWithData = this.addLastIdentifierFromData(null, value, schemaNode);
        RpcResult<TransactionStatus> status = null;
        MountInstance _mountPoint = iiWithData.getMountPoint();
        boolean _tripleNotEquals = (_mountPoint != null);
        if (_tripleNotEquals) {
          BrokerFacade _broker = this.getBroker();
          MountInstance _mountPoint_1 = iiWithData.getMountPoint();
          InstanceIdentifier _instanceIdentifier = iiWithData.getInstanceIdentifier();
          Future<RpcResult<TransactionStatus>> _commitConfigurationDataPostBehindMountPoint = _broker.commitConfigurationDataPostBehindMountPoint(_mountPoint_1, _instanceIdentifier, value);
          RpcResult<TransactionStatus> _get = null;
          if (_commitConfigurationDataPostBehindMountPoint!=null) {
            _get=_commitConfigurationDataPostBehindMountPoint.get();
          }
          status = _get;
        } else {
          BrokerFacade _broker_1 = this.getBroker();
          InstanceIdentifier _instanceIdentifier_1 = iiWithData.getInstanceIdentifier();
          Future<RpcResult<TransactionStatus>> _commitConfigurationDataPost = _broker_1.commitConfigurationDataPost(_instanceIdentifier_1, value);
          RpcResult<TransactionStatus> _get_1 = null;
          if (_commitConfigurationDataPost!=null) {
            _get_1=_commitConfigurationDataPost.get();
          }
          status = _get_1;
        }
        boolean _tripleEquals_2 = (status == null);
        if (_tripleEquals_2) {
          ResponseBuilder _status = Response.status(Status.ACCEPTED);
          return _status.build();
        }
        Response _switchResult = null;
        TransactionStatus _result = status.getResult();
        final TransactionStatus _switchValue = _result;
        boolean _matched = false;
        if (!_matched) {
          if (Objects.equal(_switchValue,TransactionStatus.COMMITED)) {
            _matched=true;
            ResponseBuilder _status_1 = Response.status(Status.NO_CONTENT);
            Response _build = _status_1.build();
            _switchResult = _build;
          }
        }
        if (!_matched) {
          ResponseBuilder _status_2 = Response.status(Status.INTERNAL_SERVER_ERROR);
          Response _build_1 = _status_2.build();
          _switchResult = _build_1;
        }
        _xblockexpression = (_switchResult);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  @Override
public Response deleteConfigurationData(final String identifier) {
    try {
      Response _xblockexpression = null;
      {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
        RpcResult<TransactionStatus> status = null;
        MountInstance _mountPoint = iiWithData.getMountPoint();
        boolean _tripleNotEquals = (_mountPoint != null);
        if (_tripleNotEquals) {
          BrokerFacade _broker = this.getBroker();
          MountInstance _mountPoint_1 = iiWithData.getMountPoint();
          InstanceIdentifier _instanceIdentifier = iiWithData.getInstanceIdentifier();
          Future<RpcResult<TransactionStatus>> _commitConfigurationDataDeleteBehindMountPoint = _broker.commitConfigurationDataDeleteBehindMountPoint(_mountPoint_1, _instanceIdentifier);
          RpcResult<TransactionStatus> _get = _commitConfigurationDataDeleteBehindMountPoint.get();
          status = _get;
        } else {
          BrokerFacade _broker_1 = this.getBroker();
          InstanceIdentifier _instanceIdentifier_1 = iiWithData.getInstanceIdentifier();
          Future<RpcResult<TransactionStatus>> _commitConfigurationDataDelete = _broker_1.commitConfigurationDataDelete(_instanceIdentifier_1);
          RpcResult<TransactionStatus> _get_1 = _commitConfigurationDataDelete.get();
          status = _get_1;
        }
        Response _switchResult = null;
        TransactionStatus _result = status.getResult();
        final TransactionStatus _switchValue = _result;
        boolean _matched = false;
        if (!_matched) {
          if (Objects.equal(_switchValue,TransactionStatus.COMMITED)) {
            _matched=true;
            ResponseBuilder _status = Response.status(Status.OK);
            Response _build = _status.build();
            _switchResult = _build;
          }
        }
        if (!_matched) {
          ResponseBuilder _status_1 = Response.status(Status.INTERNAL_SERVER_ERROR);
          Response _build_1 = _status_1.build();
          _switchResult = _build_1;
        }
        _xblockexpression = (_switchResult);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  @Override
public Response subscribeToStream(final String identifier, final UriInfo uriInfo) {
    final String streamName = Notificator.createStreamNameFromUri(identifier);
    boolean _isNullOrEmpty = StringExtensions.isNullOrEmpty(streamName);
    if (_isNullOrEmpty) {
      ResponseException _responseException = new ResponseException(Status.BAD_REQUEST, "Stream name is empty.");
      throw _responseException;
    }
    final ListenerAdapter listener = Notificator.getListenerFor(streamName);
    boolean _tripleEquals = (listener == null);
    if (_tripleEquals) {
      ResponseException _responseException_1 = new ResponseException(Status.BAD_REQUEST, "Stream was not found.");
      throw _responseException_1;
    }
    BrokerFacade _broker = this.getBroker();
    _broker.registerToListenDataChanges(listener);
    final UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
    UriBuilder _port = uriBuilder.port(WebSocketServer.PORT);
    UriBuilder _replacePath = _port.replacePath(streamName);
    final URI uriToWebsocketServer = _replacePath.build();
    ResponseBuilder _status = Response.status(Status.OK);
    ResponseBuilder _location = _status.location(uriToWebsocketServer);
    return _location.build();
  }

  private URI _namespace(final CompositeNode data) {
    QName _nodeType = data.getNodeType();
    return _nodeType.getNamespace();
  }

  private URI _namespace(final CompositeNodeWrapper data) {
    return data.getNamespace();
  }

  private String _localName(final CompositeNode data) {
    QName _nodeType = data.getNodeType();
    return _nodeType.getLocalName();
  }

  private String _localName(final CompositeNodeWrapper data) {
    return data.getLocalName();
  }

  private Module _findModule(final MountInstance mountPoint, final CompositeNode data) {
    boolean _tripleNotEquals = (mountPoint != null);
    if (_tripleNotEquals) {
      QName _nodeType = data.getNodeType();
      URI _namespace = _nodeType.getNamespace();
      return this.controllerContext.findModuleByNamespace(mountPoint, _namespace);
    } else {
      QName _nodeType_1 = data.getNodeType();
      URI _namespace_1 = _nodeType_1.getNamespace();
      return this.controllerContext.findModuleByNamespace(_namespace_1);
    }
  }

  private Module _findModule(final MountInstance mountPoint, final CompositeNodeWrapper data) {
    URI _namespace = data.getNamespace();
    Preconditions.<URI>checkNotNull(_namespace);
    Module module = null;
    boolean _tripleNotEquals = (mountPoint != null);
    if (_tripleNotEquals) {
      URI _namespace_1 = data.getNamespace();
      Module _findModuleByNamespace = this.controllerContext.findModuleByNamespace(mountPoint, _namespace_1);
      module = _findModuleByNamespace;
      boolean _tripleEquals = (module == null);
      if (_tripleEquals) {
        URI _namespace_2 = data.getNamespace();
        String _string = _namespace_2.toString();
        Module _findModuleByName = this.controllerContext.findModuleByName(mountPoint, _string);
        module = _findModuleByName;
      }
    } else {
      URI _namespace_3 = data.getNamespace();
      Module _findModuleByNamespace_1 = this.controllerContext.findModuleByNamespace(_namespace_3);
      module = _findModuleByNamespace_1;
      boolean _tripleEquals_1 = (module == null);
      if (_tripleEquals_1) {
        URI _namespace_4 = data.getNamespace();
        String _string_1 = _namespace_4.toString();
        Module _findModuleByName_1 = this.controllerContext.findModuleByName(_string_1);
        module = _findModuleByName_1;
      }
    }
    return module;
  }

  private String _getName(final CompositeNode data) {
    QName _nodeType = data.getNodeType();
    return _nodeType.getLocalName();
  }

  private String _getName(final CompositeNodeWrapper data) {
    return data.getLocalName();
  }

  private InstanceIdWithSchemaNode addLastIdentifierFromData(final InstanceIdWithSchemaNode identifierWithSchemaNode, final CompositeNode data, final DataSchemaNode schemaOfData) {
    InstanceIdentifier _instanceIdentifier = null;
    if (identifierWithSchemaNode!=null) {
      _instanceIdentifier=identifierWithSchemaNode.getInstanceIdentifier();
    }
    final InstanceIdentifier iiOriginal = _instanceIdentifier;
    InstanceIdentifierBuilder iiBuilder = null;
    boolean _tripleEquals = (iiOriginal == null);
    if (_tripleEquals) {
      InstanceIdentifierBuilder _builder = InstanceIdentifier.builder();
      iiBuilder = _builder;
    } else {
      InstanceIdentifierBuilder _builder_1 = InstanceIdentifier.builder(iiOriginal);
      iiBuilder = _builder_1;
    }
    if ((schemaOfData instanceof ListSchemaNode)) {
      QName _qName = schemaOfData.getQName();
      HashMap<QName,Object> _resolveKeysFromData = this.resolveKeysFromData(((ListSchemaNode) schemaOfData), data);
      iiBuilder.nodeWithKey(_qName, _resolveKeysFromData);
    } else {
      QName _qName_1 = schemaOfData.getQName();
      iiBuilder.node(_qName_1);
    }
    InstanceIdentifier _instance = iiBuilder.toInstance();
    MountInstance _mountPoint = null;
    if (identifierWithSchemaNode!=null) {
      _mountPoint=identifierWithSchemaNode.getMountPoint();
    }
    InstanceIdWithSchemaNode _instanceIdWithSchemaNode = new InstanceIdWithSchemaNode(_instance, schemaOfData, _mountPoint);
    return _instanceIdWithSchemaNode;
  }

  private HashMap<QName,Object> resolveKeysFromData(final ListSchemaNode listNode, final CompositeNode dataNode) {
    HashMap<QName,Object> _hashMap = new HashMap<QName, Object>();
    final HashMap<QName,Object> keyValues = _hashMap;
    List<QName> _keyDefinition = listNode.getKeyDefinition();
    for (final QName key : _keyDefinition) {
      {
        SimpleNode<? extends Object> _head = null;
        String _localName = key.getLocalName();
        List<SimpleNode<? extends Object>> _simpleNodesByName = dataNode.getSimpleNodesByName(_localName);
        if (_simpleNodesByName!=null) {
          _head=IterableExtensions.<SimpleNode<? extends Object>>head(_simpleNodesByName);
        }
        Object _value = null;
        if (_head!=null) {
          _value=_head.getValue();
        }
        final Object dataNodeKeyValueObject = _value;
        boolean _tripleEquals = (dataNodeKeyValueObject == null);
        if (_tripleEquals) {
          QName _nodeType = dataNode.getNodeType();
          String _localName_1 = _nodeType.getLocalName();
          String _plus = ("Data contains list \"" + _localName_1);
          String _plus_1 = (_plus + "\" which does not contain key: \"");
          String _localName_2 = key.getLocalName();
          String _plus_2 = (_plus_1 + _localName_2);
          String _plus_3 = (_plus_2 + "\"");
          ResponseException _responseException = new ResponseException(Status.BAD_REQUEST, _plus_3);
          throw _responseException;
        }
        keyValues.put(key, dataNodeKeyValueObject);
      }
    }
    return keyValues;
  }

  private boolean endsWithMountPoint(final String identifier) {
    boolean _or = false;
    boolean _endsWith = identifier.endsWith(ControllerContext.MOUNT);
    if (_endsWith) {
      _or = true;
    } else {
      String _plus = (ControllerContext.MOUNT + "/");
      boolean _endsWith_1 = identifier.endsWith(_plus);
      _or = (_endsWith || _endsWith_1);
    }
    return _or;
  }

  private boolean representsMountPointRootData(final CompositeNode data) {
    boolean _and = false;
    boolean _or = false;
    URI _namespace = this.namespace(data);
    URI _namespace_1 = SchemaContext.NAME.getNamespace();
    boolean _equals = Objects.equal(_namespace, _namespace_1);
    if (_equals) {
      _or = true;
    } else {
      URI _namespace_2 = this.namespace(data);
      boolean _equals_1 = Objects.equal(_namespace_2, RestconfImpl.MOUNT_POINT_MODULE_NAME);
      _or = (_equals || _equals_1);
    }
    if (!_or) {
      _and = false;
    } else {
      String _localName = this.localName(data);
      String _localName_1 = SchemaContext.NAME.getLocalName();
      boolean _equals_2 = Objects.equal(_localName, _localName_1);
      _and = (_or && _equals_2);
    }
    return _and;
  }

  private String addMountPointIdentifier(final String identifier) {
    boolean _endsWith = identifier.endsWith("/");
    if (_endsWith) {
      return (identifier + ControllerContext.MOUNT);
    }
    String _plus = (identifier + "/");
    return (_plus + ControllerContext.MOUNT);
  }

  private CompositeNode normalizeNode(final CompositeNode node, final DataSchemaNode schema, final MountInstance mountPoint) {
    boolean _tripleEquals = (schema == null);
    if (_tripleEquals) {
      QName _nodeType = null;
      if (node!=null) {
        _nodeType=node.getNodeType();
      }
      String _localName = null;
      if (_nodeType!=null) {
        _localName=_nodeType.getLocalName();
      }
      String _plus = ("Data schema node was not found for " + _localName);
      ResponseException _responseException = new ResponseException(Status.INTERNAL_SERVER_ERROR, _plus);
      throw _responseException;
    }
    boolean _not = (!(schema instanceof DataNodeContainer));
    if (_not) {
      ResponseException _responseException_1 = new ResponseException(Status.BAD_REQUEST, "Root element has to be container or list yang datatype.");
      throw _responseException_1;
    }
    if ((node instanceof CompositeNodeWrapper)) {
      boolean _isChangeAllowed = ((CompositeNodeWrapper) node).isChangeAllowed();
      if (_isChangeAllowed) {
        try {
          this.normalizeNode(((CompositeNodeWrapper) node), schema, null, mountPoint);
        } catch (final Throwable _t) {
          if (_t instanceof NumberFormatException) {
            final NumberFormatException e = (NumberFormatException)_t;
            String _message = e.getMessage();
            ResponseException _responseException_2 = new ResponseException(Status.BAD_REQUEST, _message);
            throw _responseException_2;
          } else {
            throw Exceptions.sneakyThrow(_t);
          }
        }
      }
      return ((CompositeNodeWrapper) node).unwrap();
    }
    return node;
  }

  private void normalizeNode(final NodeWrapper<? extends Object> nodeBuilder, final DataSchemaNode schema, final QName previousAugment, final MountInstance mountPoint) {
    boolean _tripleEquals = (schema == null);
    if (_tripleEquals) {
      String _localName = nodeBuilder.getLocalName();
      String _plus = ("Data has bad format.\n\"" + _localName);
      String _plus_1 = (_plus + "\" does not exist in yang schema.");
      ResponseException _responseException = new ResponseException(Status.BAD_REQUEST, _plus_1);
      throw _responseException;
    }
    QName currentAugment = null;
    QName _qname = nodeBuilder.getQname();
    boolean _tripleNotEquals = (_qname != null);
    if (_tripleNotEquals) {
      currentAugment = previousAugment;
    } else {
      QName _normalizeNodeName = this.normalizeNodeName(nodeBuilder, schema, previousAugment, mountPoint);
      currentAugment = _normalizeNodeName;
      QName _qname_1 = nodeBuilder.getQname();
      boolean _tripleEquals_1 = (_qname_1 == null);
      if (_tripleEquals_1) {
        String _localName_1 = nodeBuilder.getLocalName();
        String _plus_2 = ("Data has bad format.\nIf data is in XML format then namespace for \"" + _localName_1);
        String _plus_3 = (_plus_2 +
          "\" should be \"");
        QName _qName = schema.getQName();
        URI _namespace = _qName.getNamespace();
        String _plus_4 = (_plus_3 + _namespace);
        String _plus_5 = (_plus_4 + "\".\n");
        String _plus_6 = (_plus_5 +
          "If data is in JSON format then module name for \"");
        String _localName_2 = nodeBuilder.getLocalName();
        String _plus_7 = (_plus_6 + _localName_2);
        String _plus_8 = (_plus_7 +
          "\" should be corresponding to namespace \"");
        QName _qName_1 = schema.getQName();
        URI _namespace_1 = _qName_1.getNamespace();
        String _plus_9 = (_plus_8 + _namespace_1);
        String _plus_10 = (_plus_9 + "\".");
        ResponseException _responseException_1 = new ResponseException(Status.BAD_REQUEST, _plus_10);
        throw _responseException_1;
      }
    }
    if ((nodeBuilder instanceof CompositeNodeWrapper)) {
      final List<NodeWrapper<?>> children = ((CompositeNodeWrapper) nodeBuilder).getValues();
      for (final NodeWrapper<? extends Object> child : children) {
        {
          String _localName_3 = child.getLocalName();
          final List<DataSchemaNode> potentialSchemaNodes = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) schema), _localName_3);
          boolean _and = false;
          int _size = potentialSchemaNodes.size();
          boolean _greaterThan = (_size > 1);
          if (!_greaterThan) {
            _and = false;
          } else {
            URI _namespace_2 = child.getNamespace();
            boolean _tripleEquals_2 = (_namespace_2 == null);
            _and = (_greaterThan && _tripleEquals_2);
          }
          if (_and) {
            StringBuilder _stringBuilder = new StringBuilder();
            final StringBuilder namespacesOfPotentialModules = _stringBuilder;
            for (final DataSchemaNode potentialSchemaNode : potentialSchemaNodes) {
              StringBuilder _append = namespacesOfPotentialModules.append("   ");
              QName _qName_2 = potentialSchemaNode.getQName();
              URI _namespace_3 = _qName_2.getNamespace();
              String _string = _namespace_3.toString();
              StringBuilder _append_1 = _append.append(_string);
              _append_1.append("\n");
            }
            String _localName_4 = child.getLocalName();
            String _plus_11 = ("Node \"" + _localName_4);
            String _plus_12 = (_plus_11 + "\" is added as augment from more than one module. ");
            String _plus_13 = (_plus_12 + "Therefore node must have namespace (XML format) or module name (JSON format).");
            String _plus_14 = (_plus_13 + "\nThe node is added as augment from modules with namespaces:\n");
            String _plus_15 = (_plus_14 + namespacesOfPotentialModules);
            ResponseException _responseException_2 = new ResponseException(Status.BAD_REQUEST, _plus_15);
            throw _responseException_2;
          }
          boolean rightNodeSchemaFound = false;
          for (final DataSchemaNode potentialSchemaNode_1 : potentialSchemaNodes) {
            boolean _not = (!rightNodeSchemaFound);
            if (_not) {
              final QName potentialCurrentAugment = this.normalizeNodeName(child, potentialSchemaNode_1, currentAugment, mountPoint);
              QName _qname_2 = child.getQname();
              boolean _tripleNotEquals_1 = (_qname_2 != null);
              if (_tripleNotEquals_1) {
                this.normalizeNode(child, potentialSchemaNode_1, potentialCurrentAugment, mountPoint);
                rightNodeSchemaFound = true;
              }
            }
          }
          boolean _not_1 = (!rightNodeSchemaFound);
          if (_not_1) {
            String _localName_5 = child.getLocalName();
            String _plus_16 = ("Schema node \"" + _localName_5);
            String _plus_17 = (_plus_16 + "\" was not found in module.");
            ResponseException _responseException_3 = new ResponseException(Status.BAD_REQUEST, _plus_17);
            throw _responseException_3;
          }
        }
      }
      if ((schema instanceof ListSchemaNode)) {
        final List<QName> listKeys = ((ListSchemaNode) schema).getKeyDefinition();
        for (final QName listKey : listKeys) {
          {
            boolean foundKey = false;
            for (final NodeWrapper<? extends Object> child_1 : children) {
              Node<? extends Object> _unwrap = child_1.unwrap();
              QName _nodeType = _unwrap.getNodeType();
              String _localName_3 = _nodeType.getLocalName();
              String _localName_4 = listKey.getLocalName();
              boolean _equals = Objects.equal(_localName_3, _localName_4);
              if (_equals) {
                foundKey = true;
              }
            }
            boolean _not = (!foundKey);
            if (_not) {
              String _localName_5 = listKey.getLocalName();
              String _plus_11 = ("Missing key in URI \"" + _localName_5);
              String _plus_12 = (_plus_11 + "\" of list \"");
              QName _qName_2 = schema.getQName();
              String _localName_6 = _qName_2.getLocalName();
              String _plus_13 = (_plus_12 + _localName_6);
              String _plus_14 = (_plus_13 +
                "\"");
              ResponseException _responseException_2 = new ResponseException(Status.BAD_REQUEST, _plus_14);
              throw _responseException_2;
            }
          }
        }
      }
    } else {
      if ((nodeBuilder instanceof SimpleNodeWrapper)) {
        final SimpleNodeWrapper simpleNode = ((SimpleNodeWrapper) nodeBuilder);
        final Object value = simpleNode.getValue();
        Object inputValue = value;
        TypeDefinition<? extends Object> _typeDefinition = this.typeDefinition(schema);
        if ((_typeDefinition instanceof IdentityrefTypeDefinition)) {
          if ((value instanceof String)) {
            URI _namespace_2 = nodeBuilder.getNamespace();
            String _string = _namespace_2.toString();
            IdentityValuesDTO _identityValuesDTO = new IdentityValuesDTO(_string, ((String) value), null, ((String) value));
            inputValue = _identityValuesDTO;
          }
        }
        TypeDefinition<? extends Object> _typeDefinition_1 = this.typeDefinition(schema);
        Codec<Object,Object> _from = RestCodec.from(_typeDefinition_1, mountPoint);
        Object _deserialize = null;
        if (_from!=null) {
          _deserialize=_from.deserialize(inputValue);
        }
        final Object outputValue = _deserialize;
        simpleNode.setValue(outputValue);
      } else {
        if ((nodeBuilder instanceof EmptyNodeWrapper)) {
          final EmptyNodeWrapper emptyNodeBuilder = ((EmptyNodeWrapper) nodeBuilder);
          if ((schema instanceof LeafSchemaNode)) {
            emptyNodeBuilder.setComposite(false);
          } else {
            if ((schema instanceof ContainerSchemaNode)) {
              emptyNodeBuilder.setComposite(true);
            }
          }
        }
      }
    }
  }

  private TypeDefinition<? extends Object> _typeDefinition(final LeafSchemaNode node) {
    TypeDefinition<? extends Object> _xblockexpression = null;
    {
      TypeDefinition<? extends Object> baseType = node.getType();
      TypeDefinition<? extends Object> _baseType = baseType.getBaseType();
      boolean _tripleNotEquals = (_baseType != null);
      boolean _while = _tripleNotEquals;
      while (_while) {
        TypeDefinition<? extends Object> _baseType_1 = baseType.getBaseType();
        baseType = _baseType_1;
        TypeDefinition<? extends Object> _baseType_2 = baseType.getBaseType();
        boolean _tripleNotEquals_1 = (_baseType_2 != null);
        _while = _tripleNotEquals_1;
      }
      _xblockexpression = (baseType);
    }
    return _xblockexpression;
  }

  private TypeDefinition<? extends Object> _typeDefinition(final LeafListSchemaNode node) {
    TypeDefinition<? extends Object> _xblockexpression = null;
    {
      TypeDefinition<?> baseType = node.getType();
      TypeDefinition<? extends Object> _baseType = baseType.getBaseType();
      boolean _tripleNotEquals = (_baseType != null);
      boolean _while = _tripleNotEquals;
      while (_while) {
        TypeDefinition<? extends Object> _baseType_1 = baseType.getBaseType();
        baseType = _baseType_1;
        TypeDefinition<? extends Object> _baseType_2 = baseType.getBaseType();
        boolean _tripleNotEquals_1 = (_baseType_2 != null);
        _while = _tripleNotEquals_1;
      }
      _xblockexpression = (baseType);
    }
    return _xblockexpression;
  }

  private QName normalizeNodeName(final NodeWrapper<? extends Object> nodeBuilder, final DataSchemaNode schema, final QName previousAugment, final MountInstance mountPoint) {
    QName validQName = schema.getQName();
    QName currentAugment = previousAugment;
    boolean _isAugmenting = schema.isAugmenting();
    if (_isAugmenting) {
      QName _qName = schema.getQName();
      currentAugment = _qName;
    } else {
      boolean _and = false;
      boolean _tripleNotEquals = (previousAugment != null);
      if (!_tripleNotEquals) {
        _and = false;
      } else {
        QName _qName_1 = schema.getQName();
        URI _namespace = _qName_1.getNamespace();
        URI _namespace_1 = previousAugment.getNamespace();
        boolean _tripleNotEquals_1 = (_namespace != _namespace_1);
        _and = (_tripleNotEquals && _tripleNotEquals_1);
      }
      if (_and) {
        QName _qName_2 = schema.getQName();
        String _localName = _qName_2.getLocalName();
        QName _create = QName.create(currentAugment, _localName);
        validQName = _create;
      }
    }
    String moduleName = null;
    boolean _tripleEquals = (mountPoint == null);
    if (_tripleEquals) {
      ControllerContext _controllerContext = this.getControllerContext();
      URI _namespace_2 = validQName.getNamespace();
      String _findModuleNameByNamespace = _controllerContext.findModuleNameByNamespace(_namespace_2);
      moduleName = _findModuleNameByNamespace;
    } else {
      ControllerContext _controllerContext_1 = this.getControllerContext();
      URI _namespace_3 = validQName.getNamespace();
      String _findModuleNameByNamespace_1 = _controllerContext_1.findModuleNameByNamespace(mountPoint, _namespace_3);
      moduleName = _findModuleNameByNamespace_1;
    }
    boolean _or = false;
    boolean _or_1 = false;
    boolean _or_2 = false;
    URI _namespace_4 = nodeBuilder.getNamespace();
    boolean _tripleEquals_1 = (_namespace_4 == null);
    if (_tripleEquals_1) {
      _or_2 = true;
    } else {
      URI _namespace_5 = nodeBuilder.getNamespace();
      URI _namespace_6 = validQName.getNamespace();
      boolean _equals = Objects.equal(_namespace_5, _namespace_6);
      _or_2 = (_tripleEquals_1 || _equals);
    }
    if (_or_2) {
      _or_1 = true;
    } else {
      URI _namespace_7 = nodeBuilder.getNamespace();
      String _string = _namespace_7.toString();
      boolean _equals_1 = Objects.equal(_string, moduleName);
      _or_1 = (_or_2 || _equals_1);
    }
    if (_or_1) {
      _or = true;
    } else {
      URI _namespace_8 = nodeBuilder.getNamespace();
      boolean _equals_2 = Objects.equal(_namespace_8, RestconfImpl.MOUNT_POINT_MODULE_NAME);
      _or = (_or_1 || _equals_2);
    }
    if (_or) {
      nodeBuilder.setQname(validQName);
    }
    return currentAugment;
  }

  private URI namespace(final CompositeNode data) {
    if (data instanceof CompositeNodeWrapper) {
      return _namespace((CompositeNodeWrapper)data);
    } else if (data != null) {
      return _namespace(data);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(data).toString());
    }
  }

  private String localName(final CompositeNode data) {
    if (data instanceof CompositeNodeWrapper) {
      return _localName((CompositeNodeWrapper)data);
    } else if (data != null) {
      return _localName(data);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(data).toString());
    }
  }

  private Module findModule(final MountInstance mountPoint, final CompositeNode data) {
    if (data instanceof CompositeNodeWrapper) {
      return _findModule(mountPoint, (CompositeNodeWrapper)data);
    } else if (data != null) {
      return _findModule(mountPoint, data);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(mountPoint, data).toString());
    }
  }

  private String getName(final CompositeNode data) {
    if (data instanceof CompositeNodeWrapper) {
      return _getName((CompositeNodeWrapper)data);
    } else if (data != null) {
      return _getName(data);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(data).toString());
    }
  }

  private TypeDefinition<? extends Object> typeDefinition(final DataSchemaNode node) {
    if (node instanceof LeafListSchemaNode) {
      return _typeDefinition((LeafListSchemaNode)node);
    } else if (node instanceof LeafSchemaNode) {
      return _typeDefinition((LeafSchemaNode)node);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(node).toString());
    }
  }
}
