# Source Backend

This is the backend for the Source platform. You can find documentation on setup for development below.

## Dependencies

- >= openjdk version 11.

## Development setup

- Pull the project from GitHub.
- Create a .env file containing the following required information:

```.env
SUPER_SECRET_KEY [string, minimum 32 characters]
GOOGLE_CLIENT_ID [string, from Google Console]
GOOGLE_CLIENT_SECRET [string, from Google Console]
```

- Run the provided shell script with `./nrepl.sh` to start your nrepl server. For this, you will need the required nrepl alias in `.clojure/deps.edn`.
- In order to start your server, evaluate the `server/start-server` function in `dev/dev.clj`. 
When you have made changes, you can restart the server by evaluating `server/restart-server`.

## Database Migration

Database migration is handled using the provided migrate.sh shell script.
You should have a db directory and ensure your configuration is pointing to it. The default is `.db/` in the root, 
this can be configured with the `DATABASE_DIR` variable in the environment.

The migration system makes use of the kepler/mallard library, take a look at their [docs](https://github.com/kepler16/mallard) to find out more on how it works.
The datasource you will be operating on is passed in as context to the run-up and run-down functions.
e.g. `./migrate.sh [argument]`
The following command arguments are available:
- `up`: Run all up migrations.
- `down`: Undo all migrations. Executes in reverse order.
- `redo`: Rerun the last applied migration (runs down then up).
- `undo`: Undo the last applied migration.
- `next`: Run the next unapplied migration. Same as up, but runs only 1.

In order to make a new migration, add a migration.clj file to `src/source/migrations/` with a numbered prefix (e.g. 002_new_table.clj) 
and implement the following functions:
- `(defn run-up! [context])`: Run database actions to take place when running up. e.g. Create tables and seed data.
- `(defn run-down! [context])`: Run database actions to take place when running down. e.g. Drop tables.

## Database Seeding

- Create a json file with the following structure to add admin accounts with which to seed the database.
The default name is `admins.json`, this can be configured with the `ADMINS_PATH` environment variable.
You will need to hash your chosen password by evaluating the `hash-password` function provided in `src/source/password.clj`:

```json
[
    {"email": "johndoe@admin.com", "password": "hashedadminpassword"}
]
```

## Testing

We are making use of cognitect-labs/test-runner for running our tests. Run the provided shell script with `./test.sh` to run all the tests.
Unit tests are written and placed in clj files in `tests/source-be/`. These files should have the `_test` postfix (e.g. google_auth_test.clj) and tests should be 
written per namespace and cover all important functions.

