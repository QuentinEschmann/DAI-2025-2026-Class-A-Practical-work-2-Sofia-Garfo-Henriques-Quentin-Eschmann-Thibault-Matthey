# Lab configuration 
1. docker network create mynet
2. docker build -f dockerfile -t warehouse .

# Server Command
1. docker run --rm -it --name server --network=mynet -p 7580:7580 warehouse:latest Server

# Client Command 
1. docker run --rm -it --network=mynet warehouse:latest --host server Client