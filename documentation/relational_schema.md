# FindIt – Relational Schema & Normalization Proof

## 1. Relational Schema Representation

Notation:
- **Bold / Underlined**: Primary Key
- *Italicized / [FK]*: Foreign Key

---

### Relation 1: USERS
`USERS (`**`user_id`**`, name, email, phone, password_hash, role, created_at)`
- **Primary Key**: `user_id`
- **Candidate Key**: `email`
- **Integrity Constraints**:
  - `UQ_USERS_EMAIL`: `email` is UNIQUE
  - `CK_USERS_ROLE`: `role IN ('STUDENT', 'ADMIN')`
  - `CK_USERS_PHONE_LEN`: `LENGTH(phone) >= 10`

### Relation 2: ITEMS
`ITEMS (`**`item_id`**`, item_name, category, description, color, image_url, created_at)`
- **Primary Key**: `item_id`
- **Integrity Constraints**:
  - `CK_ITEMS_CATEGORY`: `category IN ('Electronics', 'Accessories', 'Stationery', 'Personal Items', 'Documents', 'Others')`
  - `CK_ITEMS_NAME_LEN`: `LENGTH(TRIM(item_name)) >= 2`

### Relation 3: REPORTS
`REPORTS (`**`report_id`**`, `*`item_id [FK]`*`, `*`user_id [FK]`*`, report_type, location, report_date, status, private_details, created_at)`
- **Primary Key**: `report_id`
- **Foreign Keys**:
  - `item_id` references `ITEMS(item_id)` ON DELETE CASCADE
  - `user_id` references `USERS(user_id)` ON DELETE CASCADE
- **Integrity Constraints**:
  - `CK_REPORTS_TYPE`: `report_type IN ('LOST', 'FOUND')`
  - `CK_REPORTS_STATUS`: `status IN ('OPEN', 'MATCHED', 'RETURNED', 'CLOSED')`

### Relation 4: MATCHES
`MATCHES (`**`match_id`**`, `*`lost_report_id [FK]`*`, `*`found_report_id [FK]`*`, match_score, match_status, created_at)`
- **Primary Key**: `match_id`
- **Candidate Key**: `(lost_report_id, found_report_id)`
- **Foreign Keys**:
  - `lost_report_id` references `REPORTS(report_id)` ON DELETE CASCADE
  - `found_report_id` references `REPORTS(report_id)` ON DELETE CASCADE
- **Integrity Constraints**:
  - `UQ_MATCH_PAIR`: `UNIQUE (lost_report_id, found_report_id)`
  - `CK_MATCH_SCORE`: `match_score BETWEEN 0.0 AND 4.0`
  - `CK_MATCH_STATUS`: `match_status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CLAIMED')`

### Relation 5: CLAIMS
`CLAIMS (`**`claim_id`**`, `*`match_id [FK]`*`, `*`claimant_id [FK]`*`, claim_date, claim_status, `*`reviewed_by [FK]`*`, review_date, verification_details, admin_comments, created_at)`
- **Primary Key**: `claim_id`
- **Foreign Keys**:
  - `match_id` references `MATCHES(match_id)` ON DELETE CASCADE
  - `claimant_id` references `USERS(user_id)` ON DELETE CASCADE
  - `reviewed_by` references `USERS(user_id)` ON DELETE SET NULL
- **Integrity Constraints**:
  - `CK_CLAIMS_STATUS`: `claim_status IN ('PENDING', 'APPROVED', 'REJECTED')`

### Relation 6: NOTIFICATIONS
`NOTIFICATIONS (`**`notification_id`**`, `*`user_id [FK]`*`, `*`match_id [FK]`*`, message, notification_date, read_status)`
- **Primary Key**: `notification_id`
- **Foreign Keys**:
  - `user_id` references `USERS(user_id)` ON DELETE CASCADE
  - `match_id` references `MATCHES(match_id)` ON DELETE SET NULL
- **Integrity Constraints**:
  - `CK_NOTIF_READ_STATUS`: `read_status IN ('READ', 'UNREAD')`

### Relation 7: AUDIT_LOG
`AUDIT_LOG (`**`audit_id`**`, `*`user_id [FK]`*`, operation, table_name, record_id, details, log_date)`
- **Primary Key**: `audit_id`
- **Foreign Keys**:
  - `user_id` references `USERS(user_id)` ON DELETE SET NULL

### Relation 8: TRANSACTION_LOG
`TRANSACTION_LOG (`**`log_id`**`, transaction_id, operation, table_name, record_id, old_value, new_value, log_status, log_date)`
- **Primary Key**: `log_id`

---

## 2. Functional Dependencies (FDs)

| Relation | Functional Dependencies | Primary / Superkey |
|---|---|---|
| **USERS** | `user_id -> {name, email, phone, password_hash, role, created_at}`<br/>`email -> {user_id, name, phone, password_hash, role, created_at}` | `user_id`, `email` |
| **ITEMS** | `item_id -> {item_name, category, description, color, image_url, created_at}` | `item_id` |
| **REPORTS** | `report_id -> {item_id, user_id, report_type, location, report_date, status, private_details, created_at}` | `report_id` |
| **MATCHES** | `match_id -> {lost_report_id, found_report_id, match_score, match_status, created_at}`<br/>`{lost_report_id, found_report_id} -> {match_id, match_score, match_status, created_at}` | `match_id`, `{lost_report_id, found_report_id}` |
| **CLAIMS** | `claim_id -> {match_id, claimant_id, claim_date, claim_status, reviewed_by, review_date, verification_details, admin_comments, created_at}` | `claim_id` |
| **NOTIFICATIONS** | `notification_id -> {user_id, match_id, message, notification_date, read_status}` | `notification_id` |
| **AUDIT_LOG** | `audit_id -> {user_id, operation, table_name, record_id, details, log_date}` | `audit_id` |
| **TRANSACTION_LOG** | `log_id -> {transaction_id, operation, table_name, record_id, old_value, new_value, log_status, log_date}` | `log_id` |

---

## 3. Normalization Proof (1NF to BCNF)

### First Normal Form (1NF)
- **Requirement**: Each column contains only atomic (indivisible) values, and there are no repeating groups or arrays.
- **Verification**: All attributes (`user_id`, `name`, `email`, `location`, `report_date`, etc.) hold single scalar values. No multi-valued attributes or nested arrays exist in any table. Hence, all 8 relations satisfy **1NF**.

### Second Normal Form (2NF)
- **Requirement**: The relation is in 1NF, and every non-prime attribute is fully functionally dependent on the entire primary key (no partial dependencies on a subset of a composite key).
- **Verification**:
  - Relations `USERS`, `ITEMS`, `REPORTS`, `CLAIMS`, `NOTIFICATIONS`, `AUDIT_LOG`, and `TRANSACTION_LOG` have single-column primary keys. Therefore, partial functional dependency is mathematically impossible.
  - In `MATCHES`, the composite candidate key is `{lost_report_id, found_report_id}`. Both `match_score` and `match_status` depend on the *pair* of reports together, not on either report alone. Hence, no partial dependency exists.
  - Therefore, all 8 relations satisfy **2NF**.

### Third Normal Form (3NF)
- **Requirement**: The relation is in 2NF, and no non-prime attribute is transitively dependent on the primary key (no $X \rightarrow Y \rightarrow Z$ where $Y$ is a non-prime attribute).
- **Verification**:
  - In `REPORTS`, `user_id` and `item_id` are foreign keys. The attributes `location`, `report_date`, and `status` depend directly on `report_id`.
  - Item details (`item_name`, `category`, `color`) are deliberately separated into `ITEMS` rather than duplicated in `REPORTS`, eliminating transitive dependencies.
  - User details (`name`, `phone`) are stored in `USERS`, not in `REPORTS` or `CLAIMS`.
  - Hence, no transitive dependencies exist, and all 8 relations satisfy **3NF**.

### Boyce-Codd Normal Form (BCNF)
- **Requirement**: A relation is in BCNF if for every functional dependency $X \rightarrow Y$, $X$ is a superkey.
- **Verification**:
  - In all 8 tables, every left-hand side determinant $X$ in the non-trivial functional dependencies is a candidate key (`user_id`, `email`, `item_id`, `report_id`, `match_id`, `{lost_report_id, found_report_id}`, `claim_id`, `notification_id`, `audit_id`, `log_id`).
  - Therefore, the schema is in **Boyce-Codd Normal Form (BCNF)**.

