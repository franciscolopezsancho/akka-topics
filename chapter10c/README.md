Please note that the test on this project need you to follow chapter09b/README.md 

Following those instructions you will start the database, set it up and send 3 pieces of data to it. 

After you need to add the necessary for the projections running the following scripts.

	$  psql -h 127.0.0.1 -d containers -U containers -f projection_cargos_per_container.sql 
	$ psql -h 127.0.0.1 -d containers -U containers -f projection_offset_store.sql