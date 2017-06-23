/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.IdSequence;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.OdlMdsalLowlevelTargetListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YnlListener implements OdlMdsalLowlevelTargetListener {

    private static final Logger LOG = LoggerFactory.getLogger(YnlListener.class);

    private final String id;

    private final AtomicLong localNumber = new AtomicLong();
    private final AtomicLong allNot = new AtomicLong();
    private final AtomicLong idNot = new AtomicLong();
    private final AtomicLong errNot = new AtomicLong();

    public YnlListener(final String id) {
        Preconditions.checkNotNull(id);
        this.id = id;
    }

    @Override
    public void onIdSequence(final IdSequence notification) {
        LOG.debug("Received id-sequence notification, : {}", notification);

        allNot.incrementAndGet();

        if (notification.getId().equals(id)) {
            idNot.incrementAndGet();

            localNumber.getAndUpdate(value -> {
                if (notification.getSequenceNumber() - value == 1) {
                    return value + 1;
                }
                errNot.getAndIncrement();
                return value;
            });
        }
    }

    public UnsubscribeYnlOutput getOutput() {
        return new UnsubscribeYnlOutputBuilder()
                .setAllNot(allNot.get())
                .setErrNot(errNot.get())
                .setIdNot(idNot.get())
                .setLocalNumber(localNumber.get())
                .build();
    }
}
