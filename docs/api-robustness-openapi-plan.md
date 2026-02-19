# Harden API Endpoints + Upgrade OpenAPI (Non-Breaking)

## Summary
Improve API robustness and OpenAPI quality in `pdf-inbound-api` with a non-breaking, consumer-grade contract.
Focus areas:
1. Standardized error handling and validation.
2. Endpoint hardening (input constraints, consistent semantics).
3. OpenAPI completeness (security, schemas, examples, grouped docs).
4. Test coverage for behavior and API contract.

## Scope And Success Criteria
- Keep existing endpoint paths and successful response fields compatible.
- Additive improvements only for payloads (especially errors).
- OpenAPI spec includes:
  - Bearer JWT security scheme.
  - Operation-level request/response/error docs.
  - Reusable error schemas.
  - Correct multipart and binary download documentation.
- Swagger UI and `/v3/api-docs` reachable without auth.
- New tests prove robustness and spec correctness.

## Important Public API / Interface Changes
1. Error payload contract becomes standardized and reusable across failures:
   - Keep `error` field for backward compatibility.
   - Add `message`, `status`, `path`, `timestamp`, `traceId`.
   - Add `fieldErrors` for validation failures.
2. Validation hardening (non-breaking):
   - `id` path vars: positive numeric constraint.
   - `q` query param: trim + max length guard.
   - Multipart upload: explicit consumes/produces and safer file checks.
3. OpenAPI contract additions:
   - `bearerAuth` security scheme.
   - Reusable response schemas for success/error.
   - Explicit operation docs for all auth/document endpoints.

## Implementation Plan

### 1. Standardize Error Handling
- Add API error models in `pdf-inbound-api` (e.g., `ApiErrorResponse`, `ValidationErrorResponse`, `FieldErrorItem`).
- Refactor `GlobalExceptionHandler` to produce one consistent shape for:
  - domain exceptions (`InvalidDocumentException`, `DocumentNotFoundException`, etc.)
  - validation (`MethodArgumentNotValidException`, `ConstraintViolationException`)
  - malformed payloads (`HttpMessageNotReadableException`)
  - missing/invalid params (`MissingServletRequestParameterException`, type mismatch)
  - upload failures (`MaxUploadSizeExceededException`, multipart errors)
  - fallback `Exception`
- Align `JwtAuthenticationEntryPoint` response shape with the same error schema.
- Include request path and trace/correlation id (from MDC if present).

### 2. Harden Controller Inputs And Semantics
- Add `@Validated` to controllers.
- Apply constraints:
  - `@PathVariable Long id` -> `@Positive`
  - `search q` -> optional but bounded (`@Size(max=...)`) and normalized.
- Add explicit method metadata:
  - `consumes = multipart/form-data` on upload endpoints.
  - `produces` where useful for clarity.
- Keep current success payload shapes for compatibility.
- Keep camera upload endpoint non-breaking but clearly documented as limited/stub behavior.

### 3. Improve OpenAPI Configuration
- Replace/minimally refactor `SwaggerConfig` into richer OpenAPI config:
  - `Info` (title/version/description), contact/license.
  - `Components` with JWT bearer scheme.
  - Global security requirement for protected endpoints.
- Add grouped docs:
  - `auth` group (`/api/auth/**`)
  - `documents` group (`/api/documents/**`)
- Define reusable components/schemas for:
  - error responses
  - auth/document DTOs where not inferred cleanly
- Add operation annotations to `AuthController` and enrich existing `DocumentController` annotations:
  - `@Operation` descriptions + tags
  - `@ApiResponses` with 200/204/400/401/403/404/413/500
  - request examples for auth endpoints
  - binary response documentation for download endpoint
- Ensure springdoc endpoints permitted in security:
  - `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`.

### 4. Configuration And Version Hygiene
- Keep Springdoc starter in `pdf-inbound-api`.
- Upgrade springdoc version only if needed for Spring Boot 3.5 compatibility gaps; otherwise keep and document rationale.
- Add/adjust springdoc properties in `application.yml` only if needed (UI path, grouping clarity), without changing existing runtime behavior.

### 5. Testing Plan
Add/extend tests in `pdf-inbound-api`:

1. `GlobalExceptionHandlerTest`
- Assert standardized fields exist (`error`, `status`, `path`, `timestamp`).
- Assert validation failures include `fieldErrors`.
- Assert generic exceptions still hide internals.

2. `DocumentControllerIT`
- Invalid `id` path value returns 400 with standardized error.
- Oversized/invalid multipart checks map to correct status and schema.
- Search input boundary tests (e.g., oversized `q`) return 400.

3. `AuthControllerIT`
- Validation failure responses include standardized structure.
- Unauthorized failures from security layer follow same error shape.

4. OpenAPI contract tests (new IT)
- Fetch `/v3/api-docs` and assert:
  - `components.securitySchemes.bearerAuth` exists.
  - key paths/operations present.
  - error schema component present.
  - download endpoint documents binary response.

## Acceptance Criteria
- All existing endpoint success behaviors remain compatible.
- Error responses are consistent across controller and security failures.
- Swagger UI loads without JWT.
- OpenAPI spec is complete enough for frontend/client generation.
- Tests pass for inbound-api module and new contract checks.

## Assumptions And Defaults Chosen
- Backward compatibility is strict for success responses and endpoint paths.
- Error payload can be extended additively while retaining `error`.
- Camera upload endpoint stays in place; behavior clarified/documented, not redesigned.
- No breaking auth flow changes in this pass.
