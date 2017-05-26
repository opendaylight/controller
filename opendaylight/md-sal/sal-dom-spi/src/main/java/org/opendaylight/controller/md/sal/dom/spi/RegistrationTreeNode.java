/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a single node within the registration tree. Note that the data returned from
 * and instance of this class is guaranteed to have any relevance or consistency
 * only as long as the {@link RegistrationTreeSnapshot} instance through which it is reached
 * remains unclosed.
 *
 * @param <T> registration type
 * @author Robert Varga
 */
public final class RegistrationTreeNode<T> implements Identifiable<PathArgument> {
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationTreeNode.class);

    private final Map<PathArgument, RegistrationTreeNode<T>> children = new HashMap<>();
    private final Collection<T> registrations = new ArrayList<>(2);
    private final Collection<T> publicRegistrations = Collections.unmodifiableCollection(registrations);
    private final Reference<RegistrationTreeNode<T>> parent;
    private final PathArgument identifier;

    RegistrationTreeNode(final RegistrationTreeNode<T> parent, final PathArgument identifier) {
        this.parent = new WeakReference<>(parent);
        this.identifier = identifier;
    }

    @Override
    public PathArgument getIdentifier() {
        return identifier;
    }

    /**
     * Return the child matching a {@link PathArgument} specification.
     *
     * @param arg Child identifier
     * @return Child matching exactly, or null.
     */
    public RegistrationTreeNode<T> getExactChild(@Nonnull final PathArgument arg) {
        return children.get(Preconditions.checkNotNull(arg));
    }

    /**
     * Return a collection children which match a {@link PathArgument} specification inexactly.
     * This explicitly excludes the child returned by {@link #getExactChild(PathArgument)}.
     *
     * @param arg Child identifier
     * @return Collection of children, guaranteed to be non-null.
     */
    public @Nonnull Collection<RegistrationTreeNode<T>> getInexactChildren(@Nonnull final PathArgument arg) {
        Preconditions.checkNotNull(arg);
        if (arg instanceof NodeWithValue || arg instanceof NodeIdentifierWithPredicates) {
            /*
             * TODO: This just all-or-nothing wildcards, which we have historically supported. Given
             *       that the argument is supposed to have all the elements filled out, we could support
             *       partial wildcards by iterating over the registrations and matching the maps for
             *       partial matches.
             */
            final RegistrationTreeNode<T> child = children.get(new NodeIdentifier(arg.getNodeType()));
            if (child == null) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(child);
            }
        } else {
            return Collections.emptyList();
        }
    }

    public Collection<T> getRegistrations() {
        return publicRegistrations;
    }

    RegistrationTreeNode<T> ensureChild(@Nonnull final PathArgument child) {
        RegistrationTreeNode<T> potential = children.get(Preconditions.checkNotNull(child));
        if (potential == null) {
            potential = new RegistrationTreeNode<>(this, child);
            children.put(child, potential);
        }
        return potential;
    }

    void addRegistration(@Nonnull final T registration) {
        registrations.add(Preconditions.checkNotNull(registration));
        LOG.debug("Registration {} added", registration);
    }

    void removeRegistration(@Nonnull final T registration) {
        registrations.remove(Preconditions.checkNotNull(registration));
        LOG.debug("Registration {} removed", registration);

        // We have been called with the write-lock held, so we can perform some cleanup.
        removeThisIfUnused();
    }

    private void removeThisIfUnused() {
        final RegistrationTreeNode<T> p = parent.get();
        if (p != null && registrations.isEmpty() && children.isEmpty()) {
            p.removeChild(identifier);
        }
    }

    private void removeChild(final PathArgument arg) {
        children.remove(arg);
        removeThisIfUnused();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("identifier", identifier)
                .add("registrations", registrations.size())
                .add("children", children.size()).toString();
    }
}
