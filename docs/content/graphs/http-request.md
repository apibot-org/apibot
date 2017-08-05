---
title: "Http Request"
date: 2017-07-13T00:25:17+02:00
draft: false
---

#### Description

The "Http Request node" makes an HTTP request and appends both the Http Request and Http Response to the scope. If an Http Request/Response was already in the scope, it will be overriden.

#### Arguments

* **Url**: The HTTP request's URL. Example: `https://swapi.co/api/people/1`. This value is templateable e.g. you can use `${root}/api/people/1` if the variable root is defined in the scope.
* **Method**: The HTTP method, e.g. `GET`
* **Body**: The request's body, also sometimes called payload. This value is templateable e.g. assuming your request is sending a JSON body you could send

```
{
  "user_id":"${id}"
}
```

* **Headers**: The request headers. Each row will be a request key/value pair. Both the key and value are templateable e.g. you could have a header with key `authorization` and value `Basic ${basicAuth}`.

#### Templating

Every input element in an HTTP request is templateable meaning that you can use the `${variable}` syntax to access previously defined elements in the scope. To give an example, you may set the url to `${root}/api/${version}/users/1`. When the request is executed the url will be replaced with whatever is in the scope with the key `root` and `version` (or fail if the keys are not found).

To give another example, assume that the API is protected with [basic authentication](https://en.wikipedia.org/wiki/Basic_access_authentication), and  what you can do is send a header with the value `Basic ${basicAuth}`.

You can also use the `${value.nested}` syntax to access nested elements.
