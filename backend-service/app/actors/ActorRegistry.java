/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.routing.SmallestMailboxPool;
import scala.concurrent.ExecutionContext;


/**
 * Created by zechen on 9/4/15.
 */
public class ActorRegistry {
  private static ActorSystem actorSystem = ActorSystem.create("WhereHowsETLService");

  public static ExecutionContext dispatcher = actorSystem.dispatcher();

  public static Scheduler scheduler = actorSystem.scheduler();

  public static ActorRef schedulerActor = actorSystem.actorOf(Props.create(SchedulerActor.class), "SchedulerActor");

  public static ActorRef etlJobActor =
      actorSystem.actorOf(new SmallestMailboxPool(10).props(Props.create(EtlJobActor.class)), "EtlJobActor");

  public static ActorRef treeBuilderActor = actorSystem.actorOf(Props.create(TreeBuilderActor.class), "TreeBuilderActor");

  public static ActorRef kafkaConsumerMaster =
      actorSystem.actorOf(Props.create(KafkaConsumerMaster.class), "KafkaMaster");
}
