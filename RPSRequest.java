public class RPSRequest {
  private String sender;
  private String senderChoice;
  private String target;

  public RPSRequest(String sender, String target, String senderChoice) {
    this.sender = sender;
    this.target = target;
    this.senderChoice = senderChoice;
  }

  public boolean isCounterpart(String checkSender, String checkTarget) {
    return (sender.equals(checkTarget) && target.equals(checkSender));
  }

  public String targetChose(String choice) {
    if(senderChoice.equalsIgnoreCase("R")) {
      if(choice.equalsIgnoreCase("R")) {
        return String.format("%s and %s just smashed Rocks together, and tied in Rock, Paper, Scissors!", sender, target);
      }
      else if(choice.equalsIgnoreCase("P")) {
        return String.format("%s just got their rock covered by %s\'s paper, in Rock, Paper, Scissors!", sender, target);
      }
      else if(choice.equalsIgnoreCase("S")) {
        return String.format("%s just demolished %s\'s scissors with a rock, in Rock, Paper, Scissors!", sender, target);
      }
    }
    else if(senderChoice.equalsIgnoreCase("P")) {
      if(choice.equalsIgnoreCase("R")) {
        return String.format("%s just smothered %s\'s rock with paper, in Rock, Paper, Scissors!", sender, target);
      }
      else if(choice.equalsIgnoreCase("P")) {
        return String.format("%s and %s both brought paper to battle, and tied in Rock, Paper, Scissors!", sender, target);
      }
      else if(choice.equalsIgnoreCase("S")) {
        return String.format("%s\'s paper just got all sliced up by %s\'s scissors, in Rock, Paper, Scissors!", sender, target);
      }
    }
    else {
      if(choice.equalsIgnoreCase("R")) {
        return String.format("%s just got his scissors smashed by %s\'s rock, in Rock, Paper, Scissors!", sender, target);
      }
      else if(choice.equalsIgnoreCase("P")) {
        return String.format("%s just sliced up %s\'s paper, in Rock, Paper, Scissors!", sender, target);
      }
      else if(choice.equalsIgnoreCase("S")) {
        return String.format("%s and %s just jabbed eachother with scissors, and tied in Rock, Paper, Scissors!", sender, target);
      }
    }
    return "Uh oh!";
  }

  public String getSender() {
    return sender;
  }
  public String getTarget() {
    return target;
  }
}
