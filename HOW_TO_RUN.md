# How to Run — Dealer Inventory

## Prerequisites

- **Docker Desktop** must be installed and the Docker daemon must be running before any command below.
  - Mac: Open Docker Desktop from Applications and wait for the whale icon in the menu bar to show "Running".

---

## Start the Application

```bash
./start.sh
```

This will:
1. Start a PostgreSQL container
2. Build the Spring Boot application
3. Run Liquibase migrations automatically
4. Start the app on **http://localhost:8080**

---

## Start the Database Only (run app from IntelliJ)

```bash
./start.sh --db
```

Use this when you want to run the app inside IntelliJ while still using the Dockerised database.

---

## Stop the Application

```bash
./start.sh --stop
```

Stops all running containers.

---

## Clean Reset (wipe data + rebuild)

```bash
./start.sh --clean
```

Stops containers, removes volumes (all DB data is lost), and does a fresh start.

---

## Quick Verification

Once the app is running, test it with:

```bash
# Should return 200 with an empty dealers list
curl -u tenant1_user:password \
  -H "X-Tenant-Id: tenant-1" \
  http://localhost:8080/dealers
```

---

## Credentials

| Username | Password | Role | Tenant |
|---|---|---|---|
| `tenant1_user` | `password` | USER | `tenant-1` |
| `tenant2_user` | `password` | USER | `tenant-2` |
| `global_admin` | `admin` | GLOBAL_ADMIN | unrestricted |

---

## Postman Collection

Import `DealerInventory.postman_collection.json` into Postman and run the folders in order:
1. `01 - Dealers`
2. `02 - Vehicles`
3. `03 - Admin`
4. `04 - Security Scenarios`
