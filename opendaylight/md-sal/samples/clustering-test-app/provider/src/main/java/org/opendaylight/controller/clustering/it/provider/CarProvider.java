/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidate;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterOwnershipInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipInput;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Pantelis
 */
public class CarProvider implements CarService {
    private static final Logger LOG = LoggerFactory.getLogger(CarProvider.class);

    private static final String ENTITY_TYPE = "cars";

    private final EntityOwnershipService ownershipService;
    private final CarEntityOwnershipCandidate ownershipCandidate = new CarEntityOwnershipCandidate();

    public CarProvider(EntityOwnershipService ownershipService) {
        this.ownershipService = ownershipService;
    }

    @Override
    public Future<RpcResult<Void>> registerOwnership(RegisterOwnershipInput input) {
        Entity entity = new Entity(ENTITY_TYPE, input.getCarId());
        try {
            ownershipService.registerCandidate(entity, ownershipCandidate);
        } catch (CandidateAlreadyRegisteredException e) {
            return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION,
                    "Could not register for car " + input.getCarId(), e).buildFuture();
        }

        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> unregisterOwnership(UnregisterOwnershipInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    private static class CarEntityOwnershipCandidate implements EntityOwnershipCandidate {
        @Override
        public void ownershipChanged(Entity entity, boolean wasOwner, boolean isOwner) {
            LOG.info("ownershipChanged: entity: {}, wasOwner: {}, isOwner: ()", entity, wasOwner, isOwner);
        }
    }
}
