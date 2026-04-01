# Contract Sync (User-server)

- Contract Source: https://github.com/jho951/contract
- Service SoT Branch: `main`
- Contract Role: User master and social-link ownership
- Profile visibility/privacy ownership: user-service
- Audit Log: https://github.com/jho951/contract/blob/main/contracts/audit-log/README.md

## Required Links
- User OpenAPI: https://github.com/jho951/contract/blob/main/contracts/openapi/user-service.v1.yaml
- Routing: https://github.com/jho951/contract/blob/main/contracts/routing.md
- Headers: https://github.com/jho951/contract/blob/main/contracts/headers.md
- Security: https://github.com/jho951/contract/blob/main/contracts/security.md
- Visibility: https://github.com/jho951/contract/blob/main/contracts/user/visibility.md

## Sync Checklist
- [ ] `/users/signup`, `/users/me`, `/internal/users/**` paths aligned
- [ ] internal JWT verification (`iss/aud/sub/scope`) enforced
- [ ] trace headers logged and propagated
- [ ] social link ownership behavior aligned with contract
- [ ] profile visibility/privacy policy aligned with contract
- [ ] audit-log event emission matches `contracts/audit-log/service-events.md`
