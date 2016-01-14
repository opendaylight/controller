package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

public class DataTreeCohortAdapter<C extends DOMDataTreeCommitCohort>
        implements DOMDataTreeCommitCohortRegistration<C> {



    public DataTreeCohortAdapter(ActorContext actorContext, DOMDataTreeIdentifier subtree, C cohort) {

    }

    public void init(String shardName) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public C getInstance() {
        // TODO Auto-generated method stub
        return null;
    }

}
