# Router Overview

The router is created using the `vue-router` library and handles client-side routing.

## Key Concepts

### Redirects

Use the `beforeEnter` hook to redirect users, e.g. logging out and redirecting to the home route.

#### Post-Login Redirects

There are three places where post-login redirects are configured:

1. **OAuthRedirect Component** (`components/OAuthRedirect.vue`):
   ```javascript
   // When OAuth login completes:
   if (authStore.isLoggedIn) {
     await router.push({ name: 'home' }); // Change destination here
   }
   ```

2. **Redirect Route** (in `router/index.js`):
   ```javascript
   {
     path: '/redirect',
     name: 'redirect',
     beforeEnter: async (to, from, next) => {
       // ...
       if (authStore.isLoggedIn) {
         next({ name: 'home' }); // Change destination here
       }
     }
   }
   ```

3. **Router Guard** (in `router/index.js`):
   ```javascript
   router.beforeEach(async (to, from, next) => {
     // ...
     if (to.name === 'login' && authStore.isLoggedIn) {
       next({ name: 'home' }); // Change destination here
     }
   });
   ```

### Route Protection

Use the `beforeEach` hook to protect routes that require authentication by adding `meta: { requiresAuth: true }` to a route. If a user is not logged in, they will be redirected to the login page.

```javascript
{
  path: '/user-profile',
  name: 'user-profile',
  component: () => import('../features/user/UserProfileView.vue'),
  meta: { requiresAuth: true } // This route requires authentication
}
```

The router guard will automatically check authentication:

```javascript
router.beforeEach(async (to, from, next) => {
  // Initialize auth if needed
  if (!authStore.authInitialized) {
    await authStore.checkAuth();
  }

  // Check if route requires authentication
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'login' });
  } else {
    next();
  }
});
```

### Error Handling

The router includes error handling to prevent navigation from breaking if authentication checks fail:

```javascript
try {
  // Authentication logic
} catch (error) {
  console.error('Error in router guard:', error);
  // Continue navigation even if auth check fails
  next();
}
```

### Route Order

<span style="color: red;">The order of routes is important. The router will match the first route that matches the URL.
The catch-all route `/:catchAll(.*)*` should always be last.</span>

### Authentication Flow

1. **Initial page load**: The `checkAuth()` method in the auth store determines if user is authenticated
2. **OAuth login**: User is redirected to OAuth provider, then back to the `oauth-redirect` route
3. **Logout**: User is logged out and redirected to the home page
4. **Protected routes**: Routes with `meta: { requiresAuth: true }` check authentication before allowing access

### Route Navigation Guards

- `beforeEach`: Runs before any navigation
- `beforeEnter`: Runs before entering a specific route
- `beforeRouteEnter`: Component-level guard that runs before the component is created