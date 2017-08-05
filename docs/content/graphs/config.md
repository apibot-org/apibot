---
title: "Config"
date: 2017-07-13T00:25:24+02:00
draft: false
---

### Description

Sets text variables in the scope.

### Arguments

A list of key-value pairs. The config node will set each key-value pair into the scope.

----

**Example:** Setting your initial configuration variables.

The following configuration

![](config-vars.png)

Will result in the following scope
```
{
  url: "https://myapi.co/api/3",
  username: "billybob"
}
```
