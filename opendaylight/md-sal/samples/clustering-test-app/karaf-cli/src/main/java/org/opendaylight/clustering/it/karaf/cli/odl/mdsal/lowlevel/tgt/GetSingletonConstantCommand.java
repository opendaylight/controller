/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli.odl.mdsal.lowlevel.tgt;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.clustering.it.karaf.cli.AbstractDOMRpcAction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.GetSingletonConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.target.rev170215.GetSingletonConstantInputBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

@Service
@Command(scope = "test-app", name = "get-singleton-constant", description = "Run an get-singleton-constant test")
public class GetSingletonConstantCommand extends AbstractDOMRpcAction {
    @Reference
    private DOMRpcService rpcService;
    @Reference
    private BindingNormalizedNodeSerializer serializer;

    @Override
    protected ListenableFuture<? extends DOMRpcResult> invokeRpc() {
        final ContainerNode inputNode =
                serializer.toNormalizedNodeRpcData(new GetSingletonConstantInputBuilder().build());
        return rpcService.invokeRpc(QName.create(GetSingletonConstantInput.QNAME, "get-singleton-constant"), inputNode);
    }
}
