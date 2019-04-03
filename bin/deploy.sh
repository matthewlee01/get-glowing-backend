#!/usr/bin/env bash

# build the backend images
echo "------------------------------------------------------------------------"
echo "building backend images"
cd ~/work/globar/bin &&
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
cd ~/work/globar/bin &&
./docker-down.sh &&

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
cd ~/work/globar/bin &&
./docker-up.sh &&

# start nginx
echo
echo "------------------------------------------------------------------------"
echo "starting nginx"
cd ~/work/archon/bin &&
./run-nginx.sh

echo
echo "------------------------------------------------------------------------"
echo "SUCCESS!!"
