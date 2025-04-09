# Router Overview

The router is created using the `vue-router` library and handles client-side routing.

## Key Concepts

### Redirects

Use the `beforeEnter` hook to redirect users, e.g. logging out and redirecting to the home route.

### Route Protection

Use the `beforeEach` hook to protect routes that require authentication add `meta: { requiresAuth: true }` in route. If
a user is not logged in, redirect them to the home route.

### Route Order

<span style="color: red;">The order of routes is important. The router will match the first route that matches the URL.
The catch-all route `/:catchAll(.*)*` should always be last.</span>