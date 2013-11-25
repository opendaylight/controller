package org.opendaylight.controller.sal.restconf.impl

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener
import org.opendaylight.controller.sal.rest.impl.RestconfProvider
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.InstanceIdentifierBuilder
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode
import org.opendaylight.yangtools.yang.model.api.ChoiceNode
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.yang.model.api.RpcDefinition
import org.opendaylight.yangtools.yang.model.api.SchemaContext

import static com.google.common.base.Preconditions.*

class ControllerContext implements SchemaServiceListener {

    val static ControllerContext INSTANCE = new ControllerContext

    val static NULL_VALUE = "null"

    @Property
    SchemaContext schemas;

    private val BiMap<URI, String> uriToModuleName = HashBiMap.create();
    private val Map<String, URI> moduleNameToUri = uriToModuleName.inverse();
    private val Map<QName,RpcDefinition> qnameToRpc = new ConcurrentHashMap();
    

    private new() {
        if (INSTANCE !== null) {
            throw new IllegalStateException("Already instantiated");
        }
    }

    static def getInstance() {
        return INSTANCE
    }
    
    private def void checkPreconditions() {
        if (schemas === null) {
            throw new WebApplicationException(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(RestconfProvider::NOT_INITALIZED_MSG).build())
        }
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
        val schemaNode = ret.collectPathArguments(pathArgs, restconfInstance.findModule);
        if (schemaNode === null) {
            return null
        }
        new InstanceIdWithSchemaNode(ret.toInstance, schemaNode)
    }

    private def findModule(String restconfInstance) {
        checkPreconditions
        checkNotNull(restconfInstance);
        val pathArgs = restconfInstance.split("/");
        if (pathArgs.empty) {
            return null;
        }
        val modulWithFirstYangStatement = pathArgs.filter[s|s.contains(":")].head
        val startModule = modulWithFirstYangStatement.toModuleName();
        schemas.getLatestModule(startModule)
    }

    private def getLatestModule(SchemaContext schema, String moduleName) {
        checkNotNull(schema)
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
        val initialModule = schemas.findModuleByNamespaceAndRevision(startQName.namespace, startQName.revision)
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

    def CharSequence toRestconfIdentifier(QName qname) {
        checkPreconditions
        var module = uriToModuleName.get(qname.namespace)
        if (module === null) {
            val moduleSchema = schemas.findModuleByNamespaceAndRevision(qname.namespace, qname.revision);
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
        if (strings.empty) {
            return parentNode as DataSchemaNode;
        }
        val nodeRef = strings.head;

        val nodeName = nodeRef.toNodeName();
        val targetNode = parentNode.getDataChildByName(nodeName);
        if (targetNode === null) {
            val children = parentNode.childNodes
            for (child : children) {
                if (child instanceof ChoiceNode) {
                    val choice = child as ChoiceNode
                    for (caze : choice.cases) {
                        val result = builder.collectPathArguments(strings, caze as DataNodeContainer);
                        if (result !== null)
                            return result
                    }
                }
            }
            return null
        }
        if (targetNode instanceof ChoiceNode) {
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

    private def void addKeyValue(HashMap<QName, Object> map, DataSchemaNode node, String uriValue) {
        checkNotNull(uriValue);
        checkArgument(node instanceof LeafSchemaNode);
        val decoded = URLDecoder.decode(uriValue);
        map.put(node.QName, decoded);

    }

    private def String toModuleName(String str) {
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

    public def QName toQName(String name) {
        val module = name.toModuleName;
        val node = name.toNodeName;
        val namespace = moduleNameToUri.get(module);
        return new QName(namespace,null,node);
    }

    override onGlobalContextUpdated(SchemaContext context) {
        this.schemas = context;
        for(operation : context.operations) {
            val qname = new QName(operation.QName.namespace,null,operation.QName.localName);
            qnameToRpc.put(qname,operation);
        }
    }
    
    def ContainerSchemaNode getRpcOutputSchema(QName name) {
        qnameToRpc.get(name)?.output;
    }
    
    def ContainerSchemaNode getRpcInputSchema(QName name) {
        qnameToRpc.get(name)?.input;
    }

}
