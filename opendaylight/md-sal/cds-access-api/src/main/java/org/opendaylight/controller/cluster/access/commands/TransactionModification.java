package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

//FIXME: subclasses for write, merge, delete (*not* read)
public abstract class TransactionModification extends TransactionOperation {
    private static final long serialVersionUID = 1L;

    TransactionModification(final YangInstanceIdentifier path) {
        super(path);
    }

}
