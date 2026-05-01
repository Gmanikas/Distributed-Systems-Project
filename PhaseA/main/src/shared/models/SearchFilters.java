package shared.models;

public class SearchFilters {
    
    private int minStars;
    private String riskLevel;
    private String betCategory;
    private String providerName = null;
    
    public SearchFilters(int minStars, String riskLevel, String betCategory) {
        this.minStars = minStars;
        this.riskLevel = riskLevel;
        this.betCategory = betCategory;
    }

    public SearchFilters(String providerName) {
        this.providerName = providerName;
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

    public String getProviderName() {
        return providerName;
    }

}
