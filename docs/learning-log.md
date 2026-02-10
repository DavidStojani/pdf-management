## 2026-02-09
- [Testing] `@WebMvcTest` can still pick up unintended beans when the main app has a wide `@ComponentScan`, leading to `NoUniqueBeanDefinitionException` in test slices.
- [Testing] Missing explicit parameter names (e.g., `@RequestParam("token")`) can break request binding when compiler isn’t using `-parameters`.
- [Testing] Add `GlobalExceptionHandler` to test slice config when controller tests expect error HTTP responses for thrown exceptions.

- Pitfalls:
  - Bean collisions in test slices due to broad component scanning.
  - Mockito inline mock maker self-attach warnings and context failure threshold hiding root causes.

- Patterns to reuse:
  - Use `@ContextConfiguration` with explicit controller + advice + test stubs to keep test slice minimal.

- TODO to practice:
  - Standardize Spring profile usage in tests (`@ActiveProfiles("test")` vs `@TestPropertySource`).
## 2026-02-10
- [Docker] For fast dev, run a prebuilt JAR in `docker-compose-dev.yml` instead of Maven-in-container.
- [Spring] CORS must explicitly allow frontend origins when UI runs on a different port.
- [Auth] Frontend must store `jwtToken` (not `token`) to avoid invalid JWT parsing.
- [Web] Iframe previews can’t send auth headers; either allow token query params or use blob fetch with headers.

- Pitfalls:
  - JWT in URL + `X-Frame-Options` disabled is convenient but weakens security.
  - Frontend calling protected endpoints before login causes 401s.

- Patterns to reuse:
  - Add minimal stub endpoints to avoid frontend breakage while backend features are built.

- TODO to practice:
  - Implement favourites with a proper persistence model and API.
