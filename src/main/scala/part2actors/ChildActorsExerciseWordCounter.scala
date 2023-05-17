package part2actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.collection.mutable

object ChildActorsExerciseWordCounter {

  trait MasterProtocol // messages supported by master

  trait WorkerProtocol

  trait UserProtocol

  // messages to Master

  // Agg -> Master
  case class Initialize(nChildren: Int) extends MasterProtocol

  // Anyone ---(computeTask)--> Master
  case class WordCountTask(text: String, replyTo: ActorRef[UserProtocol]) extends MasterProtocol

  // Worker ----------> Master
  case class WordCountReply(id: Int, count: Int) extends MasterProtocol

  //messages to worker
  case class WorkerTask(id: Int, text: String) extends WorkerProtocol

  //message to Requester
  case class Reply(count: Int) extends UserProtocol

  // recipient of tasks
  object WordCounterMaster {
    def apply(): Behavior[MasterProtocol] = active(Map(), 0, -1, -1, Map())

    def active(
                workerMap: Map[Int, ActorRef[WorkerProtocol]],
                nChildren: Int,
                currentAvailableWorker: Int,
                currentTaskId: Int,
                requestMap: Map[Int, ActorRef[UserProtocol]]
              ): Behavior[MasterProtocol] = Behaviors.receive { (context, message) =>
      val logger = context.log

      message match {
        case Initialize(nChildren) => {
          if (nChildren == 0) {
            Behaviors.empty
          }
          var map = mutable.Map[Int, ActorRef[WorkerProtocol]]()
          (0 until nChildren)
            .foreach(i => {
              val worker = context.spawn(WordCounterWorker(context.self), s"worker_$i")
              map.put(i, worker)
              logger.info(s"Created worker names ${worker.path.name}")
            })
          active(map.toMap, nChildren, 0, 0, requestMap)
        }

        case WordCountTask(text, replyTo) => {
          if (currentAvailableWorker == -1) {
            logger.info("No worker available")
            active(workerMap, nChildren, currentAvailableWorker, currentTaskId, requestMap)
          }

          val nextWorkerId = (currentAvailableWorker + 1) % nChildren
          val nextTaskId = currentTaskId + 1

          workerMap.get(currentAvailableWorker)
            .fold(logger.warn(s"[master] Could not find worker $currentAvailableWorker")){ worker =>
              logger.info(s"[master] Sending counting task to worker $currentAvailableWorker")
              worker ! WorkerTask(currentTaskId, text)
            }
          active(workerMap, nChildren, nextWorkerId, nextTaskId, requestMap + (currentTaskId-> replyTo))
        }

        case WordCountReply(id, count) => {
          logger.info(s"[master] reply received from worker $id, count $count")
          requestMap(id) ! Reply(count)
          active(workerMap, nChildren, currentAvailableWorker, currentTaskId, requestMap - id)
        }
      }
    }
  }

  object WordCounterWorker {
    def apply(master: ActorRef[MasterProtocol]): Behavior[WorkerProtocol] = active(master)

    def active(master: ActorRef[MasterProtocol]): Behavior[WorkerProtocol] = Behaviors.receive{ (context, message) =>
      val logger = context.log

      message match {
        case WorkerTask(id, text) => {
          val totalWords = text.split(' ').length
          logger.info(s"[worker] Counted words to be $totalWords for worker id $id")
          master ! WordCountReply(id, totalWords)
        }
        Behaviors.same
      }
    }
  }

  // to test
  object Aggregator {
    def apply(): Behavior[UserProtocol] = active(0)

    def active(totalWords: Int): Behavior[UserProtocol] = Behaviors.receive { (context, message) =>
      val logger = context.log
      message match {
        case Reply(count) =>
          val total = totalWords + count
          logger.info(s"[Aggregator] I have received count ie $count, total is $total")
          active(total)
      }
    }
  }


  def test(): Unit = {
    val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>

      val aggregator = context.spawn(Aggregator(), "Aggregator")
      val wcm = context.spawn(WordCounterMaster(), "wordCountMaster")

      wcm ! Initialize(3)
      wcm ! WordCountTask("I love waka waka", aggregator)
      wcm ! WordCountTask("samina mina mina", aggregator)
      wcm ! WordCountTask("eh eh, waka waka eh eh", aggregator)
      wcm ! WordCountTask("Tanjiro kun dosta, taskete", aggregator)

      Behaviors.empty
    }

    val system = ActorSystem(userGuardianBehavior, "userGuardianActor")
    Thread.sleep(1000)
    system.terminate()
  }

  def main(args: Array[String]): Unit = {
    test()
  }
}
