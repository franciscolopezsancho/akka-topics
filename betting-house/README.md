grpcurl -d  '{ 
  "marketId": "1243",
  "fixture": {"id": "id1", "homeTeam": "RM", "awayTeam": "MU"},
  "odds": {"winHome": 1.25, "winAway": 1.70, "tie": 1.10 },
  "opensAt": 123
}' -plaintext localhost:9002 MarketService/Open


grpcurl -d '{"marketId": "1243"}' -plaintext localhost:9002 MarketService/GetState

grpcurl -d  '{ 
  "marketId": "1243",
  "odds": {"winHome": 1.80, "winAway": 1.05, "tie": 1.40 },
  "opensAt": 123,
  "result": 0
}' -plaintext localhost:9002 MarketService/Update


grpcurl -d '{"marketId": "1243"}' -plaintext localhost:9002 MarketService/GetState

curl "localhost:9001/wallet?walletId=123"


curl -XPOST "localhost:9001/wallet/add?walletId=123&funds=222"


curl -XPOST "localhost:9001/wallet/remove?walletId=123&funds=333"


grpcurl -d  '{ 
  "betId": "112",
  "walletId": "123",
  "marketId": "1243",
  "odds": 1.80,
  "stake": 100,
  "result": 1
}' -plaintext localhost:9000 BetService/Open

grpcurl -d  '{ 
  "betId": "112"
}' -plaintext localhost:9000 BetService/GetState


grpcurl -d  '{ 
  "marketId": "1243"
}' -plaintext localhost:9003 BetProjectionService/GetBetByMarket

kompose convert -f docker-compose.yaml

psql -h 127.0.0.1 -d betting -U betting -f akka-persistence.sql
psql -h 127.0.0.1 -d betting -U betting -f akka-projection.sql
psql -h 127.0.0.1 -d betting -U betting -f bet-projection.sql


 kafkacat -C -b 127.0.0.1:9092 -t bet-projection


 ### minikube



kubectl create -f 'https://strimzi.io/install/latest?namespace=akka-cluster' -n akka-cluster

kubectl apply -f https://strimzi.io/examples/latest/kafka/kafka-persistent-single.yaml -n akka-cluster

 publish image

 k -n akka-cluster apply -f deployment.yml

k port-forward svc/postgres-containers-db 5432:5432

k port-forward svc/my-cluster-kafka-bootstrap 9092:9092

k -n akka-cluster expose deployment my-deployment-name \
  --name=betting-service

k port-forward svc/betting-service 9000:9000 9001:9001 9002:9002 9003:9003
