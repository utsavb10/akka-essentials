# Actors

With traditional objects:
- we store their state as data
- we call their methods

With actors:
- we store their state as data (private data)
- way to communicate with an actor is by sending messages to them, asynchronously

> Actors are objects we can't access directly, but only send messages to.


# How Akka works

Akka has a thread pool that it shares with actors

Actor (on a high level) has a messageHandler and a messageQueue(mailbox). It is passive data structure that needs threads to run.

So a few threads handle a huge amount of actors.

Akka schedules actors for execution

# Communication

Sending a message -> message is enqueued in the actor's mailbox (done by Akka in a thread-safe manner)

Processing a message ->
- a thread is scheduled to run this actor
- messages are extracted from the mailbox, in order
- the thread invokes the message handler on each message
- at some point the actor is unscheduled and thread goes on to do something else


## Guarantees we wet

Only one thread operates on an actor at a time
- actors are effectively single-threaded
- no locks needed !!
- processing messages is atomic

Message delivery guarantees
- at most once delivery
- for any sender-receiver pair, the message order is maintained


# Supervision

We can decide what to do with an actor in case of failure
- stop it (default)
- resume it; failure ignored, actor state preserved
- restart it; actor state reset

#### Advice
- handle multiple exception types by nesting Behaviors.supervise
- place the most specific exception in the inner handlers and the most general exception in the outer handlers
