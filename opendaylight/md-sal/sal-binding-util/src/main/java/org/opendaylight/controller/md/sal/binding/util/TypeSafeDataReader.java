package org.opendaylight.controller.md.sal.binding.util;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class TypeSafeDataReader {

    
    private final DataReader<InstanceIdentifier<?>,DataObject> delegate;
    
    
    
    public DataReader<InstanceIdentifier<?>, DataObject> getDelegate() {
        return delegate;
    }


    public TypeSafeDataReader(DataReader<InstanceIdentifier<?>, DataObject> delegate) {
        this.delegate = delegate;
    }


    @SuppressWarnings("unchecked")
    public <D extends DataObject> D readConfigurationData(InstanceIdentifier<D> path) {
        return (D) delegate.readConfigurationData(path);
    }
    
    
    @SuppressWarnings("unchecked")
    public <D extends DataObject> D  readOperationalData(InstanceIdentifier<D> path) {
        return (D) delegate.readOperationalData(path);
    }
    
    public static TypeSafeDataReader forReader(DataReader<InstanceIdentifier<?>, DataObject> delegate) {
        return new TypeSafeDataReader(delegate);
    }
}
