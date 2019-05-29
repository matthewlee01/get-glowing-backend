#!/usr/bin/env bash

docker exec -i --user postgres globar_db_1 psql globardb -a  <<__END
\d Users;
\d vendors;
\d vendor_rating;
\d vendor_bookings;
\d vendor_available_time;
__END

