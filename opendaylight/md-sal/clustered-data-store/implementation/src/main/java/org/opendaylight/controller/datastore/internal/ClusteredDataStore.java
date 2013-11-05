package org.opendaylight.controller.datastore.internal;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ClusteredDataStore implements DataReader<InstanceIdentifier<? extends Object>, Object>, DataCommitHandler<InstanceIdentifier<? extends Object>,Object> {
    @Override
    public DataCommitTransaction<InstanceIdentifier<? extends Object>, Object> requestCommit(DataModification<InstanceIdentifier<? extends Object>, Object> modification) {
        return null;
    }

    @Override
    public Object readOperationalData(InstanceIdentifier<? extends Object> path) {
        return null;
    }

    @Override
    public Object readConfigurationData(InstanceIdentifier<? extends Object> path) {
        return null;
    }
}
