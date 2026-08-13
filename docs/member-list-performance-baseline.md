# Member list query performance baseline

## Scope and test conditions

This report records the pre-refactor behavior of `GET /member/member/list`.  It
was captured on 2026-08-12 against the locally restored `brushing_test` schema,
using MySQL 8.0.42.  No data or schema changes were made while collecting it.

The request represented by the measurements was the worst count shape used by
an unrestricted administrator:

- `pageNum=1`
- `pageSize=10`
- no username, invitation code, phone, real/fake, level or date filter
- no agent data-scope predicate
- fixed `ORDER BY u.create_time DESC`

Exact test-table row counts were:

| Table | Rows |
| --- | ---: |
| `order_member_user` | 2,355 |
| `order_topup` | 5,391 |
| `order_withdrawal` | 1,683 |
| `order_member_level` | 5 |

The restored test schema did **not** yet contain the three indexes already
reported as present in production:

- `order_topup.idx_topup_user_status(user_id, status)`
- `order_withdrawal.idx_withdrawal_user_status(user_id, status)`
- `order_withdrawal.idx_withdrawal_user_application_time(user_id, application_time)`

It did contain `order_member_user.idx_parent_id(parent_id)`.  Therefore the
baseline below is deliberately conservative for financial aggregation and also
demonstrates why query shape, not only indexes, must be fixed.

## Legacy count result

PageHelper 6.1.0 wraps the complete `selectOrderMemberUserList` projection in a
count subquery.  The exact current projection was reproduced as:

```sql
EXPLAIN ANALYZE
SELECT COUNT(0)
FROM (
    SELECT /* every list column and correlated scalar subquery */
    FROM order_member_user u
    LEFT JOIN order_member_level level_info ON u.level_id = level_info.id
    LEFT JOIN order_member_user parent_user ON u.parent_id = parent_user.id
) tmp_count;
```

The complete executable SQL is in
[`scripts/member_list_performance_baseline.sql`](../scripts/member_list_performance_baseline.sql).

Measured total execution time was **10,991 ms**.  Important nodes from
`EXPLAIN ANALYZE` were:

| Plan node | Actual behavior |
| --- | --- |
| Materialize `tmp_count` | 2,355 rows, 10,990 ms |
| Base user/level/parent joins | 2,355 rows, 23.6 ms |
| Pending-withdrawal sum | full scan of 1,683 rows, 2,355 loops, 0.297 ms/loop |
| Today-withdrawal count | full scan of 1,683 rows, 2,355 loops, 0.264 ms/loop |
| Direct-subordinate count | `idx_parent_id` lookup, 2,355 loops, 0.006 ms/loop |
| All-subordinate count | full scan of 2,355 users, 2,355 loops, 1.02 ms/loop |
| Recharge sum, first copy | full scan of 5,391 rows, 2,355 loops, 1.18 ms/loop |
| Recharge sum, duplicate for `diff_amount` | full scan of 5,391 rows, 2,355 loops, 1.16 ms/loop |
| Successful-withdrawal sum, first copy | full scan of 1,683 rows, 2,355 loops, 0.318 ms/loop |
| Successful-withdrawal sum, duplicate for `diff_amount` | full scan of 1,683 rows, 2,355 loops, 0.318 ms/loop |

The base joins account for only about 0.2% of the count time.  Almost all time
is spent evaluating projection-only correlated subqueries that cannot affect
the number of matching members.

The first ten-row page also repeats the same scans ten times.  In its plan, one
recharge expression cost about 1.15 ms per returned member, all-subordinate
count about 0.99 ms per member, and each withdrawal expression about
0.26-0.32 ms per member.  The base page and sort itself was about 1.3 ms.

## Candidate query-shape measurements

The read-only prototype separates count, base page, financial aggregation and
descendant aggregation.  On the same warm local database:

| Operation | Actual time |
| --- | ---: |
| Lightweight `COUNT(*)` over filtered `order_member_user` | 0.221 ms |
| Base page with level and parent joins, 10 rows | 1.34 ms |
| One-pass page financial aggregation | 2.93 ms |
| One-pass page descendant aggregation preserving CSV semantics | 6.38 ms |

The lightweight count alone was roughly **49,700 times faster** than the legacy
materialized count on this dataset.  These are database-operator timings, not
an HTTP latency promise; network, MyBatis mapping and JVM work are excluded.
The production dataset is also larger.  The result nevertheless establishes
that the count projection is the dominant structural defect.

The financial comparison query checked all 2,355 users and returned
`financial_mismatch_users = 0` for all of these legacy fields:

- `totalRecharge`
- `totalWithdraw`
- `withdrawFrozenAmount`
- `todayWithdrawCount`

The parsed-ancestors batch query likewise returned
`descendant_count_mismatch_users = 0` against the old per-user `FIND_IN_SET`
formula.

## Implemented-query verification

After the mapper/service refactor, the exact unrestricted first-page query and
the two exact ten-ID aggregate statements were run again on the same restored
database.  A warm `EXPLAIN ANALYZE` run reported:

| Implemented statement | Actual time |
| --- | ---: |
| Explicit `selectOrderMemberUserList_COUNT` | 0.16 ms |
| Lightweight list projection and sort, 10 rows | 2.14 ms |
| Financial batch for those 10 IDs | 2.36 ms |
| Direct plus escaped materialized-path descendant batch for those 10 IDs | 6.97 ms |

The four database stages total about **11.6 ms** on this dataset, compared with
**10,991 ms for the legacy count alone**.  The exact count statement was about
68,700 times faster in this run.  The restored schema still lacked the three
production indexes, so the financial plan deliberately shows one scan of each
transaction table; it no longer shows a scan per returned member.

An old-versus-new comparison for the returned ten rows produced zero
mismatches for every derived list field: recharge total, successful-withdrawal
total, pending-withdrawal amount, today's withdrawal count, direct count, all
descendant count, next-level recharge amount and recharge-minus-withdrawal
difference.  The legacy wrapped source and explicit count both returned
`2,355`.  The full-dataset comparisons above additionally cover every member's
financial and materialized-path descendant values.

The implemented path parser applies `JSON_QUOTE` before inserting JSON array
separators.  This keeps the query valid if a legacy path unexpectedly contains
a quote or backslash.  Read-only MySQL checks with both characters parsed the
original tokens correctly; all 2,355 restored rows generated valid JSON and
the full-dataset descendant mismatch count remained zero.

The mapper test invokes the real PageHelper 6.1.0 interceptor with a capturing
MyBatis executor.  It observed execution of the mapped statement ID
`com.brushing.member.mapper.OrderMemberUserMapper.selectOrderMemberUserList_COUNT`
before the paged statement, proving that PageHelper does not synthesize a count
from the list projection.  The targeted backend test run was:

```powershell
mvn -pl brushing-admin -am `
  "-Dtest=OrderMemberUserMapperSqlTest,OrderMemberUserListServiceTest,OrderMemberUserListSortContractTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: **23 tests, 0 failures, 0 errors, BUILD SUCCESS**.  These tests also
assert the common filter fragment, single-scan aggregate shape, preserved
Page metadata, zero defaults, empty-page short circuit and 500-ID export
batches.

## Sorting compatibility verification

The member-list endpoint now treats sorting as an explicit contract rather
than passing request text to PageHelper.  Camel-case and snake-case public
fields map to fixed SQL expressions, and direction is restricted to
`asc`/`desc` (including the existing `ascending`/`descending` UI spellings).
Unknown fields, punctuation, table-qualified names, multi-column input and
composable direction values return a 400 business error.  The default remains
`u.create_time DESC`; basic, parent, level, `totalBalance` and
`rechargeNeededForNextLevel` sorts continue through the lightweight page SQL.
The legacy `maxLevelPrice` and `nextLevelPrice` sort keys map to fixed scalar
expressions equivalent to their former projection aliases.  Those level
lookups run only when either key is explicitly requested and are not restored
to the default list projection.  PageHelper's unsafe-order API is used only for
these two compile-time expressions; request text is never passed to it.

These legacy computed aliases require global aggregation and therefore use a
dedicated path:

- `totalRecharge`, `totalWithdraw`, `diffAmount`;
- `withdrawFrozenAmount`, `todayWithdrawCount`;
- `directSubCount`, `allSubCount`.

That path first applies the complete shared member filter/data scope, computes
only the requested aggregate family for the complete candidate set, sorts it,
and lets PageHelper limit the ordered IDs.  A second query loads those page
rows and restores the ID order.  Page number, page size and total are copied to
the returned `Page`; a missing second-stage row raises an error rather than
silently shortening the page.  Its mapped `_COUNT` is the same projection-free
member count as the default path.

A read-only full-data comparison on `brushing_test` materialized all seven old
correlated values and the new batched values, ranked all 2,355 users with the
same stable ID tie-breaker, and compared the first ten positions.  Both ASC and
DESC were checked for every field: **14/14 sort cases had zero ID-position
mismatches**.  The comparison took 8.64 seconds because it intentionally
evaluated the legacy correlated formulas; it made no database changes.

Representative warm `EXPLAIN ANALYZE` timings for the final global-ID sort
statements were:

| Aggregate sort stage | Actual time |
| --- | ---: |
| Explicit aggregate-sort count | 0.268 ms |
| `totalRecharge DESC`, first 10 IDs | 10.5 ms |
| `directSubCount DESC`, first 10 IDs | 3.56 ms |
| `allSubCount DESC`, first 10 IDs | 14.2 ms |

The `allSubCount` plan parses the ancestors table once.  It deliberately does
not join every parsed row to every candidate before aggregation; that
cross-product shape measured about 11.1 seconds and was rejected during
verification.  Ancestor tokens are grouped and compared as strings, preserving
legacy `FIND_IN_SET` behavior for historical values such as `01` or a token
with leading whitespace instead of coercing them to numeric IDs.  `JSON_QUOTE`
remains in both page enrichment and global sorting paths.

A read-only synthetic check containing `1`, `01`, ` 1`, `1 `, `0,1`,
`10,1,2`, and `10, 1,02` produced identical legacy and exact-token counts for
candidate IDs 0, 1, 2, and 10.  Against all 2,355 restored rows, the exact-token
implementation had zero per-user count mismatches; both ASC and DESC top-ten
ID lists were unchanged.  The combined legacy/new full comparison took
2.34 seconds.  The final `EXPLAIN ANALYZE` parsed 8,304 tokens from 2,355 rows
once, materialized 1,126 grouped ancestor tokens, and returned the first ten
IDs in 14.2 ms.

The expanded targeted suite now contains **23 tests** covering the PageHelper
count interception, all seven fixed SQL aggregate branches, whitelist and
direction rejection, the fixed legacy level-expression pass-through, global ID
ordering, Page metadata, missing-row failure, empty pages and export batching.

## Recommended implementation contract

1. Keep PageHelper for the base page, but add a mapped statement named exactly
   `selectOrderMemberUserList_COUNT`.  PageHelper's default `_COUNT` lookup will
   use it instead of generating a count from the full projection.  Both the
   count and page statement must include one shared dynamic `WHERE` fragment.
2. The shared filter must preserve every current predicate: exact username and
   invitation code, the special four-digit phone suffix rule, real/fake flag,
   level, inclusive begin/end dates, agent self-or-descendant scope, and
   `forceNoResult`.
3. Make `selectOrderMemberUserList` return only the paged member, parent and
   level fields.  Preserve the returned Page object while enriching its rows in
   the service; otherwise `getDataTable(list)` loses PageHelper's total.
4. Query recharge values once for all page IDs.  Query withdrawal values once
   for all page IDs, calculating successful total, pending amount and today's
   count in one grouped pass.  Derive `diffAmount` from the two mapped totals;
   do not rescan either transaction table.
5. Query direct subordinate counts with `parent_id IN (...) GROUP BY parent_id`.
   For all-subordinate counts, parse every `ancestors` CSV once and restrict the
   resulting ancestor IDs to the page.  This preserves the current
   `FIND_IN_SET` semantics even for legacy hierarchy inconsistencies.
6. Empty pages must skip all enrichment queries.  Missing aggregate rows must
   map to zero, matching the current `IFNULL(..., 0)` behavior.

`selectOrderMemberUserVo` is also reused by scope lists, ID lookup, invitation
code lookup and username lookup.  Refactoring the main list must not silently
remove fields from those endpoints.  A dedicated lightweight list projection
is safer than globally weakening that shared fragment.

## Hierarchy risk and long-term direction

The restored backup is not fully internally consistent:

- one member references a missing parent;
- 486 child rows do not equal `parent.ancestors + ',' + parent.id`;
- one row contains its own ID in `ancestors`;
- descendant totals derived recursively from `parent_id` differ from current
  CSV/FIND_IN_SET totals for 86 users.

Consequently, replacing list counts immediately with a recursive `parent_id`
walk would change returned values, even though it is faster.  The compatible
short-term implementation should batch-parse `ancestors`.  After hierarchy
repair and validation, the durable model is a closure table such as
`member_hierarchy_closure(ancestor_id, descendant_id, depth)`, with a unique
key on both IDs and indexes supporting both directions.  It makes agent scope
and subordinate counts indexable and removes `FIND_IN_SET` from hot paths.

## Read-only reproduction

The script contains only `SELECT`, information-schema inspection and
`EXPLAIN ANALYZE`.  `EXPLAIN ANALYZE` executes its underlying query, so the
legacy section should be run only on a restored test database or during an
explicitly approved production diagnostic window.

Store credentials in the local MySQL login-path store rather than in the
repository:

```powershell
mysql_config_editor set --login-path=brushing-test --host=127.0.0.1 --port=3306 --user=<user> --password
Get-Content .\scripts\member_list_performance_baseline.sql |
  mysql --login-path=brushing-test --database=brushing_test
```

For result evidence, save stdout outside the repository or in an approved test
artifact location.  Do not add passwords, connection strings or production
hostnames to this report.

## Index rollout and rollback

The repeatable forward migration is
[`add_member_list_performance_indexes.sql`](../brushing-member/src/main/resources/db/add_member_list_performance_indexes.sql).
It checks `information_schema.STATISTICS` and uses prepared `ALTER TABLE`
statements only for missing exact visible definitions.  An identically named
but invisible index is not accepted as usable; it produces a deliberate
duplicate-name failure for operator review rather than being changed
automatically.  Verification output includes `IS_VISIBLE`.  Because production
reportedly already has all three indexes, its expected production action is a
no-op plus verification output.

Deploy the indexes before the optimized application version.  If application
rollback is necessary, roll back the application first and retain the indexes;
they are backward compatible.  Drop them only for a demonstrated storage or
write-performance reason, after checking for other consumers and scheduling a
maintenance window.  Exact removal commands and metadata-lock cautions are
commented at the end of the migration file.
