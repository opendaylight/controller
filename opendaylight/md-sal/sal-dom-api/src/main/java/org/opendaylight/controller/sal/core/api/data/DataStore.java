package org.opendaylight.controller.sal.core.api.data;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public interface DataStore extends //
    DataReader<InstanceIdentifier, CompositeNode>,
    DataCommitHandler<InstanceIdentifier, CompositeNode> {

}
