docker exec -i globar_db_1 psql -Uglobar_role globardb -a <<__END

drop table if exists vendor_rating;
drop table if exists users;
drop table if exists vendors;

CREATE OR REPLACE FUNCTION maintain_updated_at()
RETURNS TRIGGER AS \$\$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
\$\$ language 'plpgsql';
__END
