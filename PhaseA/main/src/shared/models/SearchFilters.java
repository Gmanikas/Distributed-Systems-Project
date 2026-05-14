package shared.models;

public class SearchFilters {
    
    private int minStars;
    private String riskLevel;
    private String betCategory;
    private String gameName = null;
    private String providerName = null;
    
    public SearchFilters(int minStars, String riskLevel, String betCategory) {
        this.minStars = minStars;
        this.riskLevel = riskLevel;
        this.betCategory = betCategory;
    }

    public SearchFilters(String name, boolean isGameName) { // Bazoume to boolean, wste na xexwrizei apo to providerName search
        if (isGameName) {
            this.gameName = name;
        } else {
            this.providerName = name;
        }
    }

    
    public int getMinStars() {
        return minStars;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getBetCategory() {
        return betCategory;
    }

    public String getGameName() {
        return gameName;
    }

    public String getProviderName() {
        return providerName;
    }

}
