package wifilocation.background.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import lombok.Getter;
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

    private StringBuilder estimateReason;

    public EstimatedResult(String building, String ssid, Double pos_x, Double pos_y, Double est_x, Double est_y, String uuid, String method, Integer k, Integer threshold, Integer algorithmVersion) {
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
    }

    public EstimatedResult(String building, String ssid, String uuid, String method, int k, int threshold, int algorithmVersion) {
        this.building = building;
        this.ssid = ssid;
        this.uuid = uuid;
        this.method = method;
        this.k = k;
        this.threshold = threshold;
        this.algorithmVersion = algorithmVersion;
    }

    public EstimatedResult(EstimatedResult estimatedResult) {
        this.building = estimatedResult.getBuilding();
        this.ssid = estimatedResult.getSsid();
        this.pos_x = estimatedResult.getPos_x();
        this.pos_y = estimatedResult.getPos_y();
        this.est_x = estimatedResult.getEst_x();
        this.est_y = estimatedResult.getEst_y();
        this.uuid = estimatedResult.getUuid();
        this.method = estimatedResult.getMethod();
        this.k = estimatedResult.getK();
        this.threshold = estimatedResult.getThreshold();
        this.algorithmVersion = estimatedResult.getAlgorithmVersion();
        this.date = estimatedResult.getDate();
        this.estimateReason = estimatedResult.getEstimateReason();
    }
}
