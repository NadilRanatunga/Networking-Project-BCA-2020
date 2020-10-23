import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ChatServer {
  public static final int PORT = 1111;
  private static final ArrayList<ClientConnectionData> clientList = new ArrayList<>();
  public static void main(String[] args) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(100);
    try (ServerSocket serverSocket = new ServerSocket(PORT)){
      System.out.println("Chat Server started.");
      System.out.println("Local IP: "
              + Inet4Address.getLocalHost().getHostAddress());
      System.out.println("Local Port: " + serverSocket.getLocalPort());
  
      while (true) {
        try {
            Socket socket = serverSocket.accept();
            System.out.printf("Connected to %s:%d on local port %d\n",
                socket.getInetAddress(), socket.getPort(), socket.getLocalPort());
            
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Got both object streams");
            String name = socket.getInetAddress().getHostName();
            ClientConnectionData client = new ClientConnectionData(socket, in, out, name);
            System.out.println("Created ClientConnectionData");
            synchronized (clientList) {
                clientList.add(client);
            }
            
            System.out.println("added client " + name);
            //handle client business in another thread
            Runnable r = new ServerClientHandler(client, clientList);
            pool.execute(r);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
      }
    }
  }
}