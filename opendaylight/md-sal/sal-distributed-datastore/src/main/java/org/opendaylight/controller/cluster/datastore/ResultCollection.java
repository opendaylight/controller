/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.concurrent.GuardedBy;

final class ResultCollection<V> {

    private static final ResultCollection<Object> EMPTY_SUCCESSFUL =
            new ResultCollection<Object>(ImmutableList.<Object>of(), ImmutableList.<Throwable>of());
    private static final ListenableFuture<ResultCollection<Object>> EMPTY_SUCCESSFUL_FUTURE =
            Futures.immediateFuture(EMPTY_SUCCESSFUL);

    private final Collection<V> values;
    private final Collection<Throwable> throwables;


    private ResultCollection(Collection<V> values, Collection<Throwable> throwables) {
        super();
        this.values = ImmutableList.copyOf(values);
        this.throwables = ImmutableList.copyOf(throwables);
    }


    Collection<Throwable> getThrowables() {
        return throwables;
    }

    Collection<V> getValues() {
        return values;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <V> ListenableFuture<ResultCollection<V>> emptyFutureResult() {
        return (ListenableFuture) EMPTY_SUCCESSFUL_FUTURE;
    }

    static <V> ListenableFuture<ResultCollection<V>> fromFutures(
            Collection<? extends ListenableFuture<? extends V>> futures) {
        if (futures.isEmpty()) {
            return emptyFutureResult();
        }

        SettableFuture<ResultCollection<V>> resultFuture = SettableFuture.create();
        FutureResultCollector<V> combiner = new FutureResultCollector<>(resultFuture, futures.size());
        for (ListenableFuture<? extends V> future : futures) {
            Futures.addCallback(future, combiner);
        }
        return resultFuture;
    }

    private static class FutureResultCollector<V> implements FutureCallback<V> {

        final int totalCount;
        @GuardedBy("this")
        final Collection<V> values;
        @GuardedBy("this")
        final Collection<Throwable> exceptions;
        final SettableFuture<ResultCollection<V>> toComplete;

        FutureResultCollector(SettableFuture<ResultCollection<V>> toComplete, int countToSuccess) {
            this.totalCount = countToSuccess;
            this.toComplete = Preconditions.checkNotNull(toComplete);

            this.values = new ArrayList<>(countToSuccess);
            this.exceptions = new ArrayList<>();
        }

        @Override
        public synchronized void onSuccess(V result) {
            values.add(result);
            tryToComplete();
        }


        @Override
        public synchronized void onFailure(Throwable t) {
            exceptions.add(t);
            tryToComplete();
        }

        private void tryToComplete() {
            if (totalCount == (values.size() + exceptions.size())) {
                toComplete.set(new ResultCollection<>(values, exceptions));
            }
        }

    }

}