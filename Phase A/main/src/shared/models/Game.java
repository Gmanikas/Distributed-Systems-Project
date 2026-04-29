package shared.models;

public class Game {

    // Πρέπει να αντιστοιχούν σε αυτές του παραδείγματος του json αρχείου, ώστε να γίνεται σωστά η αρχικοποίηση
    private String name;
    private String provider;
    private int stars;
    private int voteNo;
    private String logoPath;
    private double minBet;
    private double maxBet;
    private String riskLevel;
    private String hashKey;

    private String betCategory;
    private int jackpot;

    private boolean active = false;

    public Game(String name, String provider, int stars, 
        int voteNo, String logoPath, double minBet, 
        double maxBet, String riskLevel, String hashKey) {

            this.name = name;
            this.provider = provider;
            this.stars = stars;
            this.voteNo = voteNo;
            this.logoPath = logoPath;
            this.minBet = minBet;
            this.maxBet = maxBet;
            this.riskLevel = riskLevel;
            this.hashKey = hashKey;
        }

    public String getName() {
        return this.name;
    }

    public String getProvider() {
        return this.provider;
    }
    
    public int getStars() {
        return this.stars;
    }
    
    public int getVotes() {
        return this.voteNo;
    }

    public String getLogoPath() {
        return this.logoPath;
    }
    
    public double getMinBet() {
        return this.minBet;
    }
    
    public double getMaxBet() {
        return this.maxBet;
    }
    
    public String getRisk() {
        return this.riskLevel;
    }

    public void setRisk(String newRisk) {
        this.riskLevel = newRisk;
    }

    public String getHashKey() {
        return this.hashKey;
    }

    public String getBetCategory() {
        return this.betCategory;
    }
    
    public int getJackpot() { 
        return this.jackpot;
    }

    public boolean getStatus() {
        return this.active;
    }

    public void setStatus(boolean bool) {
        this.active = bool;
    }

    
    public void calcAutoFields(){

        if (minBet == 0.1) this.betCategory = "$";
        else if (minBet == 1) this.betCategory = "$$";
        else if (minBet == 5) this.betCategory = "$$$";

        if (riskLevel.equalsIgnoreCase("LOW")) this.jackpot = 10;
        else if (riskLevel.equalsIgnoreCase("MEDIUM")) this.jackpot = 20;
        else if (riskLevel.equalsIgnoreCase("HIGH")) this.jackpot = 50;

    }


}