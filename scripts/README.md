### smoke-dev.sh

`smoke-dev.sh` is a bash script that runs a simple smoke test against the create-and-vary-a-licence-api in the dev
environment.

It checks:

1. `/info` endpoint is present (HTTP 200)
2. `/health` endpoint is present and healthy (`status: UP`)
3. Swagger UI is available (HTTP 200)
4. The SAR (`/subject-access-request`) endpoint is secured — an unauthenticated request is rejected with HTTP 401
5. A SAR request with valid credentials succeeds (HTTP 200 or 204)

Dependencies: `jq`, `kubectl` (with access to the `create-and-vary-a-licence-dev` namespace).

Run directly from the `scripts/` directory:

```bash
./smoke-dev.sh
```
