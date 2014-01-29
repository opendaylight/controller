package org.opendaylight.controller.md.sal.common.impl.data;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;

import com.google.common.base.Preconditions;

public class DataModificationReader<P, D> implements DataReader<P, D>, AutoCloseable {

    DataReader<P, D> delegate;
    DataModification<P, D> modification;

    @Override
    public D readConfigurationData(P path) {
        /**
         * Data was removed by modification.
         */
        if (modification().getRemovedConfigurationData().contains(path)) {
            return null;
        }
        D potential = modification().getUpdatedConfigurationData().get(path);
        if (potential != null) {
            return potential;
        }
        return delegate().readConfigurationData(path);
    }

    public D readOperationalData(P path) {
        if (modification().getRemovedOperationalData().contains(path)) {
            return null;
        }
        D potential = modification().getUpdatedOperationalData().get(path);
        if (potential != null) {
            return potential;
        }
        return delegate().readOperationalData(path);
    }

    private DataReader<P, D> delegate() {
        Preconditions.checkState(delegate != null, "Reader is already closed");
        return delegate;
    }

    private DataModification<P, D> modification() {
        Preconditions.checkState(modification != null, "Reader is already closed");
        return modification;
    }

    @Override
    public void close() {
        delegate = null;
        modification = null;
    }
}
