---
title: "Assert Headers"
date: 2017-07-13T00:25:24+02:00
draft: false
---

### Description

Performs an assertion over the last HTTP request's headers.

### Arguments

A javascript function which takes the HTTP response's headers and the scope as argument and returns true or false.

The headers are represented as a map from header key to header value.

----

**Example**: Checking that the server returned a specific header:

```
(headers, scope) => {
  return headers["x-auth-token"] !== null;
}
```

**Example**: Checking that the server returned a header with a specific value:

```
(headers, scope) => {
  return headers["content-type"] === "application/json";
}
```
