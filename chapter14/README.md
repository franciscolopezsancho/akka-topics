# to produce to a topic

Start the docker instances by running the following from the root of chapter14 folder. 
    
    docker compose up

By default creates topic named `test`, `test2`, and so on until `test6`. 

Then to produce to the topic test you can use kafkcat like this:
    
    kcat -P -b 127.0.0.1:9092 -t test

This starts kafkacat in producer mode. To send a record, enter some text and press ENTER.


To make sure this works entering the follwing should give you the produced messages.

    kcat -C -b 0.0.0.0:9092 -t test