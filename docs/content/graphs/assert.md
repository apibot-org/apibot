---
title: "Assert"
date: 2017-07-13T00:25:24+02:00
draft: false
---

### Description

Performs an assertion over the scope.

### Arguments

A javascript function which takes the scope as argument and returns true or false.

----

**Example**: Checking that the scope contains a specific key.

```
(scope) => {
  return scope.username !== null;
}
```

**Example**: Checking that the scope contains a specific value.

```
(scope) => {
  return scope.user === {id: "12345", name: "Bob"};
}
```
