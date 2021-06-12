start database
	$ docker-compose up

run scripts
	$ psql -h 127.0.0.1 -d parcels -U parcels -f persistence/persistence_create_tables.sql 

When running:


Three inputs:
$ 9 456 sack 22 
$ 9 459 bigbag 15
$ 11 499 barrel 120 