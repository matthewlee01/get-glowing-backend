# globar

The backend server 

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

### To inspect the raw database tables
Run one of the ```list-...``` scripts in the "bin" directory

For example:
```./list-customers.sh``` will do a select * on the customers table

### Unit Tests
New unit tests should be added under the `test/globar/` directory

All unit tests should pass before committing new code: `lein test`

To run a single unit test: `lein test :only ` globar.<unit-test-namespace>

## License

Copyright © 2019 J3MC Software

