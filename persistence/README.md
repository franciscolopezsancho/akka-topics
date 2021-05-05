start database
	$ docker-compose up

run scripts
	$ psql -h 127.0.0.1 -d parcels -U parcels -f persistence_create_tables.sql 

When running:


Three inputs:
$ 111 456 sack 22 
$ 111 459	bigbag 15
$ 223	499 barrel 120 