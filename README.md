# BCCA Client / Server
By Aidan Parkhurst, Nadil Ranatunga, and Marcus San Antonio

This is a simple chat application, with a console client, gui client, and a server

### Protocol (Message.java)
This chat application sends serialized objects of the following form:

```json
Message {
  header : int,
  targets : ArrayList<String>,
  content : string,
  sender : string
}
```

The header will be equal to one of the following:

```js
MSG_HDR_CHAT = 0;
MSG_HDR_NAME = 1;
MSG_HDR_SUBMITNAME = 2;
MSG_HDR_WELCOME = 3;
MSG_HDR_EXIT = 4;
MSG_HDR_PCHAT = 5;
MSG_HDR_RPS = 6;
MSG_HDR_RPSRESULT = 7;
MSG_HDR_QUIT = 8;
```

On connection, the server will send a `MSG_HDR_SUBMITNAME`, and will continue to do so until the client responds with a `MSG_HDR_NAME` that has a valid name in content.

The target field only need be non-null for `MSG_HDR_RPS` and `MSG_HDR_PCHAT` messages.

On disconnection, the client must send a `MSG_HDR_QUIT` message.

When the server sends a `MSG_HDR_WELCOME` or `MSG_HDR_EXIT`, the most recent list of clients will be in the targets.

### Server (ChatServer.java, ServerClientHandler.java, RPSRequest.java)

By default, the server will be hosted on the machine's local IP and port `1111`
To change the server's port, edit line #13 of ChatServer.java:

`public static final int PORT = 0000;`

### Console Client (ChatClient.java & ClientServerHandler.java)

On launch the client will prompt the user to enter an IP and port of a BCCA server.

Once a connection has been established, the client provides a name that will identify them to every other client on the server.

Typing messages and pressing enter from this point on will send a message to all the other clients in the server.

There are a few predefined commands, messages that can be sent with special functionality:

- PCHAT : Sends a private message to specified clients in the server, usage: `@[clientname] ... @[otherclients] [message]`

- RPS : Sends a Rock, Paper, Scissors challenge to a specified client in the server, usage: `/rps [clientname] [R|P|S]`

  If a user receives an RPS request, they can respond to the challenge using RPS as well, usage: `/rps [challengername] [R|P|S]`

- QUIT : Disconnects a client from the server, sending a message to every other client, usage: `/quit`

- WHOISHERE : Displays a list of every client in the server, usage: `/whoishere`

### GUI Client (ChatGuiClient.java)

On launch the client will prompt the user to enter an IP and port of a BCCA server. Alternatively, these can be specified in the order `IP PORT` when running from CMD.

The client will then prompt the user to enter a name. If the name is empty, or the server rejects the name, this prompt will reappear

The GUI now will be usable, and messages can be sent from the field at the bottom, being displayed in the chat section above.

On the right is a list of other clients connected to the server, updated live as the list changes.

Next to the `Send` button is an `RPS` button. This button will send a random Rock, Paper, Scissors request to someone in the server.

All the aforementioned commands from the Console Client function identically here, with the exception of `/quit`.