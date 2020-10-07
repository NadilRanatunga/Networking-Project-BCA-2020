# BCCA Client / Server
By Aidan Parkhurst, Nadil Ranatunga, and Marcus San Antonio

This is a simple chat application, with a client and a server

### Server (ChatServer.java, ServerClientHandler.java, RPSRequest.java)
The server has basic logging of what messages are sent and commands are run
To change the server's port, edit line #13 of ChatServer.java:
`public static final int PORT = 0000;`

### Client (ChatClient.java & ClientServerHandler.java)
The client has several features, on launch it will prompt the user to enter an IP and port of a BCCA server
Once a connection has been established, the client provides a name that will identify them to every other client on the server

Typing messages and pressing enter from this point on will send a message to all the other clients in the server

There are a few predefined commands, which start with a single forward slash (/) and have special functionality:

- PCHAT : Sends a private message to a specified client in the server, usage:
  `/pchat [clientname] [message]`

- RPS : Sends a Rock, Paper, Scissors challenge to a specified client in the server, usage:
  `/rps [clientname] [R|P|S]`
  If a user receives an RPS request, they can respond to the challenge using RPS as well, usage:
  `/rps [challengername] [R|P|S]`

- QUIT : Disconnects a client from the server, sending a message to every other client, usage:
  `/quit`
