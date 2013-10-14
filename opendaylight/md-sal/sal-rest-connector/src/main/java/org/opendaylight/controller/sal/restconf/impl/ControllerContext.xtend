package org.opendaylight.controller.sal.restconf.impl

import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import java.net.URI
import java.util.Map
import java.util.HashMap
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.model.api.ChoiceNode
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode

import java.net.URLEncoder
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.InstanceIdentifierBuilder
import java.util.List
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Collections2
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import java.net.URLDecoder

class ControllerContext {

    @Property
    SchemaContext schemas;

    private val BiMap<URI, String> uriToModuleName = HashBiMap.create();
    private val Map<String, URI> moduleNameToUri = uriToModuleName.inverse();

    public def InstanceIdentifier toInstanceIdentifier(String restconfInstance) {
        val ret = InstanceIdentifier.builder();
        val pathArgs = restconfInstance.split("/");
        val first = pathArgs.get(0);
        val startModule = first.toModuleName();
        val module = schemas.findModuleByNamespace(moduleNameToUri.get(startModule));
        checkArgument(module.size == 1); // Only one version supported now
        ret.collectPathArguments(pathArgs, module.iterator.next);
        return ret.toInstance
    }

    def String toFullRestconfIdentifier(InstanceIdentifier path) {
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

    public def CharSequence toRestconfIdentifier(QName qname) {
        var module = uriToModuleName.get(qname.namespace)
        if (module == null) {
            val moduleSchema = schemas.findModuleByNamespaceAndRevision(qname.namespace, qname.revision);
            if(moduleSchema == null) throw new IllegalArgumentException()
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
        if(object == null) return "";
        return URLEncoder.encode(object.toString)
    }

    def void collectPathArguments(InstanceIdentifierBuilder builder, List<String> strings, DataNodeContainer parentNode) {
        checkNotNull(strings)
        if (strings.length == 0) {
            return;
        }
        val nodeRef = strings.get(0);

        //val moduleName = nodeRef.toModuleName();
        val nodeName = nodeRef.toNodeName();
        val naiveTargetNode = parentNode.getDataChildByName(nodeName);

        //var URI namespace;
        var DataSchemaNode targetNode = naiveTargetNode;

        /*if(moduleName !== null) {
            namespace = moduleNameToUri.get(moduleName);
            
        }*/
        // Number of consumed elements
        var consumed = 1;
        if (targetNode instanceof ListSchemaNode) {
            val listNode = targetNode as ListSchemaNode;
            val keysSize = listNode.keyDefinition.size
            val uriKeyValues = strings.subList(1, keysSize);
            val keyValues = new HashMap<QName, Object>();
            var i = 0;
            for (key : listNode.keyDefinition) {
                val uriKeyValue = uriKeyValues.get(i);
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
            val remaining = strings.subList(consumed, strings.length - 1);
            builder.collectPathArguments(remaining, targetNode as DataNodeContainer);
        }
    }

    def void addKeyValue(HashMap<QName, Object> map, DataSchemaNode node, String uriValue) {
        checkNotNull(uriValue);
        checkArgument(node instanceof LeafSchemaNode);
        val decoded = URLDecoder.decode(uriValue);
        map.put(node.QName, decoded);

    }

    def String toModuleName(String str) {
        if (str.contains(":")) {
            val args = str.split(":");
            checkArgument(args.size === 2);
            return args.get(0);
        } else {
            return null;
        }
    }

    def String toNodeName(String str) {
        if (str.contains(":")) {
            val args = str.split(":");
            checkArgument(args.size === 2);
            return args.get(1);
        } else {
            return str;
        }
    }
    
    public def QName toRpcQName(String name) {
        
        
    }
}
