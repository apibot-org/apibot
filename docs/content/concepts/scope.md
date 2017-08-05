---
title: "Scope"
date: 2017-07-12T22:58:01+02:00
draft: false
---

The Scope is Apibots primary and only data-structure. It is defined as a map strings to anything. You can generally think of the Scope as a Javascript object. Here are a few examples of valid scopes.

```
// ✅ a valid scope
{}          

// ✅ a valid scope  
{ username: "bob" }

// ✅ valid scope
{ foo: { bar: [1,2,3] }}

// ❌ an invalid scope
null

// ❌ an invalid scope
1

// ❌ an invalid scope
[ { foo: 1 } ]
```

Every [node](../nodes) takes the scope as input and outputs a new scope. You will see [many places](/docs/graphs/evaljs) in the documentation with little code snippets like this:

```
(scope) => {
  /* some transformation over the scope */
  scope.someTransformationOverTheScope = {foo: "bar"};
  return scope;
}
```

This is a javascript function which takes the scope as argument and returns the modified scope.
