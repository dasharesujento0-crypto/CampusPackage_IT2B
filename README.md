# CampXpress – Campus Package Claim & Tracking System

A Java console application (Maven + SQLite) for registering departments, recipients and
packages, with role-based accounts and administrator account management.

## Requirements

- **JDK 17 or newer**
- **Maven 3.6+** (IntelliJ IDEA's bundled Maven is fine)
- The SQLite JDBC driver is downloaded automatically by Maven (`org.xerial:sqlite-jdbc`)

## Get the code

```
git clone https://github.com/dasharesujento0-crypto/CampusPackage_IT2B.git
cd CampusPackage_IT2B
```

## Build and run

**IntelliJ IDEA (recommended)**

1. `File > Open` and select the cloned `CampusPackage_IT2B` folder; wait for the Maven import.
2. Open `src/main/java/com/scc/Main.java` and click the green run arrow next to `main`.
3. Keep the run configuration's *Working directory* as the project folder (the default).

## The database

The database file `CampusPackage.db` is **not stored in Git** (it is in `.gitignore`).
It is created automatically, with all tables, the first time the program runs, in the folder
the program is launched from. Always launch from the project folder so the same database is
reused. The database path is shown on the first-run setup screen.

## First-run administrator setup (fresh clone)

A new database has no accounts, and nobody can register as an administrator. So, **only while
no `SYSTEM_ADMINISTRATOR` exists**, the program starts with a one-time setup screen:

1. Enter a full name and a username.
2. Enter a password (at least 8 characters) and confirm it.

That account is saved as `SYSTEM_ADMINISTRATOR` with status `APPROVED`. The setup screen then
never appears again, and it cannot create a second administrator or change an existing one.
There is no default username or password anywhere in the code or on GitHub.

> Whoever completes this step on the machine becomes the initial administrator, so the person
> setting up the project should do it right after the first run. Typing `0` at the name or
> username prompt skips it; it is offered again on the next launch.

## User roles – please read (why public registration shows only 2 roles)

The system has **four roles**, but the public **Register** option offers only **two**:

| Role | How a user gets it |
|---|---|
| `PACKAGE_STAFF` | Public registration (choose it when registering) |
| `DEPARTMENT_OFFICE_REPRESENTATIVE` | Public registration (choose it when registering) |
| `PACKAGE_ADMINISTRATOR` | **Administrator only** – assigned by a System Administrator |
| `SYSTEM_ADMINISTRATOR` | **Administrator only** – created by first-run setup, or assigned by a System Administrator |

**This is intentional, for security.** If anyone could pick an administrator role while
registering, anyone could give themselves full control. The two higher roles can only be
granted by an existing System Administrator.

How an administrator grants a role:

1. Log in as a System Administrator and choose **5. Manage User Accounts**.
2. Choose **4. Update User (Role & Status)**, enter the user's ID, then pick the new role and status.

Account statuses:

- A newly registered account is **PENDING** and **cannot log in** until it is approved.
- An administrator approves it with **5 > 3. Approve User Account**, or sets it to `APPROVED`
  in **5 > 4. Update User**.
- The Update User screen accepts only `APPROVED` or `PENDING`.
- An administrator cannot modify the account they are currently logged in with.
  Enter `0` at any prompt to cancel.

In the current version, all approved roles use the same menus (account information,
departments / offices, recipients, packages and the CSV export). The only role-specific menu is
**5. Manage User Accounts**, which is shown to `SYSTEM_ADMINISTRATOR` only.
For that role the home menu is 1-4, 5. Manage User Accounts, 6. Logout, 7. Exit; for every other
role it is 1-4, 5. Logout, 6. Exit.

## Everyday use

1. Start menu: **1. Login**, **2. Register**, **3. Exit**.
2. Register a staff or department account, then have an administrator approve it.
3. Log in to manage departments / offices, recipients and packages, and to export the package
   summary. The CSV report (`CampXpress_Report.csv`) is written to the launch folder.

## Activity logging and system audit trail (Deliverable 8)

Every important action is written to one table, `Activity_Logs`, by one class, `ActivityLogDao`
(`logAction`, `getAllLogs`, `getLogsByUserId`). All SQL stays in the DAO layer and uses
parameterized `PreparedStatement`s. The old `AuditLogDao` was removed; nothing else logs.

| Category | Action type | Logged in |
|---|---|---|
| Authentication | `USER_LOGIN`, `USER_LOGOUT` | `Authentication.recordLogin/recordLogout`, called by `CampXpressUI` |
| Account management | `USER_APPROVED`, `USER_UPDATED` | `UserDao.approveUser / updateAccountStatus / updateUserRoleAndStatus` |
| Core business process | `PACKAGE_REGISTERED` | `PackageDao.registerPackage` |
| Lifecycle / state change | `PKG_STATUS_UPDATED` | `PackageDao.updateStatus` (Package Management > 4. Update Package Status) |

Also logged: `LOGS_CLEARED`, and (not reachable from the menu yet) `PACKAGE_CLAIMED`,
`PACKAGE_RETURNED`, `PACKAGE_REMOVED`. The acting user is always the real logged-in `User`
passed down from the UI into the DAO.

Administrators: **5. Manage User Accounts > 5. View System Audit Logs** offers *1. View All System
Logs*, *2. Filter Logs by User ID*, *3. Clear Historical Logs* and *4. Back*. Clearing is for the
SYSTEM_ADMINISTRATOR only and asks you to type `YES`; anything else cancels. The deletion and one
`LOGS_CLEARED` entry (who cleared, and how many records) are saved in the same transaction.

### Database notes (deviation from the literal example)

The checklist example says `user_id REFERENCES users(id) ON DELETE CASCADE`. This project's table
is `Users` with primary key `Users_ID`, and it was **not** renamed or rebuilt. The foreign key
actually implemented is:

```
FOREIGN KEY (user_id) REFERENCES Users(Users_ID) ON DELETE CASCADE
```

(`users(id)` was **not** implemented: SQLite would reject every insert with "foreign key mismatch".)
`user_id` is nullable so historical entries that had no user are kept.
`PRAGMA foreign_keys = ON` is set on every connection in `DatabaseConnection`.

On startup `DatabaseInitializer` creates `Activity_Logs` if it does not exist. If it already exists,
it checks that all five columns (`id`, `user_id`, `action_type`, `description`, `timestamp`) and the
foreign key above are present, and stops with a message if not. It never copies, renames, backs up
or deletes the database or its log rows. Only one database file, `CampusPackage.db`, is used.

## Security note

Passwords are stored as plain text in the local SQLite file (as in earlier versions of this
project). The database is kept out of Git but should be treated as sensitive. Hashing the
passwords would be a good future improvement.

## Lost administrator access

There is no password-reset feature. If the only administrator password is lost, stop the
program and delete `CampusPackage.db` to start over (this erases all data), or edit the file
with a SQLite tool.