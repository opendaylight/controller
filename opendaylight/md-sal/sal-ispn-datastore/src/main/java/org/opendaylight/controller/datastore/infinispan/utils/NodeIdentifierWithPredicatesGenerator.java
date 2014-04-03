package org.opendaylight.controller.datastore.infinispan.utils;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeIdentifierWithPredicatesGenerator{
    private final String id;
    private final Pattern pattern = Pattern.compile("(.*)\\Q[{\\E(.*)\\Q}]\\E");
    private final Matcher matcher;
    private final boolean doesMatch;
    private final ListSchemaNode listSchemaNode;

    public NodeIdentifierWithPredicatesGenerator(String id){
        this(id, null);
    }

    public NodeIdentifierWithPredicatesGenerator(String id, ListSchemaNode schemaNode){
        this.id = id;
        matcher = pattern.matcher(this.id);
        doesMatch = matcher.matches();
        this.listSchemaNode = schemaNode;
    }


    public boolean matches(){
        return doesMatch;
    }

    public InstanceIdentifier.NodeIdentifierWithPredicates getPathArgument(){
        final String group = matcher.group(2);
        final String[] keyValues = group.split(",");
        Map<QName, Object> nameValues = new HashMap<>();

        for(String keyValue : keyValues){
            int eqIndex = keyValue.lastIndexOf('=');
            final QName key = QName.create(keyValue.substring(0, eqIndex));
            nameValues.put(key, getValue(key, keyValue.substring(eqIndex + 1)));
        }

        return new InstanceIdentifier.NodeIdentifierWithPredicates(QName.create(matcher.group(1)), nameValues);
    }


    private Object getValue(QName key, String value){
        if(listSchemaNode != null){
            for(DataSchemaNode node : listSchemaNode.getChildNodes()){
                if(node instanceof LeafSchemaNode && node.getQName().equals(key)){
                    return TypeDefinitionAwareCodec.from(LeafSchemaNode.class.cast(node).getType()).deserialize(value);
                }
            }
        }
        return value;
    }
}
