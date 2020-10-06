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
  private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9_]+$");

  public ServerClientHandler(ClientConnectionData client, ArrayList<ClientConnectionData> clientList) {
      this.client = client;
      this.clientList = clientList;
  }

  /**
* Broadcasts a message to all clients connected to the server.
*/
  public void broadcast(String msg) {
      try {
          System.out.println("Broadcasting -- " + msg);
          synchronized (clientList) {
              for (ClientConnectionData c : clientList){
                  c.getOut().println(msg);
                  // c.getOut().flush();
              }
          }
      } catch (Exception ex) {
          System.out.println("broadcast caught exception: " + ex);
          ex.printStackTrace();
      }
      
  }

  public boolean validate(String name) {
    System.out.println("Validating:" + name);
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
              } else if (incoming.startsWith("QUIT")){
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
