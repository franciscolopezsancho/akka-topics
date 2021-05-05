start database
	$ docker-compose up

run scripts
	$ psql -h 127.0.0.1 -d parcels -U parcels -f persistence_create_tables.sql 
	$ psql -h 127.0.0.1 -d parcels -U parcels -f projections_visited_cities.sql 
