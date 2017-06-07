/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.opendaylight.controller.cluster.access.commands.TransactionModification.TYPE_WRITE;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class ModifyTransactionRequestTest extends AbstractTransactionRequestTest<ModifyTransactionRequest> {
    private static final NormalizedNode<?, ?> NODE = Builders.containerBuilder().withNodeIdentifier(
            YangInstanceIdentifier.NodeIdentifier.create(QName.create("namespace", "localName"))).build();

    private static final List<TransactionModification> MODIFICATIONS = Lists.newArrayList(
            new TransactionWrite(YangInstanceIdentifier.EMPTY, NODE));

    private static final PersistenceProtocol PROTOCOL = PersistenceProtocol.ABORT;

    private static final ModifyTransactionRequest OBJECT = new ModifyTransactionRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF, MODIFICATIONS, PROTOCOL);

    @Override
    protected ModifyTransactionRequest object() {
        return OBJECT;
    }

    @Test
    public void getPersistenceProtocolTest() throws Exception {
        final Optional<PersistenceProtocol> result = OBJECT.getPersistenceProtocol();
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(PROTOCOL, result.get());
    }

    @Test
    public void getModificationsTest() throws Exception {
        final List<TransactionModification> result = OBJECT.getModifications();
        Assert.assertNotNull(result);
        Assert.assertEquals(MODIFICATIONS, result);
    }

    @Test
    public void addToStringAttributesTest() {
        final MoreObjects.ToStringHelper result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT));
        Assert.assertTrue(result.toString().contains("modifications=1"));
        Assert.assertTrue(result.toString().contains("protocol=" + PROTOCOL));
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final ModifyTransactionRequest clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        Assert.assertTrue(deserialize instanceof ModifyTransactionRequest);
        final ModifyTransactionRequest casted = (ModifyTransactionRequest) deserialize;

        Assert.assertEquals(OBJECT.getReplyTo(), casted.getReplyTo());
        Assert.assertEquals(OBJECT.getPersistenceProtocol(), casted.getPersistenceProtocol());

        Assert.assertNotNull(casted.getModifications());
        Assert.assertEquals(1, casted.getModifications().size());
        final TransactionModification modification = casted.getModifications().get(0);
        Assert.assertEquals(YangInstanceIdentifier.EMPTY, modification.getPath());
        Assert.assertEquals(TYPE_WRITE, modification.getType());
    }
}