package wifilocation.background.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity(tableName = "fingerprint")
@Getter
@Setter
public class EstimatedResult {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @SerializedName("building")
    private String building;

    @SerializedName("SSID")
    private String ssid;

    @SerializedName("pos_x")
    private Double pos_x;

    @SerializedName("pos_y")
    private Double pos_y;

    @SerializedName("est_x")
    private Double est_x;

    @SerializedName("est_y")
    private Double est_y;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("method")
    private String method;

    @SerializedName("k")
    private Integer k;

    @SerializedName("threshold")
    private Integer threshold;

    @SerializedName("algorithmVersion")
    private Integer algorithmVersion;

    @SerializedName("date")
    private Date date;

    private Integer _new;
    
    // PositioningAlgorithm에서만 사용함
    @Ignore
    public EstimatedResult(String building, String ssid, String uuid, String method, Integer k, Integer threshold, Integer algorithmVersion, Integer _new) {
        this.pos_x = 0d;
        this.pos_y = 0d;
        this.est_x = 0d;
        this.est_y = 0d;
        this.building = building;
        this.ssid = ssid;
        this.uuid = uuid;
        this.method = method;
        this.k = k;
        this.threshold = threshold;
        this.algorithmVersion = algorithmVersion;
        this._new = _new;
        this.date = new Date(System.currentTimeMillis());
    }

    public EstimatedResult(String building, String ssid,
                           Double pos_x, Double pos_y, Double est_x, Double est_y,
                           String uuid, String method, Integer k, Integer threshold, Integer algorithmVersion, Integer _new) {
        this.building = building;
        this.ssid = ssid;
        this.pos_x = pos_x;
        this.pos_y = pos_y;
        this.est_x = est_x;
        this.est_y = est_y;
        this.uuid = uuid;
        this.method = method;
        this.k = k;
        this.threshold = threshold;
        this.algorithmVersion = algorithmVersion;
        this.date = new Date(System.currentTimeMillis());
        this._new = _new;
    }

    public EstimatedResult(String building, String ssid,
                           Double pos_x, Double pos_y, Double est_x, Double est_y,
                           String uuid, String method, Integer k, Integer threshold, Integer algorithmVersion, long date, Integer _new) {
        this.building = building;
        this.ssid = ssid;
        this.pos_x = pos_x;
        this.pos_y = pos_y;
        this.est_x = est_x;
        this.est_y = est_y;
        this.uuid = uuid;
        this.method = method;
        this.k = k;
        this.threshold = threshold;
        this.algorithmVersion = algorithmVersion;
        this.date = new Date(date);
        this._new = _new;
    }
}
