package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 *
 * Commit Cohort registry for {@link DOMDataReadWriteTransaction} and {@link DOMDataWriteTransaction}.
 *
 *
 * @author Tony Tkacik <ttkacik@cisco.com>
 *
 */
public interface DOMDataCommitHandlerRegistry {

    <T extends DOMDataCommitCohort> ObjectRegistration<T> registerCommitCohort(LogicalDatastoreType store,DOMDataCommitCohort cohort);
}
