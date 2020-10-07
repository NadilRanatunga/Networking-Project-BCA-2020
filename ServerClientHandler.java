import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
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
  public void broadcast(String msg) {
      try {
          System.out.println("Broadcasting -- " + msg);
          synchronized (clientList) {
              for (ClientConnectionData c : clientList){
                  c.getOut().println(msg);
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
  public void send(String msg, String recipientName) {
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
      System.out.println("Sending -- " + msg + " -- to : " + recipient.getUserName());
      recipient.getOut().println(msg);
    } catch (Exception ex) {
      System.out.println("send caught exception: " + ex);
      ex.printStackTrace();
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
          BufferedReader in = client.getInput();
          PrintWriter out = client.getOut();
          
          String userName = "";
          while(!validate(userName)) {
            out.println("SUBMITNAME");
            
            String requested = in.readLine();
            if(requested.startsWith("NAME")) {
              userName = requested.substring(4).trim();    
            }
          }
          client.setUserName(userName);
          //notify all that client has joined
          broadcast(String.format("WELCOME %s", client.getUserName()));
          
          String incoming = "";

          while( (incoming = in.readLine()) != null) {
              if (incoming.startsWith("CHAT")) {
                String chat = incoming.substring(4).trim();
                if (chat.length() > 0) {
                  String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                  broadcast(msg);    
                }
              }
              else if (incoming.startsWith("PCHAT")) {
                String details = incoming.substring(5).trim();
                int delimiter = details.indexOf(" ");
                String target = details.substring(0, delimiter).trim();
                String message = String.format("CHAT %s %s", "(PRIVATE)" + client.getUserName(), details.substring(delimiter));
                send(message, target);
              }
              else if (incoming.startsWith("RPS")) {
                String details = incoming.substring(4).trim();
                int delimiter = details.indexOf(" ");
                String sender = client.getUserName();
                String target = details.substring(0, delimiter).trim();
                String choice = details.substring(delimiter).trim();
                if (!choice.equalsIgnoreCase("R") && !choice.equalsIgnoreCase("P") && !choice.equalsIgnoreCase("S")) {
                  send("RPSRESULT The correct format for /rps is : \"/rps [name] [R|P|S]\"", client.getUserName());
                  continue;
                }
                System.out.printf("Making a request between %s and %s with a choice of %s\n", sender, target, choice);
                synchronized (activeRequests) {
                  RPSRequest counterpart = null;
                  for(int i = 0; i < activeRequests.size(); i++) {
                    if (activeRequests.get(i).isCounterpart(sender, target)) {
                      counterpart = activeRequests.get(i);
                      activeRequests.remove(i);
                      break;
                    }
                  }
                  if (counterpart != null) {
                    String outcome = counterpart.targetChose(choice);
                    broadcast(String.format("RPSRESULT %s", outcome));
                  }
                  else {
                    String message = String.format("RPS %s", sender);
  
                    activeRequests.add(new RPSRequest(sender, target, choice));
                    send(message, target);
                  }
                }
              }
              else if (incoming.startsWith("QUIT")) {
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
          broadcast(String.format("EXIT %s", client.getUserName()));
          try {
              client.getSocket().close();
          } catch (IOException ex) {}

      }
  }
  
}
