/*
 * Copyright (c) 2019 PANTHEON.tech. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin;

import akka.dispatch.OnComplete;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.function.Function;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

final class ScalaGuavaBridge {
    private ScalaGuavaBridge() {

    }

    static <T, U> ListenableFuture<U> transform(final Future<T> scalaFuture, final Function<T, U> transform) {
        final SettableFuture<U> ret = SettableFuture.create();
        scalaFuture.onComplete(new OnComplete<T>() {
            @Override
            public void onComplete(final Throwable failure, final T success) {
                if (failure != null) {
                    ret.setException(failure);
                    return;
                }

                final U result;
                try {
                    result = transform.apply(success);
                } catch (Throwable e) {
                    ret.setException(e);
                    return;
                }

                ret.set(result);
            }

        }, ExecutionContext.global());
        return ret;
    }
}
