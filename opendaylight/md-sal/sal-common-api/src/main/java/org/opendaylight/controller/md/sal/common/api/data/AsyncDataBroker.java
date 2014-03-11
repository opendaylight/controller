package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Path;

public interface AsyncDataBroker<P extends Path<P>, D, L extends AsyncDataChangeListener<P, D>> extends //
        AsyncDataTransactionFactory<P, D> {

    /**
    *
    * Scope of Data Change
    *
    * Represents scope of data change (addition, replacement, deletion).
    *
    */
   public enum DataChangeScope {

       /**
        * Represents only a direct change of the node, such as replacement of node,
        * addition or deletion.
        *
        */
       BASE,
       /**
        * Represent a change (addition,replacement,deletion)
        * of the node or one of it's direct childs.
        *
        */
       DIRECT_CHILD,
       /**
        * Represents a change of the node or any of it's child nodes.
        *
        */
       SUBTREE

   }

    @Override
    public AsyncReadTransaction<P, D> newReadOnlyTransaction();

    @Override
    public AsyncReadWriteTransaction<P,D> newReadWriteTransaction();

    @Override
    public AsyncWriteTransaction<P, D> newWriteOnlyTransaction();

    /**
     * Registers {@link DataChangeListener} for Data Change callbacks
     * which will be triggered on Operational and Configuration changes.
     *
     * @param path Path (subtree identifier) on which client listener will be invoked.
     * @param listener Instance of listener which should be invoked on
     * @param triggeringScope Scope of change which triggers callback.
     * @return
     */
    ListenerRegistration<L> registerDataChangeListener(LogicalDatastore store, P path, L listener, DataChangeScope triggeringScope);

}
