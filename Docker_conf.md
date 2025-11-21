# Lab configuration 
1. docker network create mynet

# Server Command
1. docker build -f server-dockerfile -t server .
2. docker run --rm -it --name server --network=mynet -p 7580:7580 server:latest 

# Client Command 
1. docker build -f client-dockerfile -t client .
2. docker run --rm -it --network=mynet client:latest