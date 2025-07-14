/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link ClassLoader} using a list of delegates to look up classes. The order is significant as it defines the order
 * in which delegates will be queried. This is primarily used by {@link RestrictedObjectInputStream}.
 */
final class RestrictedClassLoader extends ClassLoader {
    static {
        verify(registerAsParallelCapable());
    }

    private final List<ClassLoader> delegates;

    @NonNullByDefault
    RestrictedClassLoader(final List<ClassLoader> delegates) {
        // TODO: do we actually split the first class loader to be the parent?
        //       doing so would allow us to have a specialization for empty delegates, i.e. just handle the case where
        //       the first class loader does not find anything.
        super(null);
        this.delegates = requireNonNull(delegates);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        // handle primitive types as ObjectInputStream would, otherwise defer to delegates
        return switch (name) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "void" -> void.class;
            default -> findDelegateClass(name);
        };
    }

    private Class<?> findDelegateClass(final String name) throws ClassNotFoundException {
        // Optimistic: only allocate if we have a failure
        List<ClassNotFoundException> failures = null;
        for (var delegate : delegates) {
            try {
                return delegate.loadClass(name);
            } catch (ClassNotFoundException e) {
                if (failures == null) {
                    failures = new ArrayList<>();
                }
                failures.add(e);
            }
        }

        final var cnfe = new ClassNotFoundException(name);
        if (failures != null) {
            failures.forEach(cnfe::addSuppressed);
        }
        throw cnfe;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("delegates", delegates).toString();
    }
}
