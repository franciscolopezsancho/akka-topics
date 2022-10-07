### start the LoggerShardedApp in cluster mode

Add first node

    $ sbt "chapter10c/runMain example.shardeddeamon.LoggerSharded 25523"

Add second node

    $ sbt "chapter10c/runMain example.shardeddeamon.LoggerSharded 25524"


### start the projection consuming from DB in Sharded Mode

Please note that you need to follow chapter09b/README.md to run this project. With these instructions you start the database, set it up and send 3 "containers" to it.

Afterwards you need to add the necessary tables for the projections running the following scripts.

	$ psql -h 127.0.0.1 -d containers -U containers -f projection_cargos_per_container.sql 
	$ psql -h 127.0.0.1 -d containers -U containers -f projection_offset_store.sql
 

### start the application


    $ sbt "chapter10c/runMain example.shardeddeamon.Main 25525"

if you want to add another node enter the following in another command line

	$ sbt "chapter10c/runMain example.shardeddeamon.Main 25526"