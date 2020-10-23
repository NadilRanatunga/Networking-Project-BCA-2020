import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
/**
 * For Java 8, javafx is installed with the JRE. You can run this program normally.
 * For Java 9+, you must install JavaFX separately: https://openjfx.io/openjfx-docs/
 * If you set up an environment variable called PATH_TO_FX where JavaFX is installed
 * you can compile this program with:
 *  Mac/Linux:
 *      > javac --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows CMD:
 *      > javac --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows Powershell:
 *      > javac --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 * 
 * Then, run with:
 * 
 *  Mac/Linux:
 *      > java --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient 
 *  Windows CMD:
 *      > java --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui.ChatGuiClient
 *  Windows Powershell:
 *      > java --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient
 * 
 * There are ways to add JavaFX to your to your IDE so the compile and run process is streamlined.
 * That process is a little messy for VSCode; it is easiest to do it via the command line there.
 * However, you should open  Explorer -> Java Projects and add to Referenced Libraries the javafx .jar files 
 * to have the syntax coloring and autocomplete work for JavaFX 
 */

class ServerInfo {
  public final String serverAddress;
  public final int serverPort;
  public ServerInfo(String serverAddress, int serverPort) {
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;
  }
}

public class ChatGuiClient extends Application {
  private Socket socket;
  private ObjectInputStream messageIn;
  private ObjectOutputStream messageOut;
  
  private Stage stage;
  private TextArea messageArea;
  private TextArea userArea;
  private TextField textInput;
  private Button sendButton;
  private Button rpsButton;
  private ServerInfo serverInfo;
  //volatile keyword makes individual reads/writes of the variable atomic
  // Since username is accessed from multiple threads, atomicity is important 
  private volatile String username = "";
  public static ArrayList<String> clientNames = null;
  public static void main(String[] args) {
      launch(args);
  }
  @Override
  public void start(Stage primaryStage) throws Exception {
    //If ip and port provided as command line arguments, use them
    List<String> args = getParameters().getUnnamed();
    if (args.size() == 2){
      this.serverInfo = new ServerInfo(args.get(0), Integer.parseInt(args.get(1)));
    }
    else {
      //otherwise, use a Dialog.
      Optional<ServerInfo> info = getServerIpAndPort();
      if (info.isPresent()) {
          this.serverInfo = info.get();
      } 
      else{
          Platform.exit();
          return;
      }
    }
    this.stage = primaryStage;
    BorderPane borderPane = new BorderPane();
    userArea = new TextArea();
    userArea.setWrapText(false);
    userArea.setEditable(false);
    userArea.setPrefWidth(100);
    borderPane.setRight(userArea);

    messageArea = new TextArea();
    messageArea.setWrapText(true);
    messageArea.setEditable(false);
    messageArea.setPrefWidth(300);
    borderPane.setCenter(messageArea);
    //At first, can't send messages - wait for WELCOME!
    textInput = new TextField();
    textInput.setEditable(false);
    textInput.setOnAction(e -> sendMessage());
    sendButton = new Button("Send");
    sendButton.setDisable(true);
    sendButton.setOnAction(e -> sendMessage());
    rpsButton = new Button("RPS");
    rpsButton.setDisable(true);
    rpsButton.setOnAction(e -> sendRandomRPS());
    HBox hbox = new HBox();
    hbox.getChildren().addAll(new Label("Message: "), textInput, sendButton, rpsButton);
    HBox.setHgrow(textInput, Priority.ALWAYS);
    borderPane.setBottom(hbox);
    Scene scene = new Scene(borderPane, 400, 500);
    stage.setTitle("Chat Client");
    stage.setScene(scene);
    stage.show();
    ServerListener socketListener = new ServerListener();
    
    //Handle GUI closed event
    stage.setOnCloseRequest(e -> {
      socketListener.appRunning = false;
      try {
        messageOut.writeObject(new Message(Message.MSG_HDR_QUIT, null, username, ""));
        messageOut.flush();
        messageOut.close();
        socket.close(); 
      } catch (IOException ex) {}
    });
    new Thread(socketListener).start();
  }
  private void sendRandomRPS() {
    Random rand = new Random();
    String target = "";
    if(clientNames.size() < 2)
      return;
    do {
      target = clientNames.get(rand.nextInt(clientNames.size()));
    } while (target.equals(username));
    String[] options = {"R", "P", "S"};
    String action = options[rand.nextInt(options.length)];

    ArrayList<String> targets = new ArrayList<String>();
    targets.add(target);
    
    Message msg = new Message(Message.MSG_HDR_RPS, targets, username, action);
    textInput.clear();
    try {
      messageOut.writeObject(msg);
      messageOut.flush();
    } catch (IOException ex) {}
    Platform.runLater(() -> {
      messageArea.appendText(String.format("You sent a random RPS request to %s and chose %s\n", targets.get(0), action));
    });
  }
  private void sendMessage() {
    String line = textInput.getText().trim();
    if (line.length() == 0) {
      return;
    }
    int header = Message.MSG_HDR_CHAT;
    ArrayList<String> targets = new ArrayList<String>();
    if (line.toLowerCase().startsWith("/whoishere")) {
      messageArea.appendText("Currently connected clients: ");
      for (String s : clientNames) {
        messageArea.appendText(String.format("%s ", s));
      }
      messageArea.appendText("\n");
      textInput.clear();
      return;
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
    
    Message msg = new Message(header, targets, username, line);
    textInput.clear();
    try {
      messageOut.writeObject(msg);
      messageOut.flush();
    } catch (IOException ex) {}
  }
  private Optional<ServerInfo> getServerIpAndPort() {
    // In a more polished product, we probably would have the ip /port hardcoded
    // But this a great way to demonstrate making a custom dialog
    // Based on Custom Login Dialog from https://code.makery.ch/blog/javafx-dialogs-official/
    // Create a custom dialog for server ip / port
    Dialog<ServerInfo> getServerDialog = new Dialog<>();
    getServerDialog.setTitle("Enter Server Info");
    getServerDialog.setHeaderText("Enter your server's IP address and port: ");
    // Set the button types.
    ButtonType connectButtonType = new ButtonType("Connect", ButtonData.OK_DONE);
    getServerDialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);
    // Create the ip and port labels and fields.
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));
    TextField ipAddress = new TextField();
    ipAddress.setPromptText("e.g. localhost, 127.0.0.1");
    grid.add(new Label("IP Address:"), 0, 0);
    grid.add(ipAddress, 1, 0);
    TextField port = new TextField();
    port.setPromptText("e.g. 54321");
    grid.add(new Label("Port number:"), 0, 1);
    grid.add(port, 1, 1);
    // Enable/Disable connect button depending on whether a address/port was entered.
    Node connectButton = getServerDialog.getDialogPane().lookupButton(connectButtonType);
    connectButton.setDisable(true);
    // Do some validation (using the Java 8 lambda syntax).
    ipAddress.textProperty().addListener((observable, oldValue, newValue) -> {
        connectButton.setDisable(newValue.trim().isEmpty());
    });
    port.textProperty().addListener((observable, oldValue, newValue) -> {
        // Only allow numeric values
        if (! newValue.matches("\\d*"))
            port.setText(newValue.replaceAll("[^\\d]", ""));
        connectButton.setDisable(newValue.trim().isEmpty());
    });
    getServerDialog.getDialogPane().setContent(grid);
    
    // Request focus on the username field by default.
    Platform.runLater(() -> ipAddress.requestFocus());
    // Convert the result to a ServerInfo object when the login button is clicked.
    getServerDialog.setResultConverter(dialogButton -> {
        if (dialogButton == connectButtonType) {
            return new ServerInfo(ipAddress.getText(), Integer.parseInt(port.getText()));
        }
        return null;
    });
    return getServerDialog.showAndWait();
  }
  private String getName(){
    username = "";
    TextInputDialog nameDialog = new TextInputDialog();
    nameDialog.setTitle("Enter Chat Name");
    nameDialog.setHeaderText("Please enter your username.");
    nameDialog.setContentText("Name: ");
    
    while(username.equals("")) {
      Optional<String> name = nameDialog.showAndWait();
      if (!name.isPresent() || name.get().trim().equals(""))
        nameDialog.setHeaderText("You must enter a nonempty name: ");
      else if (name.get().trim().contains(" "))
        nameDialog.setHeaderText("The name must have no spaces: ");
      else
        username = name.get().trim();            
    }
    return username;
  }
  class ServerListener implements Runnable {
    volatile boolean appRunning = false;
    public void run() {
      try {
        // Set up the socket for the Gui
        socket = new Socket(serverInfo.serverAddress, serverInfo.serverPort);
        messageIn = new ObjectInputStream(socket.getInputStream());
        messageOut = new ObjectOutputStream(socket.getOutputStream());
        
        appRunning = true;
        //handle all kinds of incoming messages
        Message incoming = null;
        while (appRunning && ((incoming = (Message)messageIn.readObject()) != null)) {
          if (incoming.getHeader() == Message.MSG_HDR_SUBMITNAME) {
            Platform.runLater(() -> {
              try {
                messageOut.writeObject(new Message(Message.MSG_HDR_NAME, null, "", getName()));
              } catch (IOException ex) {}
            });
          } else if (incoming.getHeader() == Message.MSG_HDR_WELCOME) {
            ChatGuiClient.clientNames = incoming.getTargets();
            String user = incoming.getSender();
            if (user.equals(username)) {
              userArea.clear();
              userArea.appendText("Online Users:\n");
              for(String s : ChatGuiClient.clientNames) {
                userArea.appendText(s + "\n");
              }
              Platform.runLater(() -> {
                stage.setTitle("Chatter - " + username);
                textInput.setEditable(true);
                sendButton.setDisable(false);
                rpsButton.setDisable(false);
                messageArea.appendText("Welcome to the chatroom, " + username + "!\n");
              });
            }
            else {
              Platform.runLater(() -> {
                userArea.clear();
                userArea.appendText("Online Users:\n");
                for(String s : ChatGuiClient.clientNames) {
                  userArea.appendText(s + "\n");
                }
                messageArea.appendText(user + " has joined the chatroom.\n");
              });
            }
          } else if (incoming.getHeader() == Message.MSG_HDR_CHAT) {
            String sender = incoming.getSender();
            String content = incoming.getContent();
            Platform.runLater(() -> {
              messageArea.appendText(sender + ": " + content + "\n");
            });
          } else if (incoming.getHeader() == Message.MSG_HDR_EXIT) {
            ChatGuiClient.clientNames = incoming.getTargets();
            String sender = incoming.getSender();
            Platform.runLater(() -> {
              userArea.clear();
              userArea.appendText("Online Users:\n");
              for(String s : ChatGuiClient.clientNames) {
                userArea.appendText(s + "\n");
              }
              messageArea.appendText(sender + " has left the chatroom.\n");
            });
          } else if (incoming.getHeader() == Message.MSG_HDR_RPS) {
            String name = incoming.getSender();
            Platform.runLater(() -> {
              messageArea.appendText(String.format("%s wants to play you in Rock, Paper, Scissors! To accept, type \"/rps %s [R|P|S]\"\n", name, name));
            });
          } else if (incoming.getHeader() == Message.MSG_HDR_RPSRESULT) {
            String result = incoming.getContent();
            Platform.runLater(() -> {
              messageArea.appendText(result + "\n");
            });
          }
      }
      } catch (UnknownHostException e) {
          e.printStackTrace();
      } catch (Exception e) {
          if (appRunning)
              e.printStackTrace();
      } 
      finally {
          Platform.runLater(() -> {
              stage.close();
          });
          try {
              if (socket != null)
                  socket.close();
          }
          catch (IOException e){
          }
      }
    }
  }
}