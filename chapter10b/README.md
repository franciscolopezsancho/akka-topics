## start database
	$ docker-compose up

### run scripts
	$ psql -h 127.0.0.1 -d containers -U containers -f persistence_create_tables.sql 


The password is `containers` as you can find the docker-compose.yml

### start the application 

    $ sbt "chapter10b/run"
