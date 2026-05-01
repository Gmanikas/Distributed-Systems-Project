package shared.models;

public class GameRating {

    private String gameName;
    private String playerId;
    private int rating;

    public GameRating(String playerId, String gameName, int rating) {
        this.gameName = gameName;
        this.rating = rating;
        this.playerId=playerId;
    }

    public String getGameName() {
        return this.gameName;
    }
    
    public int getRating() {
        return this.rating;
    }

    public String getPlayerId() {
        return this.playerId;
    }

}