# Instant Catalog Example

This package contains the Instant catalog examples used to open and test documents
from the Nutrient Document Engine.

It is intended to be used alongside the official server example apps, which provide
the `/api/documents` and `/api/document/:id` endpoints that supply JWTs:

- Node.js server example: https://github.com/PSPDFKit/pspdfkit-server-example-nodejs
- Rails server example: https://github.com/PSPDFKit/pspdfkit-server-example-rails

Use the server examples to create documents, list available documents, and retrieve
JWTs for authenticated access from the Android catalog.

When testing on an Android emulator, use `http://10.0.2.2` instead of `localhost`
to reach your host machine.
