initialize DB
	$  psql -h 127.0.0.1 -d betting -U betting -f betting-db-projection/projection_bet.sql 
	$ psql -h 127.0.0.1 -d betting -U betting -f betting-db-projection/projection_offset_store.sql


start database
	$ docker-compose up
start projection

	sbt && betting-db-projection/run




