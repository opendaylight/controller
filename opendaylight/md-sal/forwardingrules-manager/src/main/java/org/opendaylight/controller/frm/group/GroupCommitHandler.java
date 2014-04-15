/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.group;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Deprecated
/**
 * @deprecated we don't want support CommitHandler, please use {@link GroupChangeListener}
 */
public class GroupCommitHandler implements DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {

    private final SalGroupService groupService;

    public SalGroupService getGroupService() {
        return this.groupService;
    }

    @Deprecated
    /**
     * @deprecated we don't want support CommitHandler, please use {@link GroupChangeListener}
     */
    public GroupCommitHandler(final SalGroupService groupService) {
        this.groupService = groupService;
    }

    public DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> requestCommit(
            final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new GroupTransaction(modification, this.getGroupService());
    }
}
