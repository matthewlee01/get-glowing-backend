#!/usr/bin/env bash

# this script compiles all of the clojure, rebuilds all of the docker images, shuts
# down all of the running containers, and then starts new containers based off the
# new images.  the idea is that this script can be run in production after pulling
# new code from the repo.

# build the backend images
echo "------------------------------------------------------------------------"
echo "building backend images"
cd ~/work/globar/bin/prod &&
./build-globar-images.sh &&

# build the frontend
echo
echo "------------------------------------------------------------------------"
echo "compiling frontend and building nginx container"
cd ~/work/archon/bin &&
./build-nginx-images.sh &&

# stop all the running instances of the backend containers
echo
echo "------------------------------------------------------------------------"
echo "stopping backend containers"
cd ~/work/globar/bin/prod &&
./globar-down.sh &&

# stop the frontend nginx
echo
echo "------------------------------------------------------------------------"
echo "stopping frontend nginx container"
cd ~/work/archon/bin &&
./stop-nginx.sh &&

# cleanup docker
echo
echo "------------------------------------------------------------------------"
echo "docker system prune -f"
docker system prune -f &&

# start the backend servers
echo
echo "------------------------------------------------------------------------"
echo "starting the backend containers"
cd ~/work/globar/bin/prod &&
./globar-up.sh &&

# start nginx
echo
echo "------------------------------------------------------------------------"
echo "starting nginx"
cd ~/work/archon/bin &&
./run-nginx.sh

echo
echo "------------------------------------------------------------------------"
echo "SUCCESS!!"
