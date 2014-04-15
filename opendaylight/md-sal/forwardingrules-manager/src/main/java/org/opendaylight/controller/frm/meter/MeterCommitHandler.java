/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.meter;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Deprecated
/**
 * @deprecated we don't want support CommitHandler, please use {@link MeterChangeListener}
 */
public class MeterCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {

    private final SalMeterService salMeterService;

    public SalMeterService getSalMeterService() {
        return this.salMeterService;
    }

    @Deprecated
    /**
     * @deprecated we don't want support CommitHandler, please use {@link MeterChangeListener}
     */
    public MeterCommitHandler(final SalMeterService manager) {
        this.salMeterService = manager;
    }

    public DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> requestCommit(
            final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new MeterTransaction(modification, this.getSalMeterService());
    }
}