import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable{
  private static final long serialVersionUID = 4L; //🔪
  
  public static final int MSG_HDR_CHAT = 0;
  public static final int MSG_HDR_NAME = 1;
  public static final int MSG_HDR_SUBMITNAME = 2;
  public static final int MSG_HDR_WELCOME = 3;
  public static final int MSG_HDR_EXIT = 4;
  public static final int MSG_HDR_PCHAT = 5;
  public static final int MSG_HDR_RPS = 6;
  public static final int MSG_HDR_RPSRESULT = 7;
  public static final int MSG_HDR_QUIT = 8;

  private int header;
  private ArrayList<String> targets;
  private String content;
  private String sender;

  public Message(int header, ArrayList<String> targets, String sender, String content) {
    this.header = header;
    this.targets = targets;
    this.content = content;
    this.sender = sender;
  }

  public int getHeader() {
    return header;
  }
  public ArrayList<String> getTargets() {
    return targets;
  }
  public String getContent() {
    return content;
  }
  public String getSender() {
    return sender;
  }
}
