docker exec -i --user postgres globar_db_1 psql globardb -a  <<__END
select * from vendor_calendar;
__END

