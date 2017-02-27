/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.IdSequence;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.OdlMdsalLowlevelTargetListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YnlListener implements OdlMdsalLowlevelTargetListener {

    private static final Logger LOG = LoggerFactory.getLogger(YnlListener.class);

    private final String id;

    private long localNumber = 0;
    private long allNot = 0;
    private long idNot = 0;
    private long errNot = 0;

    public YnlListener(final String id) {
        Preconditions.checkNotNull(id);
        this.id = id;
    }

    @Override
    public void onIdSequence(final IdSequence notification) {
        LOG.debug("Received id-sequence notification, : {}", notification);

        allNot++;

        if (notification.getId().equals(id)) {
            idNot++;
            if (notification.getSequenceNumber() - localNumber == 1) {
                localNumber++;
            } else {
                errNot++;
            }
        }
    }

    public UnsubscribeYnlOutput getOutput() {
        return new UnsubscribeYnlOutputBuilder()
                .setAllNot(allNot)
                .setErrNot(errNot)
                .setIdNot(idNot)
                .setLocalNumber(localNumber)
                .build();
    }
}
