---
title: "Eval JS"
date: 2017-07-13T00:25:24+02:00
draft: false
---

### Description

Invokes a javascript function. The result of the invocation will be the new scope.

### Arguments

A javascript function which takes the current scope as argument and returns the new scope.

{{% notice info %}}
If you don't return anything, Apibot will assume that you returned an empty map! So always remember to return the resulting scope.
{{% /notice %}}

----

**Example:** Setting a "random" email in the scope.
```
(scope) => {
  const millis = Date.now();
  scope.email = "myemail" + millis + "@gmail.com";
  return scope;
}
```

**Example:** Removing a key from the scope. Assuming the email key is already defined in the scope.
```
(scope) => {
  delete scope.email;
  return scope;
}
```

**Example:** modifying a key in the scope. Assuming the name and lastName keys are already defined in the scope.
```
(scope) => {
  scope.fullName = scope.name + scope.lastName;
  return scope;
}
```
