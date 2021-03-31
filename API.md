# API


## Asynchrony and idiomatic event driven APIs

Using the API, it's clear that LDK tries to provide an asynchronous, event driven API.
As example:

> Implementation notes: No methods should be called on ChannelManager until after it has been synced and its chain::Watch has been given the ChannelMonitors as described in the next two steps.

Java users would look at this and say:
Why not, when building a ChannelManager, you provide me with a future or a promise,
that gets completed with an instance of `ChannelManager`,
when it has been synced?
Or, in a more rudimentary way, provide a handle that gets called back when the ChannelManager is ready to be used?

Then, in the guide you later talk about dealing with LDK events using while loops.

Clearly there needs to be some consistency around how these events are consumed.
Also, such API should make it hard to make mistakes.
You can't expect users to have an instance of say, `ChannelManager`,
and not make the mistake of using it before it's synced.
It's bound to create issues.

The issue is that achieving all of this in Java can hard to do because the best practices in this area are not fully set.
I see 3 different options you can take:


### Option 1: Do It Yourself

If your planning to be based on JDK 8 or before,
you'll definitely roll your own thing.

The best example I've seen on rolling your own thing for eventing can be found in 
[Aeron](https://github.com/real-logic/aeron/wiki/Java-Programming-Guide).
It's fully event-driven,
and its event polling is sophisticated (iow, not just a `while(true)`...).


### Option 2: CompletableFuture and Reactive Streams

When it comes to event driven systems,
there's generally 2 types of invocations:
those that produce a single result
and those that return multiple results.

The example above about creating a ChannelManager is an example of a single result.
Dealing with lightning network events is clearly a situation where you have multiple results or events.

For single results, Java has `CompletableFuture`.
You could indeed wrap your channel manager around a `CompletableFuture`,
but that construction has some gotchas,
and only covers the situation where you have an asynchronous event that returns a single item.

For dealing with asynchronous events returning more than one event,
you have Java Reactive Streams API (starting with Java 9).
This API is a bit bare and it's only the API,
no implementation provided.

The problem with this option is that it feels a bit clunky.
`CompletableFuture` and Reactive Streams were created a different stages.
Using them does not give you a lot of consistency.


### Option 3: Implementation Driven

You could use an existing implementation for dealing with all of this.
Spring, Red Hat, and others have Java middleware that can do all of this in a consistent manner.
However, that would tie you into an implementation, 
alienating other developers.

Given that I work for Red Hat,
if I was building something like this just for me,
I would use [Mutiny with Quarkus](https://quarkus.io/guides/getting-started-reactive#mutiny).
But to reiterate, others might have different opinions ;)


### Summary

There's a lot of factors to take into account here.
My general recommendation would be:

If you can, go with option 3.

If not, go with option 1.
I think you could use a lot of good practices done in Aeron and bring those in.
They've just created an
[Aeron Cookbook](https://aeroncookbook.com/cookbook/aeron/),
where they share a lot of information on how it's designed.
To be clear, I'm not advocating to use Aeron directly,
or use their networking layer.
Here I'm just focused on picking their event handling and polling aspects,
to make a consistent API that serves both single and multi event situations.


Otherwise, go with option 2,
and let the developers building against LDK decide which implementation of reactive streams to use.
Such implementations often have a way to translater between `CompletableFuture` and their own constructs.
I have this as last option because of the lack of consistency for LDK developers.


## Java NIO

This is an important topic.

I noticed that `NioPeerHandler` uses Java NIO directly.
That's notoriously difficult to get working right in all circumstances and with good enough performance.
In other words, no one would attempt to do this.

Anyone interested in using Java NIO just uses [Netty](https://netty.io/).
It's the industry standard which everyone agrees on.

I would highly recommend basing the network layer around it.

Pretty much all reactive, 
or asynchronous libraries out there,
are based on Netty.

The only exception are things like [`Aeron`](https://github.com/real-logic/aeron).
You'd use Aeron directly when you're trying to create a network of nodes in a LAN.
If you can do what you need in a LAN, Aeron will kick ass.
If you want your LDK nodes to over a WAN, then the answer is Netty.
I think LDK is the WAN business, so I think Netty is your only option.

Looking at the future, it's unclear what will happen with all this reactive,
asynchronous APIs and even Netty, 
when Java releases a non-beta version of [Loom](https://wiki.openjdk.java.net/display/loom/Main).
Along with Loom, they are revamped the synchronous IO implementation in Java.
The java architects (e.g. Brian Goetz) are confident that will virtual threads,
the majority of developers won't need the last % of performance you'd get from using asynchronous, reactive APIs.
The benefit would be that you'd have a much more friendly API (no callbacks, no futures...etc).
But, this is the future :)

Also, I'm not an Android developer, but I would expect Netty to work well there.
Netty 3.x can work with JDK 5, and Netty 3.x can run with JDK 6.
I think both of those JDKs are supported in Android.
On the other hand, it will take a very long time before Loom works on Android.


## Time

I noticed that the `KeysManager` constructor requests the user to provide the starting time.
Looking at the [example](https://lightningdevkit.org/docs/build_node#initialize-the-keysmanager),
it just gets the current time.

Rather than let the user provide that,
it's better if the LDK can be hooked with a time provider that gives you the current time.
This makes it easy to provide a test implementation,
where we can control how quickly time passes (e.g. say by incrementing by 1ms or decrease by 1ns).
It makes replicating some failure conditions more easily,
rather than being dependant on specific timing.

I've worked on distributed Java middleware before,
where we found this technique very useful to replicate obscure time related failures.


## Construction Consistency

There seems to be a lack of consistency in the API on whether constructors are called directly,
or static factory methods are invoked to construct things, e.g. `PeerManager.constructor_new` vs `new NioPeerHandler`.

Static methods are often useful when you want to hide details on implementations.
E.g. [Java Executors static factory method](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executors.html).

You can also adopt a more flexible way to do things is to use some kind of fluent mutable builder API,
and the invoke some kind of build method that uses all the parameters you've passed in,
and creates some kind of immutable object.
One of the best examples of this I've seen is [Javapoet](https://github.com/square/javapoet).

I think the fluent mutable builder API is also interesting when you have to pass many parameters to construct something.
E.g. it's a bit of pain to try to build a method invocation that takes 10 parameters.
You have to start looking at each parameter, which type it is, the order to pass in...etc.
With a fluent mutable builder API, you pass each parameter in a single call, in any order.

The downside of fluent mutable builder API is the added complexity to support such an API.
From a user perspective it all looks nice,
but it's a pain to maintain it.

My feeling is that LDK fits somewhere between static factory methods and fluent builder API.
Some of its static factory methods take a lot of parameters.
If that can be reduced somehow then I'd favour static factory methods.
As an example: `ChainMonitor` static factory takes both `tx_broadcaster` and `fee_estimator`.
Then, when constructing `ChannelManager` you pass in those two parameters,
as well as as a `ChainMonitor` as a watch.
Maybe things can be tidied up to have more manageable static factory methods.

Also, I would often use words like `of` or `from` rather than `constructor_new`.
E.g. `PeerManager.of()`.


## KeysManager constructor parameters

Aside from where the time comes from,
I noticed that KeysManager constructor takes the current time in both secs and nanoseconds.
That sounds odd.
Why do you need both parameters?
Can't you derive the seconds from the nanoseconds
(e.g. using `TimeUnit.NANOSECONDS.toSeconds(nanos)`) ? 


## Key seed management

In initializing `KeysManager` [section](https://lightningdevkit.org/docs/build_node#initialize-the-keysmanager),
it says:

> Note that you must write the key_seed you give to the KeysManager on startup to disk,
> and keep using it to initialize the KeysManager every time you restart.
> This key_seed is used to derive your node's secret key (which corresponds to its node pubkey) and all other secret key material.

I was wondering, can't LDK take of this?
It can be hooked with a persister, 
which could potentially persist it and restore it on restart.
The comment above seems to indicate that it's the user's responsibility to write it to disk and restore it.


## Fresh Start vs Restart

While reading [the guide](https://lightningdevkit.org/docs/build_node/),
I noticed that in several places the user has to write different code depending on whether doing a fresh start or a restart.
I feel it would really help the user if there was no distinction at all,
and LDK would take care of differentiation between the two situations.

My previous comment on key seed management is motivated by this.


## Channel identifiers

Channels have ids associated with them, represented by the `OutPoint` class.
As noted in the [persist section in the build node guide](https://lightningdevkit.org/docs/build_node#initialize-persist),
you'd expect to write the channel data keyed by instances of `OutPoint`.

However, `OutPoint` does not override `equals` nor `hashCode`,
hence it does not take the contents of `OutPoint` into account to see if two instances are the same.
By default, two instances are equals if they point to the same object,
not if the contents are the same.
For anything you want as key in a map or dictionary, 
you want the latter property.
Whenever you override `equals`, 
also override `hashCode` to avoid surprises in structures such as hash maps.
Unless you're using a Java version that includes records,
I normally let the IDE (e.g. IntelliJ) generate them for me.
They allow you to select which fields to use for computing `equals`/`hashCode`.
More details [here](https://www.baeldung.com/java-equals-hashcode-contracts).


## Finalizers

I've noticed that some classes, e.g. `OutPut` rely on Java finalizers.
Finalizers in Java are generally considered a bad idea.
[This article](https://www.baeldung.com/java-finalize) explains the pitfalls in detail.

The best practice for resource management in Java these days,
is to make your classes extend `AutoCloseable`, 
which then enables you to pass them into a try-with-resources construct.
See [detailed explanation](https://www.baeldung.com/java-try-with-resources).


## NioPeerHandler

Assuming the current status quo (using Java NIO directly),
how does someone close the sockets that `NioPeerHandler` uses?
As per the previous section, 
the general consensus is that it should extend `AutoCloseable`,
and close any socket or other resources it has created in the `close` method implementation.


## Style

The JDK and majority of Java libraries use camel case style throughout their source.
That means it often applies both to method names, variable names, class names...etc.
Snake case is seldom used, though I've seen it used very sparingly in Java source code that is not exposed to users.


## `TwoTuple` vs Nominal Tuples

With Java records, 
the Java community has a made a decision towards avoiding box standard tuple types.

Even though LDK is unlikely to be based on a Java version that has records in it,
I would stay away from adding tuple types.
Instead, I would just create nominal classes.

So, I would remove `TwoTuple`.


## Documentation

ChannelManager fresh node example outdated 
[here](https://lightningdevkit.org/docs/build_node#initialize-the-channelmanager).
`LDKNetwork` is no longer passed as first parameter, but as 3rd parameter before last.

Code in `channel_manager_constructor.chain_sync_completed();`
looks to be related to the 
["Sync ChannelMonitors and ChannelManager to chain tip" section](https://lightningdevkit.org/docs/build_node/#sync-channelmonitors-and-channelmanager-to-chain-tip).
However, it currently appears under ["Give ChannelMonitors to ChainMonitor#" section](https://lightningdevkit.org/docs/build_node/#give-channelmonitors-to-chainmonitor),
which does not have any code in it.


## params_latest_hash_arg

When constructing a new `ChannelManager`, 
what should be passed in to params_latest_hash_arg for a fresh node?
This is not included in the [guide](https://lightningdevkit.org/docs/build_node/#initialize-the-channelmanager).

`params_latest_hash_arg` validation a bit confusing.
I tried to pass in a byte[] of length 0 and it returned:

```bash
java.lang.ArrayIndexOutOfBoundsException: Array region 0..32 out of bounds for length 0
at org.example.ldk.node.NodeTest.buildANode(NodeTest.java:78)
```

Then I tried to pass in 3 random values and got:

```bash
java.lang.ArrayIndexOutOfBoundsException: Array region 0..32 out of bounds for length 3
at org.example.ldk.node.NodeTest.buildANode(NodeTest.java:78)
```

Aside from what value is required,
it's not clear from the exception which field is causing issues.
In this case, there's only one array field but if you have more it could be confusing.


## Testing


It could be nice for LDK to provide a test API that can make it easy to test LDK nodes.

Although [the guide](https://lightningdevkit.org/docs/build_node/) does help build the node,
it has blanks for the information that comes externally, e.g. block information. 

As a first test, you could have a test guide showing how to use `bitcoinj`,
or other dependencies, to generate info coming externally.
Lessons from 
[`PeerTest`](https://github.com/lightningdevkit/ldk-garbagecollected/blob/main/src/test/java/org/ldk/PeerTest.java)
could help drive that?
