# Backend workspace
This workspace is soley dedicated to the backend. The backend has the default settings for to give authentication using JWT tokens and all api endpoints by default have authentication by default. In other words, the access token provided will be needed for accessing all api endpoints unless the api endpoint explicitly disabled auth. Ontop of authentication, authorization is also enabled across all endpoints using role based access control (RBAC). 

Roles can be created, managed, and assigned through the django admin. Furthermore, users can be created and managed through the django admin.

# Backend sevices setup
All backend services can be quickly setup using the docker compose file. A .env file will be needed in this directory (backend) as show below. The optional values have defaults in the docker compose file. If you use your own values, the DB backup command will need slight changes. The pgadmin variables will be needed for logging into the pgadmin. The Postgres variables will be need to connect to the db. 

The database has its own container built on the PostgreSQL image.

The backend has contiainer is built on a base python image.

The container used for monitoring is built using the PgAdmin image.

```
#your .env file
POSTGRES_PASSWORD=${PASSWORD:-yourPostgresPassword}
PGADMIN_PASSWORD=${PASSWORD:-yourPgadminPassword}

#optional
PGADMIN_EMAIL=......
POSTGRES_USER=......
POSTGRES_DB=......
```
# Backend documentation
Another .env file will be needed from the backend. Location of this is basically wherever you launch the `manage.py` in `django_backend/scheduling_backend`. The `.env` file variables will be like the below.
```
#this is the .env file for django backend
POSTGRES_PASSWORD=passwordFromDockerComposeFile
POSTGRES_HOST=hostUrlOfPostgres
```
Api endpoint documentation is all in the Postman workspace.
# Quick overview
The api endpoint for getting tokens is {base_url}/api/token.

The teacher endpoints are at ```{base_url}/api/teachers/```. The survey endpoints are at ```{base_url}/api/surveys/```. The teacher endpoints are at ```{base_url}/api/history/```.

# Backing up the PostgreSQL DB
## Command to backup
docker exec postgres-service pg_dump -Fc -U admin scheduling > db.dump

## Command to restore
docker exec -i postgres-service pg_restore -d postgres -U admin --clean --create < db.dump

Note that when using the command the db you connect to can't be scheduling. It must be some other db. If you want to know what db's are available
run the command 'docker exec -it postgres-service psql -U admin -d scheduling' to open connection and shell then type '\l' to see the available DBs.

Note that when restoring to the scheduling database, you can't have any connection open to it.