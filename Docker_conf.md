# Lab configuration 
1. docker network create mynet
2. docker build -f Dockerfile -t WareHouse .

# Server Command
1. docker run --rm -it --name server --network=mynet -p 7580:7580 WareHouse:latest Server

# Client Command 
1. docker run --rm -it --network=mynet WareHouse:latest --host server Client