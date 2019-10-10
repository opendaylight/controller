/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.agc;

import akka.dispatch.OnComplete;
import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.eclipse.jdt.annotation.NonNull;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

/**
 * Utility methods for converting between {@link ListenableFuture}s and {@link Future}s.
 */
@Beta
public final class FutureConverters {
    private FutureConverters() {

    }

    public static <T> @NonNull ListenableFuture<T> toListenableFuture(final Future<T> scalaFuture,
            final ExecutionContext executionContext) {
        final SettableFuture<T> ret = SettableFuture.create();
        scalaFuture.onComplete(new OnComplete<T>() {
            @Override
            public void onComplete(final Throwable failure, final T success) {
                if (failure != null) {
                    ret.setException(failure);
                } else {
                    ret.set(success);
                }
            }
        }, executionContext);
        return ret;
    }
}
