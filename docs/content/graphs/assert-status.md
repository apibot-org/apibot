---
title: "Assert Status"
date: 2017-07-13T00:25:24+02:00
draft: false
---

### Description

The Assert Status is a type of assertion that verifies the HTTP status code of the last HTTP Response. The graph's execution will be halted if any of the following conditions occur:

1. The Http Response's status is not in the range of `from <= status <= to`.
2. There is no Http Response in the scope.

### Arguments

* from: The lower bound HTTP status code.
* to: The upper bound HTTP status code.
