start database
	$ docker-compose up
to rerun persistence:


	$ psql -h 127.0.0.1 -d containers -U containers -f projections/persistence_create_tables.sql 

	sbt && persistence/runMain example.persistence.PMain

	Three inputs:
$ 9 456 sack 22 
$ 9 459 bigbag 15
$ 11 499 barrel 120 



run scripts
	$  psql -h 127.0.0.1 -d containers -U containers -f projections/projection_cargos_per_container.sql 
	$ psql -h 127.0.0.1 -d containers -U containers -f projections/projection_create_tables.sql
