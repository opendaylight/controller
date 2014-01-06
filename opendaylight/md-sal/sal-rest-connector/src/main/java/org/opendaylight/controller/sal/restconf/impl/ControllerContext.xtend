package org.opendaylight.controller.sal.restconf.impl

import com.google.common.collect.BiMap
import com.google.common.collect.FluentIterable
import com.google.common.collect.HashBiMap
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import javax.ws.rs.core.Response
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener
import org.opendaylight.controller.sal.rest.impl.RestUtil
import org.opendaylight.controller.sal.rest.impl.RestconfProvider
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.InstanceIdentifierBuilder
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode
import org.opendaylight.yangtools.yang.model.api.ChoiceNode
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.yang.model.api.RpcDefinition
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition
import org.slf4j.LoggerFactory

import static com.google.common.base.Preconditions.*
import org.opendaylight.controller.sal.core.api.mount.MountService
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode

class ControllerContext implements SchemaServiceListener {
    val static LOG = LoggerFactory.getLogger(ControllerContext)
    val static ControllerContext INSTANCE = new ControllerContext
    val static NULL_VALUE = "null"

    @Property
    var SchemaContext globalSchema;
    
    @Property
    var MountService mountService;

    private val BiMap<URI, String> uriToModuleName = HashBiMap.create();
    private val Map<String, URI> moduleNameToUri = uriToModuleName.inverse();
    private val Map<QName, RpcDefinition> qnameToRpc = new ConcurrentHashMap();

    private new() {
        if (INSTANCE !== null) {
            throw new IllegalStateException("Already instantiated");
        }
    }

    static def getInstance() {
        return INSTANCE
    }

    private def void checkPreconditions() {
        if (globalSchema === null) {
            throw new ResponseException(Response.Status.SERVICE_UNAVAILABLE, RestconfProvider::NOT_INITALIZED_MSG)
        }
    }

    def setSchemas(SchemaContext schemas) {
        onGlobalContextUpdated(schemas)
    }

    public def InstanceIdWithSchemaNode toInstanceIdentifier(String restconfInstance) {
        val ret = InstanceIdentifier.builder();
        val pathArgs = restconfInstance.split("/");
        if (pathArgs.empty) {
            return null;
        }
        if (pathArgs.head.empty) {
            pathArgs.remove(0)
        }
        val schemaNode = ret.collectPathArguments(pathArgs, globalSchema.findModule(pathArgs.head));
        if (schemaNode === null) {
            return null
        }
        return new InstanceIdWithSchemaNode(ret.toInstance, schemaNode)
    }

    private static def findModule(SchemaContext context,String argument) {
        //checkPreconditions
        checkNotNull(argument);
        val startModule = argument.toModuleName();
        return context.getLatestModule(startModule)
    }

    static def getLatestModule(SchemaContext schema,String moduleName) {
        checkArgument(schema != null);
        checkArgument(moduleName !== null && !moduleName.empty)
        val modules = schema.modules.filter[m|m.name == moduleName]
        var latestModule = modules.head
        for (module : modules) {
            if (module.revision.after(latestModule.revision)) {
                latestModule = module
            }
        }
        return latestModule
    }

    def String toFullRestconfIdentifier(InstanceIdentifier path) {
        checkPreconditions
        val elements = path.path;
        val ret = new StringBuilder();
        val startQName = elements.get(0).nodeType;
        val initialModule = globalSchema.findModuleByNamespaceAndRevision(startQName.namespace, startQName.revision)
        var node = initialModule as DataSchemaNode;
        for (element : elements) {
            node = node.childByQName(element.nodeType);
            ret.append(element.toRestconfIdentifier(node));
        }
        return ret.toString
    }

    private def dispatch CharSequence toRestconfIdentifier(NodeIdentifier argument, DataSchemaNode node) {
        '''/«argument.nodeType.toRestconfIdentifier()»'''
    }

    private def dispatch CharSequence toRestconfIdentifier(NodeIdentifierWithPredicates argument, ListSchemaNode node) {
        val nodeIdentifier = argument.nodeType.toRestconfIdentifier();
        val keyValues = argument.keyValues;
        return '''/«nodeIdentifier»/«FOR key : node.keyDefinition SEPARATOR "/"»«keyValues.get(key).toUriString»«ENDFOR»'''
    }

    private def dispatch CharSequence toRestconfIdentifier(PathArgument argument, DataSchemaNode node) {
        throw new IllegalArgumentException("Conversion of generic path argument is not supported");
    }

    def findModuleByNamespace(URI namespace) {
        checkPreconditions
        var module = uriToModuleName.get(namespace)
        if (module === null) {
            val moduleSchemas = globalSchema.findModuleByNamespace(namespace);
            if(moduleSchemas === null) return null
            var latestModule = moduleSchemas.head
            for (m : moduleSchemas) {
                if (m.revision.after(latestModule.revision)) {
                    latestModule = m
                }
            }
            if(latestModule === null) return null
            uriToModuleName.put(namespace, latestModule.name)
            module = latestModule.name;
        }
        return module
    }

    def findNamespaceByModule(String module) {
        var namespace = moduleNameToUri.get(module)
        if (namespace === null) {
            val moduleSchemas = globalSchema.modules.filter[it|it.name.equals(module)]
            var latestModule = moduleSchemas.head
            for (m : moduleSchemas) {
                if (m.revision.after(latestModule.revision)) {
                    latestModule = m
                }
            }
            if(latestModule === null) return null
            namespace = latestModule.namespace
            uriToModuleName.put(namespace, latestModule.name)
        }
        return namespace
    }

    def CharSequence toRestconfIdentifier(QName qname) {
        checkPreconditions
        var module = uriToModuleName.get(qname.namespace)
        if (module === null) {
            val moduleSchema = globalSchema.findModuleByNamespaceAndRevision(qname.namespace, qname.revision);
            if(moduleSchema === null) throw new IllegalArgumentException()
            uriToModuleName.put(qname.namespace, moduleSchema.name)
            module = moduleSchema.name;
        }
        return '''«module»:«qname.localName»''';
    }

    private static dispatch def DataSchemaNode childByQName(ChoiceNode container, QName name) {
        for (caze : container.cases) {
            val ret = caze.childByQName(name)
            if (ret !== null) {
                return ret;
            }
        }
        return null;
    }

    private static dispatch def DataSchemaNode childByQName(ChoiceCaseNode container, QName name) {
        val ret = container.getDataChildByName(name);
        return ret;
    }

    private static dispatch def DataSchemaNode childByQName(ContainerSchemaNode container, QName name) {
        return container.dataNodeChildByQName(name);
    }

    private static dispatch def DataSchemaNode childByQName(ListSchemaNode container, QName name) {
        return container.dataNodeChildByQName(name);
    }

    private static dispatch def DataSchemaNode childByQName(DataSchemaNode container, QName name) {
        return null;
    }

    private static def DataSchemaNode dataNodeChildByQName(DataNodeContainer container, QName name) {
        var ret = container.getDataChildByName(name);
        if (ret === null) {

            // Find in Choice Cases
            for (node : container.childNodes) {
                if (node instanceof ChoiceCaseNode) {
                    val caseNode = (node as ChoiceCaseNode);
                    ret = caseNode.childByQName(name);
                    if (ret !== null) {
                        return ret;
                    }
                }
            }
        }
        return ret;
    }

    private def toUriString(Object object) {
        if(object === null) return "";
        return URLEncoder.encode(object.toString)
    }

    private def DataSchemaNode collectPathArguments(InstanceIdentifierBuilder builder, List<String> strings,
        DataNodeContainer parentNode) {
        checkNotNull(strings)
        if (parentNode === null) {
            return null;
        }
        if (strings.empty) {
            return parentNode as DataSchemaNode;
        }
        val nodeRef = strings.head;

        val nodeName = nodeRef.toNodeName;
        var targetNode = parentNode.findInstanceDataChild(nodeName);
        if (targetNode instanceof ChoiceNode) {
            return null
        }
        
        if (targetNode === null) {
            // Node is possibly in other mount point
            val partialPath = builder.toInstance;
            val mountPointSchema = mountService?.getMountPoint(partialPath)?.schemaContext;
            if(mountPointSchema != null) {
                return builder.collectPathArguments(strings, mountPointSchema.findModule(strings.head));
            }
            return null
        }
        

        // Number of consumed elements
        var consumed = 1;
        if (targetNode instanceof ListSchemaNode) {
            val listNode = targetNode as ListSchemaNode;
            val keysSize = listNode.keyDefinition.size

            // every key has to be filled
            if ((strings.length - consumed) < keysSize) {
                return null;
            }
            val uriKeyValues = strings.subList(consumed, consumed + keysSize);
            val keyValues = new HashMap<QName, Object>();
            var i = 0;
            for (key : listNode.keyDefinition) {
                val uriKeyValue = uriKeyValues.get(i);

                // key value cannot be NULL
                if (uriKeyValue.equals(NULL_VALUE)) {
                    return null
                }
                keyValues.addKeyValue(listNode.getDataChildByName(key), uriKeyValue);
                i = i + 1;
            }
            consumed = consumed + i;
            builder.nodeWithKey(targetNode.QName, keyValues);
        } else {

            // Only one instance of node is allowed
            builder.node(targetNode.QName);
        }
        if (targetNode instanceof DataNodeContainer) {
            val remaining = strings.subList(consumed, strings.length);
            val result = builder.collectPathArguments(remaining, targetNode as DataNodeContainer);
            return result
        }

        return targetNode
    }
    
    static def DataSchemaNode findInstanceDataChild(DataNodeContainer container, String name) {
        // FIXME: Add namespace comparison
        var potentialNode = container.getDataChildByName(name);
        if(potentialNode.instantiatedDataSchema) {
            return potentialNode;
        }
        val allCases = container.childNodes.filter(ChoiceNode).map[cases].flatten
        for (caze : allCases) {
            potentialNode = caze.findInstanceDataChild(name);
            if(potentialNode != null) {
                return potentialNode;
            }
        }
        return null;
    }
    
    static def boolean isInstantiatedDataSchema(DataSchemaNode node) {
        switch node {
            LeafSchemaNode: return true
            LeafListSchemaNode: return true
            ContainerSchemaNode: return true
            ListSchemaNode: return true
            default: return false
        }
    }

    private def void addKeyValue(HashMap<QName, Object> map, DataSchemaNode node, String uriValue) {
        checkNotNull(uriValue);
        checkArgument(node instanceof LeafSchemaNode);
        val urlDecoded = URLDecoder.decode(uriValue);
        val typedef = (node as LeafSchemaNode).type;
        
        var decoded = TypeDefinitionAwareCodec.from(typedef)?.deserialize(urlDecoded)
        if(decoded === null) {
            var baseType = RestUtil.resolveBaseTypeFrom(typedef)
            if(baseType instanceof IdentityrefTypeDefinition) {
                decoded = toQName(urlDecoded)
            }
        }
        map.put(node.QName, decoded);
    }

    private static def String toModuleName(String str) {
        checkNotNull(str)
        if (str.contains(":")) {
            val args = str.split(":");
            checkArgument(args.size === 2);
            return args.get(0);
        } else {
            return null;
        }
    }

    private def String toNodeName(String str) {
        if (str.contains(":")) {
            val args = str.split(":");
            checkArgument(args.size === 2);
            return args.get(1);
        } else {
            return str;
        }
    }

    private def QName toQName(String name) {
        val module = name.toModuleName;
        val node = name.toNodeName;
        val namespace = FluentIterable.from(globalSchema.modules.sort[o1,o2 | o1.revision.compareTo(o2.revision)]) //
            .transform[QName.create(namespace,revision,it.name)].findFirst[module == localName]
        ;
        return QName.create(namespace,node);
    }

    def getRpcDefinition(String name) {
        return qnameToRpc.get(name.toQName)
    }

    override onGlobalContextUpdated(SchemaContext context) {
        this.globalSchema = context;
        for (operation : context.operations) {
            val qname = operation.QName;
            qnameToRpc.put(qname, operation);
        }
    }

}
