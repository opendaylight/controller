package org.opendaylight.controller.md.sal.binding.impl.test;

import org.junit.Before;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class AbstractSchemaAwareTest  {

    private Iterable<YangModuleInfo> moduleInfos;
    private SchemaContext schemaContext;


    protected Iterable<YangModuleInfo> getModuleInfos() {
        return BindingReflections.loadModuleInfos();
    }


    @Before
    public final void setup() {
        moduleInfos = getModuleInfos();
        ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        moduleContext.addModuleInfos(moduleInfos);
        schemaContext = moduleContext.tryToCreateSchemaContext().get();
        setupWithSchema(schemaContext);
    }


    protected abstract void setupWithSchema(SchemaContext context);

}
