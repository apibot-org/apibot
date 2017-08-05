---
title: "Reusable Graphs"
date: 2017-07-10T00:27:23+02:00
draft: true
---

This tutorial will teach you how to create reusable components. This technique will help you build more maintainable integration tests and facilitate the creation of new ones.

In this tutorial we will be consuming the [Coindesk](http://www.coindesk.com/api/) API which provides information on the price of bitcoin.

### Composing Graphs

[As we've learned already](/docs/tutorials/getting-started), a graph is composed of nodes and edges. Nodes can perform different functions like sending HTTP requests or making assertions. One
