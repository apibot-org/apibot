---
title: "Nodes"
date: 2017-07-12T22:58:01+02:00
draft: false
---

A node is the basic building block of an Apibot test. Every node has a different function which is determined by node's parent graph.

#### Node Parent or Parent Graph

Every node in Apibot is an instance of either a builtin graph such as the [HTTP request](/docs/graphs/http-request) or a user made graph (e.g. a graph made by you!).

A node's behavior is determined by the node's parent. If node `A`'s parent is `B` we say that node `A` is an instance of graph `B`.

#### Nodes are Functions

Every node in Apibot is a function that takes a scope as input and returns a scope. The function can be anything, from making an HTTP request and [appending the HTTP request/response to the scope](/docs/graphs/http-request), to a function that [appends some values to the scope](/docs/graphs/config).



#### Graph Reutilization:

Every graph can be re-used in other graphs by creating instances of the graph. The following screenshots illustrate this point.

![](graph-1.png)

`Graph #1` contains two nodes. The first one makes an HTTP request the second one makes an assertion.

![](graph-2.png)

`Graph #2` contains an instance of `Graph #1`. This is a very simple yet powerful way to achieve reusability. If `Graph #1` were a very common operation in your business (e.g. authentication), you can use this technique to simplify your Apibot graphs.
