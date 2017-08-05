---
title: "Assert Body"
date: 2017-07-13T00:25:24+02:00
draft: false
---

### Description

Performs an assertion over the last HTTP request's body.

### Arguments

A javascript function which takes the HTTP response's body and the scope as argument and returns true or false.

----

**Example**: Checking that the body contains a specific key.

```
(body, scope) => {
  return body.user_id !== null;
}
```

**Example**: Checking that the body contains a specific value.

```
(body, scope) => {
  return body.user.id === scope.userId;
}
```
