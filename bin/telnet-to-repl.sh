# This script installs a telnet client and then invokes telnet to connect to the running globar server
# and get access to a repl.  this facilitates the invokation of any functions within the running server
# but typically used to run the function that loads sample vendors.
# This script is copied into globar_app containers so that it can be invoked once you log into the
# container.  You would typically do this by executing 'docker exec -it globar_app_1 sh'
apk update
apk add busybox-extras
busybox-extras telnet localhost 5555
