package org.opendaylight.controller.yang.model.parser.builder.api;

import org.opendaylight.controller.yang.model.api.TypeDefinition;

public class AbstractTypeAwareBuilder implements TypeAwareBuilder {

    protected TypeDefinition<?> type;
    protected TypeDefinitionBuilder typedef;

    @Override
    public TypeDefinition<?> getType() {
        return type;
    }

    @Override
    public TypeDefinitionBuilder getTypedef() {
        return typedef;
    }

    @Override
    public void setType(TypeDefinition<?> type) {
        this.type = type;
        this.typedef = null;
    }

    @Override
    public void setType(TypeDefinitionBuilder typedef) {
        this.typedef = typedef;
        this.type = null;
    }

}
