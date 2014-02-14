package org.opendaylight.controller.md.sal.common.impl.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.yangtools.concepts.Path;

public class RootedChangeSet<P extends Path<P>,D> {

    private final P root;
    private final Map<P,D> original;
    private final Map<P,D> created = new HashMap<>();
    private final Map<P,D> updated = new HashMap<>();
    private final Set<P> removed = new HashSet<>();



    public RootedChangeSet(P root,Map<P, D> original) {
        super();
        this.root = root;
        this.original = original;
    }

    protected P getRoot() {
        return root;
    }

    protected Map<P, D> getOriginal() {
        return original;
    }

    protected Map<P, D> getCreated() {
        return created;
    }

    protected Map<P, D> getUpdated() {
        return updated;
    }

    protected Set<P> getRemoved() {
        return removed;
    }

    public void addCreated(Map<P,D> created) {
        this.created.putAll(created);
    }

    public void addCreated(Entry<P,D> entry) {
        created.put(entry.getKey(), entry.getValue());
    }

    public void addUpdated(Entry<P,D> entry) {
        updated.put(entry.getKey(), entry.getValue());
    }

    public void addRemoval(P path) {
        removed.add(path);
    }

    public boolean isChange() {
        return !created.isEmpty() || !updated.isEmpty() || !removed.isEmpty();
    }
}
