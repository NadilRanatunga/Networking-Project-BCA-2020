import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class ChatClient {
  private static Socket socket;
  private static ObjectInputStream messageIn;
  private static ObjectOutputStream messageOut;
  public static ArrayList<String> clientNames;
  public static String name = "";
  public static boolean ready = false;

  public static void main(String[] args) throws Exception {
    Scanner userInput = new Scanner(System.in);
    clientNames = new ArrayList<String>();

    System.out.println("What's the server IP? ");
    String serverip = userInput.nextLine();
    System.out.println("What's the server port? ");
    int port = userInput.nextInt();
    userInput.nextLine();
    
    socket = new Socket(serverip, port);
    messageIn = new ObjectInputStream(socket.getInputStream());
    messageOut = new ObjectOutputStream(socket.getOutputStream());

    // start a thread to listen for server messages
    ClientServerHandler listener = new ClientServerHandler(messageIn);
    Thread t = new Thread(listener);
    t.start();
    
    String line = userInput.nextLine().trim();
    while(!line.toLowerCase().startsWith("/quit")) {
      int header = Message.MSG_HDR_CHAT;
      ArrayList<String> targets = new ArrayList<String>();

      if (!ready) {
        header = Message.MSG_HDR_NAME;
        name = line.trim();
      }
      else if (line.toLowerCase().startsWith("/whoishere")) {
        System.out.printf("Currently connected clients: ");
        for (String s : clientNames) {
          System.out.printf("%s, ", s);
        }
        System.out.println();
        line = userInput.nextLine().trim();
        continue;
      }
      else if (line.startsWith("@")) {
        String[] sections = line.split(" ");
        for (int i = 0; i < sections.length; i++) {
          if (sections[i].trim().startsWith("@")) {
            targets.add(sections[i].substring(1).trim());
          }
          else {
            line = String.join(" ", Arrays.copyOfRange(sections, i, sections.length));
            break;
          }
        }

        header = Message.MSG_HDR_PCHAT;
      }
      else if (line.toLowerCase().startsWith("/rps")) {
        String details = line.substring(4).trim();
        int delimiter = details.indexOf(" ");
        targets.add(details.substring(0, delimiter).trim());
        line = details.substring(delimiter).trim();

        header = Message.MSG_HDR_RPS;
      }
      
      Message msg = new Message(header, targets, name, line);
      messageOut.writeObject(msg);
      messageOut.flush();

      line = userInput.nextLine().trim();
    }
    
    messageOut.writeObject(new Message(Message.MSG_HDR_QUIT, null, name, ""));
    messageOut.flush();
    messageOut.close();

    userInput.close();

    messageIn.close();

    socket.close();   
  }
}
