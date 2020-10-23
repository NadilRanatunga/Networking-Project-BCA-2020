import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerClientHandler implements Runnable {
  // Maintain data about the client serviced by this thread
  private ClientConnectionData client;
  private ArrayList<ClientConnectionData> clientList;
  private static ArrayList<RPSRequest> activeRequests = new ArrayList<RPSRequest>();
  private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9_]+$");

  public ServerClientHandler(ClientConnectionData client, ArrayList<ClientConnectionData> clientList) {
      this.client = client;
      this.clientList = clientList;
  }

/*
 Broadcasts a message to all clients connected to the server.
*/
  public void broadcast(Message msg) {
      try {
          System.out.println("Broadcasting -- " + msg.getContent());
          synchronized (clientList) {
              for (ClientConnectionData c : clientList){
                  c.getOut().writeObject(msg);
                  c.getOut().flush();
              }
          }
      } catch (Exception ex) {
          System.out.println("broadcast caught exception: " + ex);
          ex.printStackTrace();
      }
  }

/*
Sends a message to only select clients
*/
  public void send(Message msg, ArrayList<String> targets) {
    for (String recipientName : targets) {
      ClientConnectionData recipient = null;
      synchronized (clientList) {
        for (ClientConnectionData c : clientList) {
          if (c.getUserName().equals(recipientName)) {
            recipient = c;
            break;
          }
        }
      }
    if(recipient == null) {
      return;
    }
    try {
      System.out.println("Sending -- " + msg.getContent() + " -- to : " + recipient.getUserName());
      recipient.getOut().writeObject(msg);
      recipient.getOut().flush();
    } catch (Exception ex) {
      System.out.println("send caught exception: " + ex);
      ex.printStackTrace();
    }
    }
    
  }

  public boolean validate(String name) {
    synchronized (clientList) {
      for (ClientConnectionData c : clientList) {
        if (c.getUserName() != null && c.getUserName().equalsIgnoreCase(name)) {
          return false;
        }
      }
    }
    
    Matcher matcher = pattern.matcher(name);
    return matcher.find();
  }

  @Override
  public void run() {
      try {
          ObjectInputStream in = client.getInput();
          ObjectOutputStream out = client.getOut();
          
          String userName = "";
          while(!validate(userName)) {
            out.writeObject(new Message(Message.MSG_HDR_SUBMITNAME, null, null, null));
            out.flush();
            
            Message requested = ((Message)in.readObject());
            userName = requested.getContent();
          }
          client.setUserName(userName);
          ArrayList<String> clientNames = new ArrayList<String>();
          for(ClientConnectionData c : clientList) {
            clientNames.add(c.getUserName());
          }
          //notify all that client has joined
          broadcast(new Message(Message.MSG_HDR_WELCOME, clientNames, userName, null));
          
          Message incoming = null;

          while( (incoming = (Message)in.readObject()) != null) {
              if (incoming.getHeader() == Message.MSG_HDR_CHAT) {
                String chat = incoming.getContent();
                if (chat.length() > 0) {
                  broadcast(new Message(Message.MSG_HDR_CHAT, null, incoming.getSender(), chat));    
                }
              }
              else if (incoming.getHeader() == Message.MSG_HDR_PCHAT) {
                Message msg = new Message(Message.MSG_HDR_CHAT, null, "(PRIVATE) " + incoming.getSender(), incoming.getContent());
                send(msg, incoming.getTargets());
              }
              else if (incoming.getHeader() == Message.MSG_HDR_RPS) {
                ArrayList<String> targets = incoming.getTargets();
                String choice = incoming.getContent();
                System.out.println(choice);
                if ((!choice.equalsIgnoreCase("R") && !choice.equalsIgnoreCase("P") && !choice.equalsIgnoreCase("S")) || targets.isEmpty()) {
                  targets.clear();
                  targets.add(incoming.getSender());
                  send(new Message(Message.MSG_HDR_RPSRESULT, null, "", "The correct format for /rps is : \"/rps [name] [R|P|S]\""), targets);
                  continue;
                }
                System.out.printf("Making a request between %s and %s with a choice of %s\n", incoming.getSender(), targets.get(0), choice);
                synchronized (activeRequests) {
                  RPSRequest counterpart = null;
                  for(int i = 0; i < activeRequests.size(); i++) {
                    if (activeRequests.get(i).isCounterpart(incoming.getSender(), targets.get(0))) {
                      counterpart = activeRequests.get(i);
                      activeRequests.remove(i);
                      break;
                    }
                  }
                  if (counterpart != null) {
                    String outcome = counterpart.targetChose(choice);
                    broadcast(new Message(Message.MSG_HDR_RPSRESULT, null, null, outcome));
                  }
                  else {
                    Message msg = new Message(Message.MSG_HDR_RPS, null, incoming.getSender(), "");
  
                    activeRequests.add(new RPSRequest(incoming.getSender(), targets.get(0), choice));
                    send(msg, targets);
                  }
                }
              }
              else if (incoming.getHeader() == Message.MSG_HDR_QUIT) {
                break;
              }
          }
      } catch (Exception ex) {
          if (ex instanceof SocketException) {
              System.out.println("Caught socket ex for " + 
                  client.getName());
          } else {
              System.out.println(ex);
              ex.printStackTrace();
          }
      } finally {
          //Remove client from clientList, notify all
          synchronized (clientList) {
              clientList.remove(client); 
          }
          System.out.println(client.getName() + " has left.");
          broadcast(new Message(Message.MSG_HDR_EXIT, null, client.getUserName(), ""));
          try {
              client.getSocket().close();
          } catch (IOException ex) {}

      }
  }
  
}
