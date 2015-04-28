/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DataTreeChangeTracker {

    private final ModifyAction defaultAction;

    private final Deque<ModifyAction> actions;
    private final Deque<PathArgument> currentPath;
    private final ArrayList<DataTreeChange> dataTreeChanges;
    private int deleteOperationTracker = 0;
    private int removeOperationTracker = 0;

    public DataTreeChangeTracker(final ModifyAction defaultAction) {
        this.defaultAction = defaultAction;
        this.currentPath = new ArrayDeque<>();
        this.actions = new ArrayDeque<>();
        this.dataTreeChanges = new ArrayList<>();
    }

    public void pushAction(final ModifyAction action) {
        if (ModifyAction.DELETE.equals(action)) {
            deleteOperationTracker++;
        }

        if (ModifyAction.REMOVE.equals(action)) {
            removeOperationTracker++;
        }
        this.actions.push(action);
    }

    public ModifyAction peekAction() {
        return this.actions.peekFirst();
    }

    public ModifyAction popAction() {
        final ModifyAction popResult = actions.pop();
        if (ModifyAction.DELETE.equals(popResult)) {
            deleteOperationTracker--;
        }

        if (ModifyAction.REMOVE.equals(popResult)) {
            removeOperationTracker--;
        }
        return popResult;
    }

    public int getDeleteOperationTracker() {
        return deleteOperationTracker;
    }

    public int getRemoveOperationTracker() {
        return removeOperationTracker;
    }

    public void addDataTreeChange(final DataTreeChange change) {
        dataTreeChanges.add(change);
    }

    public ArrayList<DataTreeChange> getDataTreeChanges() {
        return dataTreeChanges;
    }

    public ModifyAction getDefaultAction() {
        return defaultAction;
    }

    public void pushPath(final PathArgument pathArgument) {
        currentPath.push(pathArgument);
    }

    public PathArgument popPath() {
        return currentPath.pop();
    }

    public Deque<PathArgument> getCurrentPath() {
        return currentPath;
    }


    public static final class DataTreeChange {

        private final NormalizedNode<?, ?> changeRoot;
        private final ModifyAction action;
        private final List<PathArgument> path;

        public DataTreeChange(final NormalizedNode<?, ?> changeRoot, final ModifyAction action, final ArrayList<PathArgument> path) {
            this.changeRoot = changeRoot;
            this.action = action;
            this.path = Lists.reverse(path);
        }

        public NormalizedNode<?, ?> getChangeRoot() {
            return changeRoot;
        }

        public ModifyAction getAction() {
            return action;
        }

        public List<PathArgument> getPath() {
            return path;
        }
    }
}
