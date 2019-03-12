# Distributed-Chat-application-using-Java-RMI

Project Spesfications:
The project is divided into two parts main parts and a bonus feature which was implemented using Java and java RMI to create a distributed chat application.

Phase I:

• Implement a simple distributed chat application using Java RMI.
• The system should have at least three chat nodes in a chat room where each node
can send and receive chat messages from other nodes (each node works as a client
and a server at the same time).
• The system should maintain consistency of events (message sending and receiving)
among nodes using Totally Ordered Multicasting illustrated as follows:
o Each node maintains a local logical clock value.
o When a node wants to send a message, it updates its logical clock and
multicasts the message (timestamped with its logical clock value) to all other
nodes. Therefore, the nodes information (hostnames, ports, and service
names) must be stored on every node.
o Upon receiving a message, the recipient puts it in a queue ordered by
requests’ timestamp (a tie breaker will be needed for requests with the same
timestamp e.g. node_Id), then it updates its local clock value by adding one
to the maximum of its local clock value and the clock value timestamped on
the message. Finally, it multicasts an ACK to other nodes (ACKs are not
queued).
o A message is polled from the messages queue and displayed only when it has
been ACKed by all nodes.
o Assumptions:
▪ Multicasts also go to sender.
▪ No messages are lost (all messages are eventually delivered).
▪ Failure and restart are not considered (A crashed node will cause
everything to stop).

Phase II:

• Implement a simple backup service. The backup service consists of the following:
o Two backup servers (primary, secondary).
o Four or more replication nodes.
• The chat nodes periodically send their locally saved chat messages to the primary
backup server.
• The backup server selects three (replica factor 3) replication nodes (based on some
policy e.g. round robin), then it broadcasts the chat node’s messages to the
replication nodes to be stored.
• When the chat node starts, it loads the previously sent messages from local storage,
if it does not exist, the chat node sends a request to the backup server to download
its backup of chat message.
• The backup server keeps a record for each chat node with its replication nodes, and
it retrieves the messages from these replication nodes and sends the messages back
to the chat node.
• The primary backup server keeps the secondary backup server updated with its
records of replication nodes.
• A heartbeat process is followed wherein all chat nodes keep pinging the primary
server and upon primary failure, the chat nodes start sending their requests to the
secondary server instead.

Bonus feature:

• Make two or more additional secondary backup server and when the
primary server fails, initiate a primary (leader) election algorithm (e.g. Bully
Algorithm, Ring Algorithm) to select the new primary (leader) server from the three
(or more) secondary servers.
