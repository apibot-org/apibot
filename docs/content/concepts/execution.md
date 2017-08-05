---
title: "Execution"
date: 2017-07-12T22:58:01+02:00
draft: false
---

The algorithm for executing graphs is actually very simple. Apibot first picks the starting node (which is the only node in the graph without predecessors), invokes the node's function and then concurrently repeats this procedure for every successor of the current node.

![](line.png)

In the image above, the `first` node is executed first, followed by `second` and finally `third`.

![](fork-join.png)

If a node has more than one predecessor, then the node will wait until all predecessors have been executed. In the image above, the two `third` nodes will be executed concurrently (although not necessarily in parallel). When both `third` nodes are executed then the `fourth` will follow.
