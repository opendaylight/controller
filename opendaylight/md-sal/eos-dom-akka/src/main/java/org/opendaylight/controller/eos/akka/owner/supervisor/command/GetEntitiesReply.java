/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingInstanceIdentifierCodec;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntitiesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntitiesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.NodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.get.entities.output.EntitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsal.core.general.entity.rev150930.Entity;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

public final class GetEntitiesReply extends OwnerSupervisorReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ImmutableSetMultimap<DOMEntity, String> candidates;
    private final ImmutableMap<DOMEntity, String> owners;

    public GetEntitiesReply(final Map<DOMEntity, String> owners, final Map<DOMEntity, Set<String>> candidates) {
        final Builder<DOMEntity, String> builder = ImmutableSetMultimap.builder();
        for (Entry<DOMEntity, Set<String>> entry : candidates.entrySet()) {
            builder.putAll(entry.getKey(), entry.getValue());
        }
        this.candidates = builder.build();
        this.owners = ImmutableMap.copyOf(owners);
    }

    public @NonNull GetEntitiesOutput toOutput(final BindingInstanceIdentifierCodec iidCodec) {
        final Set<DOMEntity> entities = new HashSet<>();
        entities.addAll(owners.keySet());
        entities.addAll(candidates.keySet());

        return new GetEntitiesOutputBuilder()
            .setEntities(entities.stream()
                .map(entity -> {
                    final EntitiesBuilder eb = new EntitiesBuilder()
                        .setType(new EntityType(entity.getType()))
                        .setName(extractName(entity, iidCodec))
                        .setCandidateNodes(candidates.get(entity).stream()
                            .map(NodeName::new).collect(Collectors.toUnmodifiableList()));

                    final String owner = owners.get(entity);
                    if (owner != null) {
                        eb.setOwnerNode(new NodeName(owner));
                    }
                    return eb.build();
                })
                .collect(BindingMap.toMap()))
            .build();
    }

    /**
     * if the entity is general entity then shorthand the name to only the last path argument, otherwise return
     * full YIID path encoded as string.
     *
     * @param entity Entity to extract the name from
     * @param iidCodec codec to encode entity name back to InstanceIdentifier if needed
     * @return Extracted name
     */
    private static EntityName extractName(final DOMEntity entity, final BindingInstanceIdentifierCodec iidCodec) {
        final var id = entity.getIdentifier();
        if (id.isEmpty() || !id.getPathArguments().get(0).getNodeType().equals(Entity.QNAME)) {
            return new EntityName(iidCodec.toBinding(id));
        }

        final PathArgument last = id.getLastPathArgument();
        verify(last instanceof NodeIdentifierWithPredicates, "Unexpected last argument %s", last);
        final Object value = Iterables.getOnlyElement(((NodeIdentifierWithPredicates) last).values());
        verify(value instanceof String, "Unexpected predicate value %s", value);
        return new EntityName((String) value);
    }
}
