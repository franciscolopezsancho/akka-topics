## start database
	$ docker-compose up

### run scripts
	$ psql -h 127.0.0.1 -d containers -U containers -f persistence_create_tables.sql 


The password is `containers` as you can find the docker-compose.yml

Once is the app is running:

Press enter and then copy one input at a time - without the dolar - and press enter afterwards.
#### Input
	$ 9 456 sack 22 
	$ 9 459 bigbag 15
	$ 11 499 barrel 120 

## Known Problems
After pressing ENTER it appears a `M^` in the console. As a consequence this not produces a new line and you can't interact with the console to send input to create containers. 

To solve this enter the following:

	stty sane

This only works for MacOs or Linux. 