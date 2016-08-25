/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.ExplicitAskSupport;
import akka.util.Timeout;
import com.google.common.annotations.Beta;
import java.util.function.Function;
import scala.Function1;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;

/**
 * Unfortunately Akka's explicit ask pattern does not work with its Java API, as it fails to invoke passed message.
 * In order to make this work for now, we tap directly into ExplicitAskSupport and use a Scala function instead
 * of akka.japi.Function.
 *
 * @author Robert Varga
 */
@Beta
public final class ExplicitAsk {
    private static final ExplicitAskSupport ASK_SUPPORT = akka.pattern.extended.package$.MODULE$;

    private ExplicitAsk() {
        throw new UnsupportedOperationException();
    }

    public static <T> Function1<ActorRef, T> toScala(final Function<ActorRef, T> function) {
        return new AbstractFunction1<ActorRef, T>() {
            @Override
            public T apply(final ActorRef askSender) {
                return function.apply(askSender);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Future<Object> ask(final ActorRef actor, final Function1<ActorRef, ?> function,
            final Timeout timeout) {
        return ASK_SUPPORT.ask(actor, (Function1<ActorRef, Object>)function, timeout);
    }

    @SuppressWarnings("unchecked")
    public static Future<Object> ask(final ActorSelection actor, final Function1<ActorRef, ?> function,
            final Timeout timeout) {
        return ASK_SUPPORT.ask(actor, (Function1<ActorRef, Object>)function, timeout);
    }

    public static Future<Object> ask(final ActorRef actor, final Function<ActorRef, ?> function, final Timeout timeout) {
        return ask(actor, toScala(function), timeout);
    }

    public static Future<Object> ask(final ActorSelection actor, final Function<ActorRef, ?> function,
            final Timeout timeout) {
        return ask(actor, toScala(function), timeout);
    }
}
