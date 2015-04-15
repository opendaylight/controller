/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.base.Preconditions;
import javax.annotation.CheckForNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.FeatureDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.common
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Feb 5, 2015
 */
public class RestconfServiceUtils {

    private RestconfServiceUtils () {
        throw new UnsupportedOperationException("Utility class");
    }

    public static MapEntryNode toModuleEntryNode(@CheckForNull final Module module,
            @CheckForNull final ListSchemaNode moduleSchemaNode) {
        Preconditions.checkArgument(module != null);
        Preconditions.checkArgument(moduleSchemaNode != null);

        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> moduleNodeValues =
                Builders.mapEntryBuilder(moduleSchemaNode);

        final DataSchemaNode nameSchemaNode = RestconfSchemaUtils
                .findInstanceDataChildByName((moduleSchemaNode), "name");
        Preconditions.checkState(nameSchemaNode instanceof LeafSchemaNode);
        moduleNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) nameSchemaNode)
                .withValue(module.getName()).build());

        final DataSchemaNode revisionSchemaNode = RestconfSchemaUtils
                .findInstanceDataChildByName((moduleSchemaNode), "revision");
        Preconditions.checkState(revisionSchemaNode instanceof LeafSchemaNode);
        final String revision = RestconfInternalConstants.REVISION_FORMAT.format(module.getRevision());
        moduleNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) revisionSchemaNode)
                .withValue(revision).build());

        final DataSchemaNode namespaceSchemaNode = RestconfSchemaUtils
                .findInstanceDataChildByName((moduleSchemaNode), "namespace");
        Preconditions.checkState(namespaceSchemaNode instanceof LeafSchemaNode);
        moduleNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) namespaceSchemaNode)
                .withValue(module.getNamespace().toString()).build());

        final DataSchemaNode featureSchemaNode = RestconfSchemaUtils
                .findInstanceDataChildByName((moduleSchemaNode), "feature");
        Preconditions.checkState(featureSchemaNode instanceof LeafListSchemaNode);
        final ListNodeBuilder<Object, LeafSetEntryNode<Object>> featuresBuilder = Builders
                .leafSetBuilder((LeafListSchemaNode) featureSchemaNode);
        for (final FeatureDefinition feature : module.getFeatures()) {
            featuresBuilder.withChild(Builders.leafSetEntryBuilder(((LeafListSchemaNode) featureSchemaNode))
                    .withValue(feature.getQName().getLocalName()).build());
        }
        moduleNodeValues.withChild(featuresBuilder.build());

        return moduleNodeValues.build();
    }

    public static MapEntryNode toStreamEntryNode(@CheckForNull final String streamName,
            @CheckForNull final ListSchemaNode streamSchemaNode) {
        Preconditions.checkArgument(streamName != null);
        Preconditions.checkArgument(streamSchemaNode != null);

        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> streamNodeValues =
                Builders.mapEntryBuilder(streamSchemaNode);

        final DataSchemaNode nameSchemaNode = RestconfSchemaUtils
                .findInstanceDataChildByName((streamSchemaNode), "name");
        Preconditions.checkState(nameSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) nameSchemaNode)
                .withValue(streamName).build());

        final DataSchemaNode descriptionSchemaNode = RestconfSchemaUtils
                .findInstanceDataChildByName((streamSchemaNode), "description");
        Preconditions.checkState(descriptionSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) nameSchemaNode)
                .withValue("DESCRIPTION_PLACEHOLDER").build());

        final DataSchemaNode replaySupportSchemaNode = RestconfSchemaUtils
                .findInstanceDataChildByName((streamSchemaNode), "replay-support");
        Preconditions.checkState(replaySupportSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) replaySupportSchemaNode)
                .withValue(Boolean.valueOf(true)).build());

        final DataSchemaNode replayLogCreationTimeSchemaNode = RestconfSchemaUtils
                .findInstanceDataChildByName((streamSchemaNode), "replay-log-creation-time");
        Preconditions.checkState(replayLogCreationTimeSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) replayLogCreationTimeSchemaNode)
                .withValue("").build());

        final DataSchemaNode eventsSchemaNode = RestconfSchemaUtils
                .findInstanceDataChildByName((streamSchemaNode), "events");
        Preconditions.checkState(eventsSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) eventsSchemaNode)
                .withValue("").build());

        return streamNodeValues.build();
    }
}
