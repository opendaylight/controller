package org.opendaylight.controller.sal.restconf.impl.test;

import java.util.List;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.*;

public class DummyType implements TypeDefinition<DummyType> {
    QName dummyQName = TestUtils.buildQName("dummy type", "simple:uri", "2012-12-17");

    @Override
    public QName getQName() {
        return dummyQName;
    }

    @Override
    public SchemaPath getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getReference() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DummyType getBaseType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUnits() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getDefaultValue() {
        // TODO Auto-generated method stub
        return null;
    }

}
