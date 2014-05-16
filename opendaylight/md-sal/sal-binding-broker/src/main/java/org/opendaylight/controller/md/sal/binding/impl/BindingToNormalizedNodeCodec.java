/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.concepts.util.ClassLoaderUtils;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
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
        return toNormalizedNode(toEntry(bindingPath, bindingObject));

    }

    public Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> toNormalizedNode(
            final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> binding) {
        Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> legacyEntry = bindingToLegacy
                .toDataDom(binding);
        Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalizedEntry = legacyToNormalized
                .toNormalized(legacyEntry);
        LOG.trace("Serialization of {}, Legacy Representation: {}, Normalized Representation: {}", binding,
                legacyEntry, normalizedEntry);
        if (Augmentation.class.isAssignableFrom(binding.getKey().getTargetType())) {

            for (DataContainerChild<? extends PathArgument, ?> child : ((DataContainerNode<?>) normalizedEntry
                    .getValue()).getValue()) {
                if (child instanceof AugmentationNode) {
                    ImmutableList<PathArgument> childArgs = ImmutableList.<PathArgument> builder()
                            .addAll(normalizedEntry.getKey().getPath()).add(child.getIdentifier()).build();
                    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier childPath = new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(
                            childArgs);
                    return new SimpleEntry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>>(
                            childPath, child);
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

        PathArgument lastArgument = Iterables.getLast(normalized.getPath());
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
        AugmentationIdentifier lastArgument = (AugmentationIdentifier) Iterables.getLast(normalized.getPath());

        // Here we employ small trick - Binding-aware Codec injects an pointer
        // to augmentation class
        // if child is referenced - so we will reference child and then shorten
        // path.
        for (QName child : lastArgument.getPossibleChildNames()) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier childPath = new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(
                    ImmutableList.<PathArgument> builder().addAll(normalized.getPath()).add(new NodeIdentifier(child))
                            .build());
            try {
                if (!isNotRepresentable(childPath)) {
                    InstanceIdentifier<? extends DataObject> potentialPath = shortenToLastAugment(toBindingImpl(
                            childPath).get());
                    int potentialAugmentCount = getAugmentationCount(potentialPath);
                    if(potentialAugmentCount == normalizedCount) {
                        return Optional.<InstanceIdentifier<? extends DataObject>> of(potentialPath);
                    }
                }
            } catch (Exception e) {
                LOG.trace("Unable to deserialize aug. child path for {}", childPath, e);
            }
        }
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
        if( op.isMixin() && op.getIdentifier() instanceof NodeIdentifier) {
            return true;
        }
        if(op.isLeaf()) {
            return true;
        }
        return false;
    }

    private DataNormalizationOperation<?> findNormalizationOperation(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized)
            throws DataNormalizationException {
        DataNormalizationOperation<?> current = legacyToNormalized.getRootOperation();
        for (PathArgument arg : normalized.getPath()) {
            current = current.getChild(arg);
        }
        return current;
    }

    private static final Entry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject> toEntry(
            final org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject> key,
            final DataObject value) {
        return new SimpleEntry<org.opendaylight.yangtools.yang.binding.InstanceIdentifier<? extends DataObject>, DataObject>(
                key, value);
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
            return Optional.of(toEntry(bindingPath, bindingData));
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
        // Here we employ small trick - DataNormalizer injects augmentation
        // identifier if child is
        // also part of the path (since using a child we can safely identify
        // augmentation)
        // so, we scan augmentation for children add it to path
        // and use original algorithm, then shorten it to last augmentation
        for (@SuppressWarnings("rawtypes")
        Class augChild : getAugmentationChildren(augPath.getTargetType())) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<?> childPath = augPath.child(augChild);
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized = toNormalizedImpl(childPath);
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier potentialDiscovered = shortenToLastAugmentation(normalized);
            if (potentialDiscovered != null) {
                return potentialDiscovered;
            }
        }
        return processed;
    }

    private org.opendaylight.yangtools.yang.data.api.InstanceIdentifier shortenToLastAugmentation(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized) {
        int position = 0;
        int foundPosition = -1;
        for (PathArgument arg : normalized.getPath()) {
            position++;
            if (arg instanceof AugmentationIdentifier) {
                foundPosition = position;
            }
        }
        if (foundPosition > 0) {
            return new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(normalized.getPath().subList(0,
                    foundPosition));
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

    @SuppressWarnings("unchecked")
    private Iterable<Class<? extends DataObject>> getAugmentationChildren(final Class<?> targetType) {
        List<Class<? extends DataObject>> ret = new LinkedList<>();
        for (Method method : targetType.getMethods()) {
            Class<?> entity = getYangModeledType(method);
            if (entity != null) {
                ret.add((Class<? extends DataObject>) entity);
            }
        }
        return ret;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Class<? extends DataObject> getYangModeledType(final Method method) {
        if (method.getName().equals("getClass") || !method.getName().startsWith("get")
                || method.getParameterTypes().length > 0) {
            return null;
        }

        Class<?> returnType = method.getReturnType();
        if (DataContainer.class.isAssignableFrom(returnType)) {
            return (Class) returnType;
        } else if (List.class.isAssignableFrom(returnType)) {
            try {
                return ClassLoaderUtils.withClassLoader(method.getDeclaringClass().getClassLoader(),
                        new Callable<Class>() {

                            @SuppressWarnings("rawtypes")
                            @Override
                            public Class call() throws Exception {
                                Type listResult = ClassLoaderUtils.getFirstGenericParameter(method
                                        .getGenericReturnType());
                                if (listResult instanceof Class
                                        && DataObject.class.isAssignableFrom((Class) listResult)) {
                                    return (Class<?>) listResult;
                                }
                                return null;
                            }

                        });
            } catch (Exception e) {
                LOG.debug("Could not get YANG modeled entity for {}", method, e);
                return null;
            }

        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static InstanceIdentifier<?> toWildcarded(final InstanceIdentifier<?> orig) {
        List<org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument> wildArgs = new LinkedList<>();
        for (org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument arg : orig.getPathArguments()) {
            wildArgs.add(new Item(arg.getType()));
        }
        return InstanceIdentifier.create(wildArgs);
    }

    private static boolean isAugmentation(final Class<? extends DataObject> type) {
        return Augmentation.class.isAssignableFrom(type);
    }

    private static boolean isAugmentationIdentifier(final InstanceIdentifier<?> potential) {
        return Augmentation.class.isAssignableFrom(potential.getTargetType());
    }

    private boolean isAugmentationIdentifier(final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier processed) {
        return Iterables.getLast(processed.getPath()) instanceof AugmentationIdentifier;
    }

    private static int getAugmentationCount(final InstanceIdentifier<?> potential) {
        int count = 0;
        for(org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument arg : potential.getPathArguments()) {
            if(isAugmentation(arg.getType())) {
                count++;
            }

        }
        return count;
    }

    private static int getAugmentationCount(final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier potential) {
        int count = 0;
        for(PathArgument arg : potential.getPath()) {
            if(arg instanceof AugmentationIdentifier) {
                count++;
            }
        }
        return count;
    }
}
