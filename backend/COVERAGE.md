# Test & Coverage policy

We enforce a JaCoCo **instruction coverage** gate (minimum **0.75**) at the bundle level to keep test coverage focused on business logic.

To make the gate actionable and avoid penalizing low-value or hard-to-test code, the following patterns are excluded from coverage checks:

- `com/ahy/payment/gateway/impl/**` (gateway implementations such as Stripe/Mock adapters; typically integration-heavy)
- `com/ahy/payment/exception/**` (small exception POJOs and mapping classes)

**Rationale:** these exclusions keep the coverage threshold meaningful for core business logic while allowing integration-level testing or targeted tests to cover gateway implementations if desired.

If you want to change the policy (lower the gate, remove exclusions, or scope coverage differently), open a PR and include a brief justification and test plan.

Quick verification (locally):

```bash
# run unit & integration tests + coverage check
./mvnw verify
```
