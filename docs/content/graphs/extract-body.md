---
title: "Extract Body"
date: 2017-07-13T00:25:24+02:00
draft: false
---

### Description

Takes a value in the body of the last HTTP request and places it in the scope.

### Arguments

**Property Name**: The name that will be given to the extracted contents inside the scope.

**Function**: A javascript function which takes the body as argument and returns anything.

----

**Example**: extracting an authentication token from the body. If the name is "token" and the functions is set as follows:
```
(body) => {
  return body.access_token;
}
```
Then the resulting scope will look like:
```
{
  /* ... */
  token: "<some access token>"
}
```
