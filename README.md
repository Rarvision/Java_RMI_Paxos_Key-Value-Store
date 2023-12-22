# Multi-threaded Key-Value Store based on 2PC protocol

## Introduction

This Java RMI project implements a distributed key-value store using the Paxos. Every server in this project implement all Paxos interfaces: Proposer, Acceptor, and Learner. Each server server maintains a concurrent hash map as the key-value store, and the client interacts with servers using RMI to send PUT, GET, and DELETE operations. The Client can choose any active server to send the request. The server which received the request will become the proposer. All other servers registered will be updated correspondingly (or fail together) using Paxos.

After running, the client will pre-populate the key-value store by reading key-value pairs from the file "KeyValuePairs.txt", then perform 5 PUT, 5 GET, and 5 DELETE operations. The coordinator is used for communication between servers based on the 2PC protocol. The client can send an update request to any server registered to the coordinator. All servers registered to the same coordinator will be updated synchronously.

The server can simulate random failures. Each method in the server has a timeout mechanism. There is a 40% chance that any paxos process will cause the program to sleep for 600 milliseconds, causing the corresponding operation to fail. (These value can be changed in the server). Paxos ensures that replicas of the server can still maintain consistency when failure happens.

The log files of the coordinator, servers, and the client will be automatically generated as "Server\<ServerID\>-log.txt" and "Client-log.txt" after startup.

This README provides instructions on how to compile, configure, and run the project with five server replicas.

## Prerequisites

- Java JDK (version 15 or higher is recommended).
- Ensure the `rmiregistry` tool is available in your path.

## Directory Structure

- `src/`: Contains all the source code for the client and server.
	- `AcceptorInterface.java`:  Interface of the Acceptor role in Paxos.
	- `ProposerInterface.java`: Interface of the Proposer role in Paxos.
	- `LearnerInterface.java`: Interface of the Learner role in Paxos.
	- `KVStoreInterface.java`: RMI interface for the server.
	- `PaxosState.java`: Prototype of Paxos instances for each operation.
	- `ProposalResponse.java`: Prototype of responses returned by Paxos methods.
	- `Client.java`: The client application for interacting with the key-value store.
	- `Server.java`: Server replica which can be any role in Paxos.
	- `PaxosServerCreator.java`:  Helper class for creating, binding and configuring the Paxos servers.
	- `Logger.java`: Utility class for logging.


## Compilation

1. Open the terminal, navigate to the /src folder of the project.

2. Compile all Java files using the following command:
```
javac *.java
```

## Running the Project

### Starting the RMI Registry

1. Open a terminal window.
2. Start the RMI registry on a specific port (e.g., 1099):

```bash
rmiregistry 1099
```
This starts the RMI registry.

### Start Server Replicas

1. Open a new terminal window.
2. Run the server creator to create server replicas.
```
java PaxosServerCreator <Port>
```
For example (the coordinator runs on port 5000):
```
java PaxosServerCreator 5000
```
The creator will create 5 server replicas with consecutive port numbers. For this example, it will create servers on 5000, 5001, 5002, 5003, 5004 

### Run the Client
1.  Open a new terminal window.
2.  Run the client and connect it to anyone of the server replicas:
```
java Client <ServerHost> <ServerPort>
```    
For example:
```
java Client localhost 5001
```

Server on the port `5001` will become the proposer. Client will send request automatically after running.

## Notes

- Ensure that the server and `rmiregistry` are running and are accessible from the client.
- Ensure all components are started in the correct order: RMI registry, server creator, and then the client.
- The client expects a `.txt` file with key-value pairs in the format `key=value` in the `/src` directory for pre-population.
- Press Enter in the terminal which runs PaxosServerCreator to close servers.
- Previous log files is attached in `/previousLogFiles` for your reference.

