package shared.models;

public class UpdateRisk { // UPDATE_RISK | {"gameName":"...", "newRisk":"..."} τέτοια μορφή πρέπει να έχει η είσοδος από το Master

    private String gameName;
    private String newRisk;

    public UpdateRisk (String gameName, String newRisk) {
        this.gameName = gameName;
        this.newRisk = newRisk;
    }
 
    public String getGameName() {
        return gameName;
    }

    public String getNewRisk() {
        return newRisk;
    }

}

    