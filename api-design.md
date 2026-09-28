# E-Commerce API - Overall Structure

## Base Path

All endpoints are under `/api/v1`. There is no separate `/api/v1/admin/...` prefix in
this codebase — admin-only actions live at the same path as their public counterpart
(e.g. `POST /api/v1/categories`) and are gated by role instead.

All customer endpoints that require authentication use the current user from the
security context (`/me`). Requests are authenticated with a JWT access token sent as
`Authorization: Bearer <token>`.

**Auth column values below:**
- `Public` — no token required
- `Authenticated` — any signed-in user, any role
- `ADMIN` — signed-in user with the `ADMIN` role

**Envelope:** every endpoint except the `/api/v1/product` group returns
```json
{ "success": true, "message": "...", "data": { /* payload below */ }, "timestamp": "..." }
```
`GET /api/v1/product` and `GET/POST/PUT/DELETE /api/v1/product/**` return the raw
payload with **no** envelope — see the note in section 9.

Field tables below list only what a request accepts / a response returns; required
fields are marked, everything else is optional.

---

## 1. Authentication (`/api/v1/auth`)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST   | `/auth/register` | Public | Register a new user (assigned the `CUSTOMER` role) |
| POST   | `/auth/login` | Public | Login with email/password → access + refresh token, registers/refreshes the calling device |
| POST   | `/auth/refresh` | Public | Exchange a valid refresh token for a new access/refresh token pair |
| POST   | `/auth/logout` | Public | Revoke the session identified by this refresh token (current device only) |
| POST   | `/auth/logout-all` | Authenticated | Revoke every active session for the authenticated user, across all devices |
| GET    | `/auth/devices` | Authenticated | List the authenticated user's known devices and their session status |
| DELETE | `/auth/devices/{deviceId}` | Authenticated | Revoke a single device's session(s) |

### Request bodies

**`RegisterRequest`** (`POST /auth/register`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | string | ✓ | must match `[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,3}` |
| `firstName` | string | ✓ | |
| `lastName` | string | ✓ | |
| `password` | string | ✓ | min 8 chars |
| `confirmPassword` | string | ✓ | must equal `password` |

**`LoginRequest`** (`POST /auth/login`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | string | ✓ | |
| `password` | string | ✓ | |
| `deviceId` | string | ✓ | client-generated stable id for this device |
| `deviceName` | string | | e.g. `"John's iPhone"` |
| `deviceType` | string | | e.g. `"MOBILE"`, `"WEB"` |

**`RefreshTokenRequest`** (`POST /auth/refresh`, `POST /auth/logout`)

| Field | Type | Required |
|---|---|---|
| `refreshToken` | string | ✓ |

`POST /auth/logout-all` and `GET/DELETE /auth/devices/**` have no request body — the
target user/device comes from the access token / path.

### Response bodies

**`AuthResponse`** (`login`, `refresh`) — wrapped in `data`

| Field | Type |
|---|---|
| `accessToken` | string |
| `refreshToken` | string |
| `tokenType` | string, always `"Bearer"` |
| `expiresInSeconds` | number |

**`register`** response `data` is a `UserResponse` — see section 2.

**`DeviceResponse`** (`GET /auth/devices` → `data: DeviceResponse[]`)

| Field | Type | Notes |
|---|---|---|
| `id` | number | |
| `deviceId` | string | |
| `deviceName` | string | |
| `deviceType` | string | |
| `lastSeenAt` | ISO instant | |
| `createdAt` | ISO instant | |
| `current` | boolean | true if this is the device the caller is authenticated from |
| `active` | boolean | true if it has at least one non-revoked, non-expired refresh token |

### Notes

- Sessions are tracked per device in `user_device` / `refresh_token` (rotated refresh tokens, revocation, reuse detection).
- `forgot-password` / `reset-password` are **not implemented yet** (`password_reset_tokens` entity exists but has no service/controller).

---

## 2. Current User (`/api/v1/users/me`)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| GET    | `/users/me` | Authenticated | Get the authenticated user's profile |
| PATCH  | `/users/me` | Authenticated | Update name, phone, profile image |
| PATCH  | `/users/me/password` | Authenticated | Change password |
| DELETE | `/users/me` | Authenticated | Soft-deactivate account (`active = false`) |

### Addresses (nested under user)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| GET    | `/users/me/addresses` | Authenticated | List all addresses |
| POST   | `/users/me/addresses` | Authenticated | Add address (first address for a user is auto-set as default) |
| GET    | `/users/me/addresses/{id}` | Authenticated | Get one address |
| PATCH  | `/users/me/addresses/{id}` | Authenticated | Update address |
| DELETE | `/users/me/addresses/{id}` | Authenticated | Delete address |
| PATCH  | `/users/me/addresses/{id}/default` | Authenticated | Set as default (clears the default flag on all others) |

### Request bodies

**`UpdateProfileRequest`** (`PATCH /users/me`) — all fields optional, only non-null ones are applied

| Field | Type | Notes |
|---|---|---|
| `firstName` | string | 3-50 chars if present |
| `lastName` | string | 3-50 chars if present |
| `phone` | string | max 30 chars |
| `profileImage` | string | URL |

**`UpdatePasswordRequest`** (`PATCH /users/me/password`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `oldPassword` | string | ✓ | must match current password |
| `newPassword` | string | ✓ | min 8 chars |
| `confirmNewPassword` | string | ✓ | must equal `newPassword` |

`DELETE /users/me` has no request body.

**`AddressRequest`** (`POST` / `PATCH` on `/users/me/addresses/**`) — on `PATCH`, all fields optional/partial

| Field | Type | Notes |
|---|---|---|
| `street` | string | |
| `city` | string | |
| `state` | string | |
| `zipCode` | string | |
| `country` | string | |
| `isDefault` | boolean | if true, clears the default flag on the user's other addresses |

`PATCH /users/me/addresses/{id}/default` has no request body.

### Response bodies

**`UserResponse`** (`GET`/`PATCH /users/me`, `data`)

| Field | Type | Notes |
|---|---|---|
| `id` | number | |
| `name` | string | `firstName + " " + lastName` |
| `email` | string | |
| `firstName` | string | |
| `lastName` | string | |
| `phone` | string | |
| `profileImage` | string | |
| `active` | boolean | |
| `addresses` | `AddressResponse[]` | |
| `role` | `RoleResponse[]` | see section 4 for shape |
| `createdAt` | ISO datetime | |
| `updatedAt` | ISO datetime | |

**`AddressResponse`** (`data` on all `/users/me/addresses/**` endpoints)

| Field | Type |
|---|---|
| `id` | number |
| `street` | string |
| `city` | string |
| `state` | string |
| `zipCode` | string |
| `country` | string |
| `isDefault` | boolean |

**Sample `GET /users/me` response:**

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": 101,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1 555-123-4567",
    "profileImage": "https://example.com/avatars/john.jpg",
    "active": true,
    "addresses": [
      { "id": 1, "street": "123 Main St", "city": "New York", "state": "NY", "zipCode": "10001", "country": "USA", "isDefault": true }
    ],
    "role": [
      { "roleId": 7, "roleName": "CUSTOMER", "active": true, "description": "Regular shopper", "permissions": [] }
    ],
    "createdAt": "2026-08-28T10:15:30",
    "updatedAt": "2026-08-28T12:05:10"
  }
}
```

---

## 3. Users - Admin (`/api/v1/users`)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST   | `/users` | ADMIN | Create a user directly (bypasses `/auth/register`) |
| PUT    | `/users/{id}` | ADMIN | Full update of a user by id |
| PATCH  | `/users/{id}/password` | ADMIN | Change a user's password by id |
| GET    | `/users/{id}` | ADMIN | Get a user by id |
| GET    | `/users/email/{email}` | ADMIN | Get a user by email |
| DELETE | `/users/{id}` | ADMIN | Hard-delete a user by id |

### Request bodies

**`UserRequest`** (`POST /users`, `PUT /users/{id}`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | string | | must be a valid email if present; **not required**, unlike `RegisterRequest` |
| `firstName` | string | ✓ | 3-50 chars |
| `lastName` | string | ✓ | 3-50 chars |
| `password` | string | ✓ | min 8 chars |
| `confirmPassword` | string | ✓ | must equal `password` |
| `profilePicture` | string | | maps to `profileImage` |
| `address` | `AddressRequest` | | only applied on **create**; ignored on `PUT` update |
| `roles` | `number[]` | | role ids — **accepted but currently ignored**: `UserMapper` never assigns roles from this field, so a user created via this endpoint has no roles until assigned separately |

`PUT` reuses the same `UserRequest` but every field is treated as an optional partial
update (only non-null values are applied), and `password`/`confirmPassword` are still
required by `@NotBlank` even though they're not used to change the password on update.

**`UpdatePasswordRequest`** (`PATCH /users/{id}/password`) — same shape as section 2.

### Response bodies

`UserResponse` for all endpoints (same shape as section 2). `GET /users/email/{email}`
returns the same shape, looked up by email instead of id.

---

## 4. Roles (`/api/v1/role`)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST   | `/role` | ADMIN | Create a new role |
| PUT    | `/role/{id}` | ADMIN | Update an existing role |
| GET    | `/role/{id}` | ADMIN | Get a role by id (includes its permissions) |
| GET    | `/role` | ADMIN | List all roles |
| DELETE | `/role/{id}` | ADMIN | Delete a role (blocked if still assigned to users) |
| PUT    | `/role/{id}/permissions` | ADMIN | Replace the full set of permissions assigned to a role |

### Request bodies

**`RoleRequest`** (`POST /role`, `PUT /role/{id}`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `roleName` | string | ✓ | unique |
| `active` | boolean | | ignored on create (always set `true`); not currently applied on update either — only `roleName`/`description` are mapped |
| `description` | string | | 2-100 chars if present |

**`AssignPermissionsRequest`** (`PUT /role/{id}/permissions`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `permissionIds` | `number[]` | ✓ | full replacement set — any permission ids not in this list are unassigned; unknown ids reject the whole request |

### Response bodies

**`RoleResponse`**

| Field | Type |
|---|---|
| `roleId` | number |
| `roleName` | string |
| `active` | boolean |
| `description` | string |
| `permissions` | `PermissionResponse[]` |

---

## 5. Permissions (`/api/v1/permissions`)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST   | `/permissions` | ADMIN | Create a new permission |
| PUT    | `/permissions/{id}` | ADMIN | Update an existing permission |
| GET    | `/permissions/{id}` | ADMIN | Get a permission by id |
| GET    | `/permissions` | ADMIN | List all permissions |
| DELETE | `/permissions/{id}` | ADMIN | Delete a permission (blocked if still assigned to roles) |

### Request bodies

**`PermissionRequest`** (`POST /permissions`, `PUT /permissions/{id}`) — on `PUT`, all fields are optional/partial

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | ✓ | unique, max 100 chars, e.g. `"PRODUCT_CREATE"` |
| `resource` | string | ✓ | max 50 chars, e.g. `"PRODUCT"` |
| `action` | string | ✓ | max 50 chars, e.g. `"CREATE"` |
| `description` | string | | max 255 chars |

### Response bodies

**`PermissionResponse`**

| Field | Type |
|---|---|
| `id` | number |
| `name` | string |
| `resource` | string |
| `action` | string |
| `description` | string |
| `createdAt` | ISO datetime |
| `updatedAt` | ISO datetime |

---

## 6. Categories (`/api/v1/categories`)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| GET    | `/categories` | Public | List all categories |
| GET    | `/categories/{id}` | Public | Get a category by id |
| GET    | `/categories/{id}/detail` | Public | Get a category by id with nested sub-categories (+ each sub-category's products) |
| POST   | `/categories` | ADMIN | Create a category |
| PUT    | `/categories/{id}` | ADMIN | Update a category |
| DELETE | `/categories/{id}` | ADMIN | Delete a category (cascades to its sub-categories) |

### Request body

**`CategoryRequest`** (`POST`, `PUT`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | ✓ | max 255 chars |
| `slug` | string | ✓ | max 255 chars, unique |
| `image` | string | | max 500 chars, URL |

### Response bodies

**`CategoryResponse`** (`GET /categories`, `GET /categories/{id}`, create/update)

| Field | Type |
|---|---|
| `id` | number |
| `name` | string |
| `slug` | string |
| `image` | string |
| `createdAt` | ISO datetime |
| `updatedAt` | ISO datetime |

**`CategoryDetailResponse`** (`GET /categories/{id}/detail`)

| Field | Type |
|---|---|
| `id` | number |
| `name` | string |
| `slug` | string |
| `image` | string |
| `subCategories` | `SubCategoryDetailResponse[]` |
| `createdAt` | ISO datetime |
| `updatedAt` | ISO datetime |

**`SubCategoryDetailResponse`** (nested in the above)

| Field | Type |
|---|---|
| `subCategory` | `SubCategoryResponse` (see section 7) |
| `products` | `ProductResponse[]` (see section 9) |

## 7. Sub-Categories (`/api/v1/sub-categories`)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| GET    | `/sub-categories` | Public | List all sub-categories (optional `?categoryId=` filter) |
| GET    | `/sub-categories/{id}` | Public | Get a sub-category by id |
| POST   | `/sub-categories` | ADMIN | Create a sub-category under a category |
| PUT    | `/sub-categories/{id}` | ADMIN | Update a sub-category |
| DELETE | `/sub-categories/{id}` | ADMIN | Delete a sub-category |

### Request body

**`SubCategoryRequest`** (`POST`, `PUT`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | ✓ | max 255 chars |
| `slug` | string | ✓ | max 255 chars, unique |
| `categoryId` | number | ✓ | parent category id |

### Response body

**`SubCategoryResponse`**

| Field | Type |
|---|---|
| `id` | number |
| `name` | string |
| `slug` | string |
| `categoryId` | number |
| `categoryName` | string |
| `createdAt` | ISO datetime |
| `updatedAt` | ISO datetime |

## 8. Brands (`/api/v1/brands`)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| GET    | `/brands` | Authenticated | List all brands |
| GET    | `/brands/{id}` | Authenticated | Get a brand by id |
| POST   | `/brands` | Authenticated | Create a brand |
| PUT    | `/brands/{id}` | Authenticated | Update a brand |
| DELETE | `/brands/{id}` | Authenticated | Delete a brand |

> ⚠️ No role restriction on write operations yet (any authenticated user can create/update/delete a brand) — `BrandController` has no `@PreAuthorize`, unlike `CategoryController`.

### Request body

**`BrandRequest`** (`POST`, `PUT`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | ✓ | max 255 chars |
| `slug` | string | ✓ | max 255 chars |
| `image` | string | | max 500 chars, URL |

### Response body

**`BrandResponse`**

| Field | Type |
|---|---|
| `id` | number |
| `name` | string |
| `slug` | string |
| `image` | string |
| `createdAt` | ISO datetime |
| `updatedAt` | ISO datetime |

## 9. Products (`/api/v1/product`)

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| GET    | `/product` | Authenticated | Paginated product list; filters: `title`, `brandId`, `subCategoryId`, `minPrice`, `maxPrice`, `minRating` |
| GET    | `/product/{id}` | Authenticated | Get a product with brand, sub-category, variants, and images |
| POST   | `/product` | Authenticated | Create a product with its variants and images |
| PUT    | `/product/{id}` | Authenticated | Update a product, its variants, and associations |
| DELETE | `/product/{id}` | Authenticated | Delete a product (cascades to variants and images) |

> Note: base path is singular (`/product`, not `/products`), and responses are the raw
> DTO/`Page` (not wrapped in the `ApiResponse` envelope used by every other module) —
> both are existing inconsistencies, not typos in this doc.
>
> ⚠️ Same as brands: no role restriction on write operations yet.

### Request body

**`ProductRequest`** (`POST`, `PUT`)

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | string | ✓ | max 255 chars |
| `slug` | string | ✓ | max 255 chars, unique |
| `description` | string | | |
| `imageCover` | string | | max 500 chars, URL |
| `subCategoryId` | number | ✓ | |
| `brandId` | number | | |
| `variants` | `ProductVariantsRequest[]` | ✓ | at least one required |
| `images` | `ProductImageRequest[]` | | |

**`ProductVariantsRequest`** (nested)

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | number | | present on update to match an existing variant |
| `color` | string | | max 50 chars |
| `sizeId` | number | | FK to a `ProductSize` |
| `sku` | string | ✓ | max 50 chars, unique |
| `price` | number | ✓ | must be > 0 |
| `priceAfterDiscount` | number | | must be ≥ 0 |
| `soldQuantity` | number | | default `0`, must be ≥ 0 |

**`ProductImageRequest`** (nested)

| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | number | | present on update to match an existing image |
| `imageUrl` | string | ✓ | max 500 chars, unique across all products |
| `sortOrder` | number | | |

### Response bodies

**`ProductResponse`** (`GET /product`, `GET /product/{id}`, create/update)

| Field | Type |
|---|---|
| `id` | number |
| `title` | string |
| `slug` | string |
| `description` | string |
| `imageCover` | string |
| `ratingsAverage` | number |
| `ratingsQuantity` | number |
| `subCategory` | `{ subCategoryId, subCategoryName }` — a lightweight summary, **not** the full `SubCategoryResponse` from section 7 |
| `brand` | `{ brandId, brandName }` — a lightweight summary, **not** the full `BrandResponse` from section 8 |
| `variants` | `ProductVariantResponse[]` |
| `images` | `ProductImageResponse[]` |
| `createdAt` | ISO datetime |
| `updatedAt` | ISO datetime |

**`ProductVariantResponse`**

| Field | Type |
|---|---|
| `id` | number |
| `color` | string |
| `size` | `{ id, name }` |
| `sku` | string |
| `price` | number |
| `priceAfterDiscount` | number |
| `soldQuantity` | number |

**`ProductImageResponse`**

| Field | Type |
|---|---|
| `id` | number |
| `imageUrl` | string |
| `sortOrder` | number |

`GET /product` wraps `ProductResponse[]` in a standard Spring `Page` object
(`content`, `totalElements`, `totalPages`, `number`, `size`, ...).

---

## Not Yet Implemented

These modules only have a bare JPA `entity` today — no `repository`, `service`, or
`controller` — so they have no API surface at all:

- **Cart** (`Cart`, `CartItem`) — additionally, `orders` is missing its own `user_id`
  column, so cart→order checkout can't be wired up without a schema fix first.
- **Order** (`Order`, `OrderItem`, `OrderCoupon`)
- **Coupon** (`Coupon` entity + table exist; 5 rows seeded via `DataSeeder`, but no API)
- **Review** (`Review` entity + table exist; 5 rows seeded via `DataSeeder`, but no API)
- **Wishlist** — no entity, no table at all
