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
