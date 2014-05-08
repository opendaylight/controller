/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.BindingMapping;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class BindingToNormalizedNodeCodec implements SchemaContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(BindingToNormalizedNodeCodec.class);

    private final BindingIndependentMappingService bindingToLegacy;
    private DataNormalizer legacyToNormalized;

    public BindingToNormalizedNodeCodec(final BindingIndependentMappingService mappingService) {
        super();
        this.bindingToLegacy = mappingService;
    }

    public org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toNormalized(
            final InstanceIdentifier<? extends DataObject> binding) {

        // Used instance-identifier codec do not support serialization of last
        // path
        // argument if it is Augmentation (behaviour expected by old datastore)
        // in this case, we explicitly check if last argument is augmentation
        // to process it separately
        if (isAugmentationIdentifier(binding)) {
            return toNormalizedAugmented(binding);
        }
        return toNormalizedImpl(binding);
    }

    public Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final InstanceIdentifier<? extends DataObject> bindingPath, final DataObject bindingObject) {
        return toNormalizedNode(toBindingEntry(bindingPath, bindingObject));

    }

    public Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> binding) {
        Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> legacyEntry = bindingToLegacy
                .toDataDom(binding);
        Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalizedEntry = legacyToNormalized
                .toNormalized(legacyEntry);
        LOG.trace("Serialization of {}, Legacy Representation: {}, Normalized Representation: {}", binding,
                legacyEntry, normalizedEntry);
        if (isAugmentation(binding.getKey().getTargetType())) {

            for (DataContainerChild<? extends PathArgument, ?> child : ((DataContainerNode<?>) normalizedEntry
                    .getValue()).getValue()) {
                if (child instanceof AugmentationNode) {
                    ImmutableList<PathArgument> childArgs = ImmutableList.<PathArgument> builder()
                            .addAll(normalizedEntry.getKey().getPathArguments()).add(child.getIdentifier()).build();
                    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier childPath = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
                            .create(childArgs);
                    return toDOMEntry(childPath, child);
                }
            }

        }
        return normalizedEntry;

    }

    /**
     *
     * Returns a Binding-Aware instance identifier from normalized
     * instance-identifier if it is possible to create representation.
     *
     * Returns Optional.absent for cases where target is mixin node except
     * augmentation.
     *
     */
    public Optional<InstanceIdentifier<? extends DataObject>> toBinding(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized)
                    throws DeserializationException {

        PathArgument lastArgument = Iterables.getLast(normalized.getPathArguments());
        // Used instance-identifier codec do not support serialization of last
        // path
        // argument if it is AugmentationIdentifier (behaviour expected by old
        // datastore)
        // in this case, we explicitly check if last argument is augmentation
        // to process it separately
        if (lastArgument instanceof AugmentationIdentifier) {
            return toBindingAugmented(normalized);
        }
        return toBindingImpl(normalized);
    }

    private Optional<InstanceIdentifier<? extends DataObject>> toBindingAugmented(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized)
                    throws DeserializationException {
        Optional<InstanceIdentifier<? extends DataObject>> potential = toBindingImpl(normalized);
        // Shorthand check, if codec already supports deserialization
        // of AugmentationIdentifier we will return
        if (potential.isPresent() && isAugmentationIdentifier(potential.get())) {
            return potential;
        }

        int normalizedCount = getAugmentationCount(normalized);
        AugmentationIdentifier lastArgument = (AugmentationIdentifier) Iterables.getLast(normalized.getPathArguments());

        // Here we employ small trick - Binding-aware Codec injects an pointer
        // to augmentation class
        // if child is referenced - so we will reference child and then shorten
        // path.
        LOG.trace("Looking for candidates to match {}", normalized);
        for (QName child : lastArgument.getPossibleChildNames()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier childPath = normalized.node(child);
            try {
                if (isNotRepresentable(childPath)) {
                    LOG.trace("Path {} is not BI-representable, skipping it", childPath);
                    continue;
                }
            } catch (DataNormalizationException e) {
                LOG.warn("Failed to denormalize path {}, skipping it", childPath, e);
                continue;
            }

            Optional<InstanceIdentifier<? extends DataObject>> baId = toBindingImpl(childPath);
            if (!baId.isPresent()) {
                LOG.debug("No binding-aware identifier found for path {}, skipping it", childPath);
                continue;
            }

            InstanceIdentifier<? extends DataObject> potentialPath = shortenToLastAugment(baId.get());
            int potentialAugmentCount = getAugmentationCount(potentialPath);
            if (potentialAugmentCount == normalizedCount) {
                LOG.trace("Found matching path {}", potentialPath);
                return Optional.<InstanceIdentifier<? extends DataObject>> of(potentialPath);
            }

            LOG.trace("Skipping mis-matched potential path {}", potentialPath);
        }

        LOG.trace("Failed to find augmentation matching {}", normalized);
        return Optional.absent();
    }

    private Optional<InstanceIdentifier<? extends DataObject>> toBindingImpl(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized)
                    throws DeserializationException {
        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier legacyPath;

        try {
            if (isNotRepresentable(normalized)) {
                return Optional.absent();
            }
            legacyPath = legacyToNormalized.toLegacy(normalized);
        } catch (DataNormalizationException e) {
            throw new IllegalStateException("Could not denormalize path.", e);
        }
        LOG.trace("InstanceIdentifier Path Deserialization: Legacy representation {}, Normalized representation: {}",
                legacyPath, normalized);
        return Optional.<InstanceIdentifier<? extends DataObject>> of(bindingToLegacy.fromDataDom(legacyPath));
    }

    private boolean isNotRepresentable(final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized)
            throws DataNormalizationException {
        DataNormalizationOperation<?> op = findNormalizationOperation(normalized);
        if (op.isMixin() && op.getIdentifier() instanceof NodeIdentifier) {
            return true;
        }
        if (op.isLeaf()) {
            return true;
        }
        return false;
    }

    private DataNormalizationOperation<?> findNormalizationOperation(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized)
                    throws DataNormalizationException {
        DataNormalizationOperation<?> current = legacyToNormalized.getRootOperation();
        for (PathArgument arg : normalized.getPathArguments()) {
            current = current.getChild(arg);
        }
        return current;
    }

    private static final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> toBindingEntry(
            final org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> key,
            final DataObject value) {
        return new SimpleEntry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject>(
                key, value);
    }

    private static final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> toDOMEntry(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier key, final NormalizedNode<?, ?> value) {
        return new SimpleEntry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>>(key,
                value);
    }

    public DataObject toBinding(final InstanceIdentifier<?> path, final NormalizedNode<?, ?> normalizedNode)
            throws DeserializationException {
        CompositeNode legacy = null;
        if (isAugmentationIdentifier(path) && normalizedNode instanceof AugmentationNode) {
            QName augIdentifier = BindingReflections.findQName(path.getTargetType());
            ContainerNode virtualNode = Builders.containerBuilder() //
                    .withNodeIdentifier(new NodeIdentifier(augIdentifier)) //
                    .withChild((DataContainerChild<?, ?>) normalizedNode) //
                    .build();
            legacy = (CompositeNode) DataNormalizer.toLegacy(virtualNode);
        } else {
            legacy = (CompositeNode) DataNormalizer.toLegacy(normalizedNode);
        }

        return bindingToLegacy.dataObjectFromDataDom(path, legacy);
    }

    public DataNormalizer getDataNormalizer() {
        return legacyToNormalized;
    }

    public Optional<Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject>> toBinding(
            final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, ? extends NormalizedNode<?, ?>> normalized)
                    throws DeserializationException {
        Optional<InstanceIdentifier<? extends DataObject>> potentialPath = toBinding(normalized.getKey());
        if (potentialPath.isPresent()) {
            InstanceIdentifier<? extends DataObject> bindingPath = potentialPath.get();
            DataObject bindingData = toBinding(bindingPath, normalized.getValue());
            if (bindingData == null) {
                LOG.warn("Failed to deserialize {} to Binding format. Binding path is: {}", normalized, bindingPath);
            }
            return Optional.of(toBindingEntry(bindingPath, bindingData));
        } else {
            return Optional.absent();
        }
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext arg0) {
        legacyToNormalized = new DataNormalizer(arg0);
    }

    private org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toNormalizedAugmented(
            final InstanceIdentifier<?> augPath) {
        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier processed = toNormalizedImpl(augPath);
        // If used instance identifier codec added supports for deserialization
        // of last AugmentationIdentifier we will just reuse it
        if (isAugmentationIdentifier(processed)) {
            return processed;
        }
        Optional<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> additionalSerialized;
        additionalSerialized = toNormalizedAugmentedUsingChildContainers(augPath, processed);

        if (additionalSerialized.isPresent()) {
            return additionalSerialized.get();
        }
        additionalSerialized = toNormalizedAugmentedUsingChildLeafs(augPath, processed);
        if (additionalSerialized.isPresent()) {
            return additionalSerialized.get();
        }
        throw new IllegalStateException("Unabled to construct augmentation identfier for " + augPath);
    }

    /**
     * Tries to find correct augmentation identifier using children leafs
     *
     * This method uses normalized Instance Identifier of parent node to fetch
     * schema and {@link BindingReflections#getModuleInfo(Class)} to learn about
     * augmentation namespace, specificly, in which module it was defined.
     *
     * Then it uses it to filter all available augmentations for parent by
     * module. After that it walks augmentations in particular module and
     * pick-up first which at least one leaf name matches supplied augmentation.
     * We could do this safely since YANG explicitly states that no any existing
     * augmentations must differ in leaf fully qualified names.
     *
     *
     * @param augPath
     *            Binding Aware Path which ends with augment
     * @param parentPath
     *            Processed path
     * @return
     */
    private Optional<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> toNormalizedAugmentedUsingChildLeafs(
            final InstanceIdentifier<?> augPath,
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier parentPath) {
        try {
            DataNormalizationOperation<?> parentOp = legacyToNormalized.getOperation(parentPath);
            if(!parentOp.getDataSchemaNode().isPresent()) {
                return Optional.absent();
            }
            DataSchemaNode parentSchema = parentOp.getDataSchemaNode().get();
            if (parentSchema instanceof AugmentationTarget) {
                Set<AugmentationSchema> augmentations = ((AugmentationTarget) parentSchema).getAvailableAugmentations();
                LOG.info("Augmentations for {}, {}", augPath, augmentations);
                Optional<AugmentationSchema> schema = findAugmentation(augPath.getTargetType(), augmentations);
                if (schema.isPresent()) {
                    AugmentationIdentifier augmentationIdentifier = DataNormalizationOperation
                            .augmentationIdentifierFrom(schema.get());
                    return Optional.of(parentPath.node(augmentationIdentifier));
                }
            }
        } catch (DataNormalizationException e) {
            throw new IllegalArgumentException(e);
        }
        return Optional.absent();
    }

    /**
     * Creates instance identifier for augmentation child, tries to serialize it
     * Instance Identifier is then shortened to last augmentation.
     *
     * This is for situations, where underlying codec is implementing hydrogen
     * style DOM APIs (which did not supported {@link AugmentationIdentifier}.)
     *
     * @param augPath
     * @param parentPath
     *            Path to parent node
     * @return
     */
    @SuppressWarnings("rawtypes")
    private Optional<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> toNormalizedAugmentedUsingChildContainers(
            final InstanceIdentifier<?> augPath,
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier parentPath) {
        for (Class augChild : BindingReflections.getChildrenClasses(augPath.getTargetType())) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<?> childPath = augPath.child(augChild);
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized = toNormalizedImpl(childPath);
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier potentialDiscovered = shortenToLastAugmentation(
                    normalized, parentPath);
            if (potentialDiscovered != null) {
                return Optional.of(potentialDiscovered);
            }
        }
        return Optional.absent();
    }

    private Optional<AugmentationSchema> findAugmentation(final Class<?> targetType,
            final Set<AugmentationSchema> augmentations) {
        YangModuleInfo moduleInfo;
        try {
            moduleInfo = BindingReflections.getModuleInfo(targetType);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Iterable<AugmentationSchema> filtered = filteredByModuleInfo(augmentations,
                BindingReflections.getModuleQName(moduleInfo).getModule());
        filtered.toString();
        Set<String> targetTypeGetters = getYangModeledGetters(targetType);
        for (AugmentationSchema schema : filtered) {
            for (DataSchemaNode child : schema.getChildNodes()) {
                String getterName = "get" + BindingMapping.getClassName(child.getQName());
                if (targetTypeGetters.contains(getterName)) {
                    return Optional.of(schema);
                }
            }
        }
        return Optional.absent();
    }

    private static Iterable<AugmentationSchema> filteredByModuleInfo(final Iterable<AugmentationSchema> augmentations,
            final QNameModule module) {
        return Iterables.filter(augmentations, new Predicate<AugmentationSchema>() {
            @Override
            public boolean apply(final AugmentationSchema schema) {
                final Set<DataSchemaNode> childNodes = schema.getChildNodes();
                return !schema.getChildNodes().isEmpty()
                        && module.equals(Iterables.get(childNodes, 0).getQName().getModule());
            }
        });
    }

    public static final Set<String> getYangModeledGetters(final Class<?> targetType) {
        HashSet<String> ret = new HashSet<String>();
        for (Method method : targetType.getMethods()) {
            if (isYangModeledGetter(method)) {
                ret.add(method.getName());
            }
        }
        return ret;
    }

    /**
     *
     * Returns true if supplied method represent getter for YANG modeled value
     *
     * @param method
     *            Method to be tested
     * @return true if method represent getter for YANG Modeled value.
     */
    private static final boolean isYangModeledGetter(final Method method) {
        return !method.getName().equals("getClass") && !method.getName().equals("getImplementedInterface")
                && method.getName().startsWith("get") && method.getParameterTypes().length == 0;
    }

    private org.opendaylight.yangtools.yang.data.api.InstanceIdentifier shortenToLastAugmentation(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized,
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier parentPath) {
        int parentSize = Iterables.size(parentPath.getPathArguments());
        int position = 0;
        int foundPosition = -1;
        for (PathArgument arg : normalized.getPathArguments()) {
            position++;
            if (arg instanceof AugmentationIdentifier) {
                foundPosition = position;
            }
        }
        if (foundPosition > 0 && foundPosition > parentSize) {
            Iterable<PathArgument> shortened = Iterables.limit(normalized.getPathArguments(), foundPosition);
            return org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.create(shortened);
        }
        return null;
    }

    private InstanceIdentifier<? extends DataObject> shortenToLastAugment(
            final InstanceIdentifier<? extends DataObject> binding) {
        int position = 0;
        int foundPosition = -1;
        for (org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument arg : binding.getPathArguments()) {
            position++;
            if (isAugmentation(arg.getType())) {
                foundPosition = position;
            }
        }
        return InstanceIdentifier.create(Iterables.limit(binding.getPathArguments(), foundPosition));
    }

    private org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toNormalizedImpl(
            final InstanceIdentifier<? extends DataObject> binding) {
        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier legacyPath = bindingToLegacy
                .toDataDom(binding);
        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized = legacyToNormalized
                .toNormalized(legacyPath);
        return normalized;
    }

    private static boolean isAugmentation(final Class<? extends DataObject> type) {
        return Augmentation.class.isAssignableFrom(type);
    }

    private static boolean isAugmentationIdentifier(final InstanceIdentifier<?> potential) {
        return Augmentation.class.isAssignableFrom(potential.getTargetType());
    }

    private boolean isAugmentationIdentifier(final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier processed) {
        return Iterables.getLast(processed.getPathArguments()) instanceof AugmentationIdentifier;
    }

    private static int getAugmentationCount(final InstanceIdentifier<?> potential) {
        int count = 0;
        for (org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument arg : potential.getPathArguments()) {
            if (isAugmentation(arg.getType())) {
                count++;
            }

        }
        return count;
    }

    private static int getAugmentationCount(final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier potential) {
        int count = 0;
        for (PathArgument arg : potential.getPathArguments()) {
            if (arg instanceof AugmentationIdentifier) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T extends DataObject> Function<Optional<NormalizedNode<?, ?>>, Optional<T>>  deserializeFunction(final InstanceIdentifier<T> path) {
        return new DeserializeFunction(this, path);
    }

    private static class DeserializeFunction<T extends DataObject> implements Function<Optional<NormalizedNode<?, ?>>, Optional<T>> {

        private final BindingToNormalizedNodeCodec codec;
        private final InstanceIdentifier<?> path;

        public DeserializeFunction(final BindingToNormalizedNodeCodec codec, final InstanceIdentifier<?> path) {
            super();
            this.codec = Preconditions.checkNotNull(codec, "Codec must not be null");
            this.path = Preconditions.checkNotNull(path, "Path must not be null");
        }

        @SuppressWarnings("rawtypes")
        @Nullable
        @Override
        public Optional apply(@Nullable final Optional<NormalizedNode<?, ?>> normalizedNode) {
            if (normalizedNode.isPresent()) {
                final DataObject dataObject;
                try {
                    dataObject = codec.toBinding(path, normalizedNode.get());
                } catch (DeserializationException e) {
                    LOG.warn("Failed to create dataobject from node {}", normalizedNode.get(), e);
                    throw new IllegalStateException("Failed to create dataobject", e);
                }

                if (dataObject != null) {
                    return Optional.of(dataObject);
                }
            }
            return Optional.absent();
        }
    }

    /**
     * Returns an default object according to YANG schema for supplied path.
     *
     * @param path DOM Path
     * @return Node with defaults set on.
     */
    public NormalizedNode<?, ?> getDefaultNodeFor(final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier path) {
        Iterator<PathArgument> iterator = path.getPathArguments().iterator();
        DataNormalizationOperation<?> currentOp = legacyToNormalized.getRootOperation();
        while (iterator.hasNext()) {
            PathArgument currentArg = iterator.next();
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
            }
        }
        return currentOp.createDefault(path.getLastPathArgument());
    }
}
