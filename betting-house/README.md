## Intro
This project is designed to reinforce most of the concepts you learned in this book. This application is just a scaffold, it is by no means finished. I encourage you to test different scenarios until you find that something is broken or doesn't work as you expected. For example, you can start with the TODO  created for you IntegrationSpec.

There are also some suggestions in SuggestionsSpec.

With the knowledge you have acquired so far, you should be able to improve the application and take it to the next level. 

## Run in local docker

    cd local-deployment

### start DB

    docker compose up

Create tables in DB. When prompt enter password `betting`

    cd ../common-deployment
    psql -h 127.0.0.1 -d betting  -U betting -f bet-projection.sql
    psql -h 127.0.0.1 -d betting  -U betting -f akka-persistence.sql
    psql -h 127.0.0.1 -d betting  -U betting -f akka-projection.sql

### start the application

    sbt "-Dconfig.resource=local.conf" \
    betting-house/run


### create and Update Market

    grpcurl -d  '{ 
      "marketId": "1243",
      "fixture": {"id": "id1", "homeTeam": "RM", "awayTeam": "MU"},
      "odds": {"winHome": 1.25, "winAway": 1.70, "tie": 1.10 },
      "opensAt": 123
    }' -plaintext localhost:9002 MarketService/Open

and verify its creation

      grpcurl -d '{"marketId": "1243"}' -plaintext localhost:9002 MarketService/GetState

Update 

      grpcurl -d  '{ 
        "marketId": "1243",
        "odds": {"winHome": 1.80, "winAway": 1.05, "tie": 1.40 },
        "opensAt": 123,
        "result": 1
      }' -plaintext localhost:9002 MarketService/Update

and verify 

    grpcurl -d '{"marketId": "1243"}' -plaintext localhost:9002 MarketService/GetState

### create wallet and add funds

Add
    
    curl -XPOST "localhost:9001/wallet/add?walletId=123&funds=222"


Remove
    
    curl -XPOST "localhost:9001/wallet/remove?walletId=123&funds=3"

Verify

    curl "localhost:9001/wallet?walletId=123"


### Place a couple of Bets// TODO review. 
    grpcurl -d  '{ 
      "betId": "112",
      "walletId": "123",
      "marketId": "1243",
      "odds": 1.80,
      "stake": 100,
      "result": 0
    }' -plaintext localhost:9000 BetService/Open

and verify 

    grpcurl -d  '{ "betId": "112"}' -plaintext localhost:9000 BetService/GetState


    grpcurl -d  '{ 
      "betId": "113",
      "walletId": "123",
      "marketId": "1243",
      "odds": 1.05,
      "stake": 10,
      "result": 1
    }' -plaintext localhost:9000 BetService/Open

and verify 

    grpcurl -d  '{ "betId": "112"}' -plaintext localhost:9000 BetService/GetState

### Get Projection

     grpcurl -d  '{ "marketId": "1243"}' -plaintext -emit-defaults localhost:9003 BetProjectionService/GetBetByMarket

here `-emit-defaults` does print zero in values with type int32. Such as `result` in `bet.proto` 

The result of this call is expresses the stake multiplied by the odds and grouped by result.

    {
      "sumstakes": [
        {
          "total": 179.99999523162842,
          "result": 0
        },
        {
          "total": 10.499999523162842,
          "result": 1
        }
      ]
    }

#### TIP
To create the required files for the Kubernetes Deployments
    
    kompose convert -f docker-compose.yaml

Those files are already created for you in k8s-deployemnt folder. This is just a tip.

## Run in Kubernetes

### start minikube [optional]
    
    minikube start

In another command like go to

    cd k8s-deployment

### create namespace to hold all the resources for the Akka Cluster, Kafka and DB

    kubectl create ns akka-cluster

### create Kafka cluster

    kubectl create -f 'https://strimzi.io/install/latest?namespace=akka-cluster' -n akka-cluster

    kubectl apply -f https://strimzi.io/examples/latest/kafka/kafka-persistent-single.yaml -n akka-cluster

#### create topic in Kafka cluster 
    
    kubectl -n akka-cluster run kafka-consumer -ti \
       --image=quay.io/strimzi/kafka:0.26.0-kafka-3.0.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic bet-projection --from-beginning

### create database

    kubectl apply -f postgres-betting-db-deployment.yaml 
    kubectl apply -f postgres-betting-db-service.yaml 
    kubectl port-forward pods/[pod-name] 5432:5432 -n akka-cluster 

change the `pod-name` accordingly 
    
    cd ../common-deployment
    psql -h 127.0.0.1 -d betting  -U betting -f bet-projection.sql
    psql -h 127.0.0.1 -d betting  -U betting -f akka-persistence.sql
    psql -h 127.0.0.1 -d betting  -U betting -f akka-projection.sql
 

### create betting-house image and deploy

    sbt docker:publish

    k -n akka-cluster apply -f deployment.yml


### link local ports to kubernetes pods


    k port-forward svc/my-cluster-kafka-bootstrap 9092:9092 -n akka-cluster

    k port-forward svc/betting-service 9000:9000 9001:9001 9002:9002 9003:9003 -n akka-cluster



#### To connect to DB

You can use the port forwarding to the DB to connect to it and check the expected events are stored in the DB.
  
    k port-forward svc/postgres-betting-db 5432:5432 -n akka-cluster


#### To connect to the betting services

    k -n akka-cluster expose deployment my-deployment-name --name=betting-service


    k port-forward svc/betting-service 9000:9000 9001:9001 9002:9002 9003:9003 -n akka-cluster

#### To connect to the Kafka topic

You can use a Kafka consumer to check if the expected events end up in the Topic of the market. One possibility is the Strimzi Kafka consumer , which you can use as follows.
    
    kubectl -n akka-cluster run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.26.0-kafka-3.0.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic bet-projection --from-beginning      
                  
This spins a kafka-consumer pod that listens to the market-projection topic and connect you to the pod. 


