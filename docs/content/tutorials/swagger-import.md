---
title: "Swagger Import"
date: 2017-07-12T23:08:38+02:00
draft: false
---


This short tutorial will show you how to import your swagger definitions in Apibot. This assumes
basic knowledge of how to create and run an Apibot graph. If you do not know what this means, please
check our [getting started tutorial](../getting-started")

### What is Swagger?

The OpenAPI Specification, originally known as the Swagger Specification, is a 
specification for describing,  and visualizing REST APIs. In essence it is a standard format for 
describing API.

Click [here](https://en.wikipedia.org/wiki/OpenAPI_Specification) to learn more about Swagger/OpenAPI.

### Importing a Single Swagger Endpoint

To import a swagger endpoint you will need to get a swagger JSON definition of your
API.

![swagger json](swagger-json.png)

This file is usually available in Swagger UI as a url ending in `.json` (see image above). 

1. Copy the JSON contents into your clipboard.
1. Go to Apibot and create a new HTTP node inside an existing graph.
1. Click on the green Import from Swagger Button
    ![swagger button](swagger-button.png)
1. Paste the Swagger JSON and click on `Import`.
1. Find and select the endpoint to import.
1. If you look at your HTTP node's properties you will see that many fields have been pre-filled.

### Done!

You have just imported an endpoint from a swagger definition in Apibot. Bear in mind that you will
probably need to tweak your recently imported HTTP node by e.g. setting values for query params.
