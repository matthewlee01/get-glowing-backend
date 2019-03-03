#!/usr/bin/env bash

docker exec -i --user postgres globar_db_1 psql globardb -a  <<__END
\d customers;
\d vendors;
\d ratings;
select * from customers;
__END

