# globar

### NOTE
This is a snapshot of the server code for the get-glowing web app.
Some sensitive information and files have been redacted for privacy reasons,
so the code as it is in this version of the repo will most likely not compile.

## Usage

### To support front-end development
To just have a backend available for the front-end to talk to:
1. get the source: ```git clone http://gitlab.j3mc.ca/dev/globar.git```
2. build the docker images with the script in the 'bin' folder: ```./build-globar-images.sh```
3. start the system with the script in the 'bin' folder: ```./docker-up.sh```

### To start the backend server in a repl
To get an environment up to work on the globar code itself:
1. follow steps #1 and #2 above
2. start the database container: ```./docker-run-testdb.sh```
3. start a repl: ```lein repl```
4. start the system in the repl: ```(start)```
5. to stop the system: ```(stop)```

### To populate the sample vendors
To load the sample vendors from the .edn file
1. start the containers to bring up the production mode system: ```bin/docker-up.sh```
2. start a shell inside the docker container running the globar server: ```docker exec -it globar_app_1 sh```
3. run the script to install telnet and connect a repl to the globar server: ```./telnet-to-repl.sh```
4. at the repl prompt, change to the globar.core directory and run the function that loads the sample data: ```(in-ns 'globar.core) (load-sample-vendors)```

### To inspect the raw database tables
Run one of the ```list-...``` scripts in the "bin" directory

For example:
```./list-users.sh``` will do a select * on the Users table

### Unit Tests
New unit tests should be added under the `test/globar/` directory

All unit tests should pass before committing new code: `lein test`

To run a single unit test: `lein test :only globar.<unit-test-namespace>`

### Production
To deploy to the production machine, the source lives in work/archon and work/globar directories and the PRODUCTION variable is set in the .bashrc file.  This is copied into the globar_app container to signal that this is the production environment.
Steps to deploy:
1. ```git pull``` in globar, then another ```git pull``` in archon
2. goto globar/bin/prod and run ```deploy.sh```
3. do a ```docker logs globar_app_1``` to check for the PRODUCTION log emitted

## License

Copyright © 2019 J3MC Software

