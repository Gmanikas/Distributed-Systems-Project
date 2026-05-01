package shared.models;

public class Play {
    
    private String id;
    private String gameName;
    private double bet;

    public Play(String id, String name, double bet) {
        this.id = id;
        this.gameName = name;
        this.bet = bet;
    }

    public String getPlayerId() {
        return id;
    }

    public String getGameName() {
        return gameName;
    }

    public double getBet() {
        return bet;
    }


}
