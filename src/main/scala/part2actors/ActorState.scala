package part2actors

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

/**
 * Tips: HOW to turn Stateful behavior to Stateless behavior
 *  - each var/mutable field becomes an immutable METHOD ARGUMENT
 *  - each state change = new behavior obtained by calling the method with a different argument
 */
object ActorState {

  /*
    Exercise: use the setup method to create a word counter which
      - splits each message into words
      - keeps track of the TOTAL number of words received so far
      - log the current # of words + TOTAL # of words
   */

  object WordCount2 {
    def apply(): Behavior[String] = Behaviors.setup { context =>

      /**
       * Use of fields here creates a STATEFUL word counter
       */
      var totalWords = 0
      val logger = context.log

      Behaviors.receiveMessage { message =>
        val words: Array[String] = message.split(' ')
        totalWords += words.length

        logger.info(s"Received ${words.length} words, total count is $totalWords")

        Behaviors.same
      }
    }
  }

  def demoWordCount2() {
    val actorSystem = ActorSystem(WordCount2(), "WordCount_2")

    actorSystem ! "first wrods"
    actorSystem ! "all my friends are heathens, take it slow"
    actorSystem ! "HEATHENS !!!!!!!!!!!!!!!"

    Thread.sleep(3000)
    actorSystem.terminate()
  }

  object WordCount2_Stateless {
    def apply(): Behavior[String] = statelessCount(0)

    def statelessCount(totalWords: Int): Behavior[String] = Behaviors.setup { context =>
      val logger = context.log

      Behaviors.receiveMessage { message =>
        val words: Array[String] = message.split(' ')
        val total = words.length + totalWords
        logger.info(s"Received ${words.length} words, total count is $total")
        statelessCount(words.length + totalWords) // return the behavior from this stateMethod
      }
    }
  }

  def demoWordCount2_Stateless() {
    val actorSystem = ActorSystem(WordCount2_Stateless(), "WordCount_2_Stateless")

    actorSystem ! "first wrods"
    actorSystem ! "all my friends are heathens, take it slow"
    actorSystem ! "HEATHENS !!!!!!!!!!!!!!!"

    Thread.sleep(3000)
    actorSystem.terminate()
  }


  object WordCounter {
    def apply(): Behavior[String] = Behaviors.setup { context =>
      var total = 0

      Behaviors.receiveMessage { message =>
        val newCount = message.split(" ").length
        total += newCount
        context.log.info(s"Message word count: $newCount - total count: $total")
        Behaviors.same
      }
    }
  }

  trait SimpleThing
  case object EatChocolate extends SimpleThing
  case object CleanUpTheFloor extends SimpleThing
  case object LearnAkka extends SimpleThing
  /*
    Message types must be IMMUTABLE and SERIALIZABLE.
    - use case classes/objects
    - use a flat type hierarchy
   */

  object SimpleHuman {
    def apply(): Behavior[SimpleThing] = Behaviors.setup { context =>
      var happiness = 0

      Behaviors.receiveMessage {
        case EatChocolate =>
          context.log.info(s"[$happiness] Eating chocolate")
          happiness += 1
          Behaviors.same
        case CleanUpTheFloor =>
          context.log.info(s"[$happiness] Wiping the floor, ugh...")
          happiness -= 2
          Behaviors.same
        case LearnAkka =>
          context.log.info(s"[$happiness] Learning Akka, YAY!")
          happiness += 99
          Behaviors.same
      }
    }
  }

  def demoSimpleHuman(): Unit = {
    val human = ActorSystem(SimpleHuman_V2(), "DemoSimpleHuman")

    human ! LearnAkka
    human ! EatChocolate
    (1 to 30).foreach(_ => human ! CleanUpTheFloor)

    Thread.sleep(1000)
    human.terminate()
  }

  /*
  Not as same as recursion --> new behavior obtained will be used to process the next incoming message sometime in the future
   */
  object SimpleHuman_V2 {
    def apply(): Behavior[SimpleThing] = statelessHuman(0)

    def statelessHuman(happiness: Int): Behavior[SimpleThing] = Behaviors.receive { (context, message) =>
      message match {
        case EatChocolate =>
          context.log.info(s"[$happiness] Eating chocolate")
          statelessHuman(happiness + 1)
        case CleanUpTheFloor =>
          context.log.info(s"[$happiness] Wiping the floor, ugh...")
          statelessHuman(happiness - 2)
        case LearnAkka =>
          context.log.info(s"[$happiness] Learning Akka, YAY!")
          statelessHuman(happiness + 99)
      }
    }
  }

  /**
   * Exercise: refactor the "stateful" word counter into a "stateless" version.
   */
  object WordCounter_V2 {
    def apply(): Behavior[String] = active(0)

    def active(total: Int): Behavior[String] = Behaviors.setup { context =>
      Behaviors.receiveMessage { message =>
        val newCount = message.split(" ").length
        context.log.info(s"Message word count: $newCount - total count: ${total + newCount}")
        active(total + newCount)
      }
    }
  }

  def demoWordCounter(): Unit = {
    val wordCounter = ActorSystem(WordCounter_V2(), "WordCounterDemo")

    wordCounter ! "I am learning Akka"
    wordCounter ! "I hope you will be stateless one day"
    wordCounter ! "Let's see the next one"

    Thread.sleep(1000)
    wordCounter.terminate()
  }

  def main(args: Array[String]): Unit = {
//    demoWordCounter()
    demoWordCount2()
    demoWordCount2_Stateless()
  }
}
