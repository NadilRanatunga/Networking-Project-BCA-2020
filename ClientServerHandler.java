import java.io.BufferedReader;

public class ClientServerHandler implements Runnable {
  private BufferedReader socketIn;

  public ClientServerHandler(BufferedReader socketIn) {
    this.socketIn = socketIn;
  }

  @Override
  public void run() {
    try {
      String incoming = "";

      while( (incoming = socketIn.readLine()) != null) {
        //handle different headers
        //EXIT
        if(incoming.startsWith("SUBMITNAME")) {
          System.out.print("Joined! Enter a name: ");
        }
        else if(incoming.startsWith("WELCOME")) {
          if(!ChatClient.ready) {
            ChatClient.ready = true;
            System.out.println("Accepted. Welcome to the chat!");
          }
          else {
            String name = incoming.substring(7).trim();
            System.out.printf("Say hello to %s!\n", name);
          }
        }
        else if(incoming.startsWith("CHAT")) {
          String details = incoming.substring(4).trim();
          int delimiter = details.indexOf(" ");
          String sender = details.substring(0, delimiter).trim();
          String contents = details.substring(delimiter).trim();
          if(!sender.equals(ChatClient.name)) {
            System.out.printf("%s : %s\n", sender, contents);
          }
        }
        else {
          System.out.println(incoming);
        }
      }
    }
    catch (Exception ex) {
      if(ex.getMessage().equals("Socket closed"))
        System.out.println("Disconnected");
      else
        System.out.println("Exception caught in listener - " + ex);
    }
  }
}
