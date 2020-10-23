import java.io.ObjectInputStream;

public class ClientServerHandler implements Runnable {
  private ObjectInputStream messageIn;

  public ClientServerHandler(ObjectInputStream messageIn) {
    this.messageIn = messageIn;
  }

  @Override
  public void run() {
    try {
      Message incoming = null;

      while( (incoming = (Message)messageIn.readObject()) != null) {
        if(incoming.getHeader() == Message.MSG_HDR_CHAT) {
          String sender = incoming.getSender();
          if(!sender.equals(ChatClient.name)) {
            System.out.printf("%s : %s\n", sender, incoming.getContent());
          }
        }
        else if(incoming.getHeader() == Message.MSG_HDR_SUBMITNAME) {
          System.out.print("Joined! Enter a name: ");
        }
        else if(incoming.getHeader() == Message.MSG_HDR_WELCOME) {
          ChatClient.clientNames = incoming.getTargets();
          if(!ChatClient.ready) {
            ChatClient.ready = true;
            System.out.println("Accepted. Welcome to the chat!");
            
            System.out.printf("Currently connected clients: ");
            for (String s : ChatClient.clientNames) {
              System.out.printf("%s ", s);
            }
            System.out.println();
          }
          else {
            String name = incoming.getSender();
            System.out.printf("Say hello to %s!\n", name);
          }
        }
        else if(incoming.getHeader() == Message.MSG_HDR_EXIT) {
          String name = incoming.getSender();
          System.out.printf("%s disconnected\n", name);
        }
        else if(incoming.getHeader() == Message.MSG_HDR_RPSRESULT) {
          String result = incoming.getContent();
          System.out.println(result);
        }
        else if(incoming.getHeader() == Message.MSG_HDR_RPS) {
          String name = incoming.getSender();
          System.out.printf("%s wants to play you in Rock, Paper, Scissors! To accept, type \"/rps %s [R|P|S]\"\n", name, name);
        }
        else {
          System.out.printf("Undefined or poorly formatted header on message : %s", incoming.getContent());
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
