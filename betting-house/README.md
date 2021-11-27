## Run in local docker

    

### start DB

    docker compose up

Create tables in DB

    psql -h 127.0.0.1 -d betting  -U betting -f bet-projection.sql
    psql -h 127.0.0.1 -d betting  -U betting -f akka-persistence.sql
    psql -h 127.0.0.1 -d betting  -U betting -f akka-projection.sql

### start the application

    sbt "-Dconfig.resource=local.conf"
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
        "result": 0
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


### Place Bet
    grpcurl -d  '{ 
      "betId": "112",
      "walletId": "123",
      "marketId": "1243",
      "odds": 1.80,
      "stake": 100,
      "result": 1
    }' -plaintext localhost:9000 BetService/Open

and verify 

    grpcurl -d  '{ 
      "betId": "112"
    }' -plaintext localhost:9000 BetService/GetState

### Get Projection

grpcurl -d  '{ 
  "marketId": "1243"
}' -plaintext localhost:9003 BetProjectionService/GetBetByMarket

kompose convert -f docker-compose.yaml




 kafkacat -C -b 127.0.0.1:9092 -t bet-projection


## minikube

### create kafka cluster

kubectl create -f 'https://strimzi.io/install/latest?namespace=akka-cluster' -n akka-cluster

kubectl apply -f https://strimzi.io/examples/latest/kafka/kafka-persistent-single.yaml -n akka-cluster

kubectl -n akka-cluster run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.26.0-kafka-3.0.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic bet-projection --from-beginning

### create database

kubectl apply -f postgres-containers-db-deployment.yaml 
kubectl apply -f postgres-containers-db-service.yaml 

### create betting-house and deploy

 sbt docker:publish

 k -n akka-cluster apply -f deployment.yml

 k -n akka-cluster expose deployment my-deployment-name \
  --name=betting-service

### link local ports to kubernetes pods
k port-forward svc/postgres-containers-db 5432:5432

k port-forward svc/my-cluster-kafka-bootstrap 9092:9092

k port-forward svc/betting-service 9000:9000 9001:9001 9002:9002 9003:9003
