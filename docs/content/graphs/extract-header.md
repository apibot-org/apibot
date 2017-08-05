---
title: "Extract Header"
date: 2017-07-13T00:25:24+02:00
draft: false
---

### Description

Takes a header last HTTP request and places it in the scope.

### Arguments

**Property Name**: The name that will be given to the extracted header inside the scope.

**Header Name**: The name of the http header.

----

**Example**: extracting the APIs version assuming an API that returns versions as headers.

![](ex1.png)

Will result in the following scope:
```
{
  /* ... */
  version: "<some API version>"
}
```
