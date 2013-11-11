package org.opendaylight.controller.sal.binding.impl.connect.dom

import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yangtools.sal.binding.model.api.CodeGenerator
import org.opendaylight.yangtools.sal.binding.generator.impl.BindingGeneratorImpl
import org.opendaylight.yangtools.sal.binding.generator.api.BindingGenerator
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import java.util.Collections
import java.util.Map.Entry
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.AbstractMap.SimpleEntry
import org.opendaylight.controller.sal.core.api.model.SchemaService

class MappingServiceImpl implements SchemaServiceListener, BindingIndependentMappingService {

    var extension BindingMapping mapping = new BindingMapping;

    @Property
    BindingGeneratorImpl binding;

    @Property
    SchemaService schemaService;

    override onGlobalContextUpdated(SchemaContext arg0) {
        recreateBindingContext(arg0);
    }

    def recreateBindingContext(SchemaContext schemaContext) {
        val newBinding = new BindingGeneratorImpl();
        newBinding.generateTypes(schemaContext);
        val newMapping = new BindingMapping();
        for (entry : newBinding.moduleContexts.entrySet) {
            val module = entry.key;
            val context = entry.value;
            
            newMapping.updateBinding(schemaContext, context);
        }
        mapping = newMapping
    }

    override CompositeNode toDataDom(DataObject data) {
        mapping.toCompositeNode(data);
    }

    override Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> toDataDom(
        Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry) {
        val key = mapping.toDataDom(entry.key);
        val data = mapping.toCompositeNode(entry.value);
        return new SimpleEntry(key, data);
    }

    override org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toDataDom(
        InstanceIdentifier<? extends DataObject> path) {
        return mapping.toDataDom(path);
    }
    
    override dataObjectFromDataDom(InstanceIdentifier<? extends DataObject> path, CompositeNode result) {
        return mapping.dataObjectFromDataDom(path,result);
    }
    
    public def void start() {
        schemaService.registerSchemaServiceListener(this);
        recreateBindingContext(schemaService.globalContext);
    }
}
