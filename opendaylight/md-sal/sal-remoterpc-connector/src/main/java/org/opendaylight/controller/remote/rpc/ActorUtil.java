/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class ActorUtil {
  public static final FiniteDuration LOCAL_ASK_DURATION = Duration.create(2, TimeUnit.SECONDS);
  public static final FiniteDuration REMOTE_ASK_DURATION = Duration.create(15, TimeUnit.SECONDS);
  public static final FiniteDuration ASK_DURATION = Duration.create(17, TimeUnit.SECONDS);
  public static final FiniteDuration LOCAL_AWAIT_DURATION = Duration.create(2, TimeUnit.SECONDS);
  public static final FiniteDuration REMOTE_AWAIT_DURATION = Duration.create(15, TimeUnit.SECONDS);
  public static final FiniteDuration AWAIT_DURATION = Duration.create(17, TimeUnit.SECONDS);

  /**
   * Executes an operation on a local actor and wait for it's response
   * @param actor
   * @param message
   * @param askDuration
   * @param awaitDuration
   * @return The response of the operation
   */
  public static Object executeLocalOperation(ActorRef actor, Object message,
                                      FiniteDuration askDuration, FiniteDuration awaitDuration) throws Exception{
    Future<Object> future =
        ask(actor, message, new Timeout(askDuration));

      return Await.result(future, awaitDuration);
  }

  /**
   * Execute an operation on a remote actor and wait for it's response
   * @param actor
   * @param message
   * @param askDuration
   * @param awaitDuration
   * @return
   */
  public static Object executeRemoteOperation(ActorSelection actor, Object message,
                                              FiniteDuration askDuration, FiniteDuration awaitDuration) throws Exception{
    Future<Object> future =
        ask(actor, message, new Timeout(askDuration));
      return Await.result(future, awaitDuration);
  }

}
