#!/usr/bin/env bash

docker exec -i --user postgres globar_db_1 psql globardb -a  <<__END
select * from Images;
__END

