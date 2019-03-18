#!/usr/bin/env bash
# this script is intended to spin up a db containter for the purpose of developing against, and testing
docker run -ti -d -p25432:5432 --name globar_db_1 globar_db  
