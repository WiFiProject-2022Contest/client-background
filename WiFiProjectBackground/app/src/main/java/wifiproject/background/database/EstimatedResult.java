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

    public EstimatedResult() {
        this.building = null;
        this.ssid = null;
        this.pos_x = 0d;
        this.pos_y = 0d;
        this.est_x = 0d;
        this.est_y = 0d;
        this.uuid = null;
        this.method = null;
        this.k = -1;
        this.threshold = -1;
        this.algorithmVersion = 0;
        this.date = new Date(System.currentTimeMillis());
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
    }

    public EstimatedResult(String building, String ssid, String uuid) {
        this();

        this.building = building;
        this.ssid = ssid;
        this.uuid = uuid;
    }

    public EstimatedResult(String building, String ssid, String uuid, String method, int k, int threshold, int algorithmVersion) {
        this(building, ssid, uuid);

        this.method = method;
        this.k = k;
        this.threshold = threshold;
        this.algorithmVersion = algorithmVersion;
    }

    public EstimatedResult(String building, String ssid, String uuid, String method, int K, int threshold, int algorithmVersion, long date) {
        this(building, ssid, uuid, method, K, threshold, algorithmVersion);

        this.date = new Date(date);
    }

    public EstimatedResult(String building, String ssid, double positionRealX, double positionRealY, double positionEstimatedX, double positionEstimatedY,
                           String uuid, String method, int k, int threshold, int algorithmVersion, long date) {
        this();

        this.building = building;
        this.ssid = ssid;
        this.pos_x = positionRealX;
        this.pos_y = positionRealY;
        this.est_x = positionEstimatedX;
        this.est_y = positionEstimatedY;
        this.uuid = uuid;
        this.method = method;
        this.k = k;
        this.threshold = threshold;
        this.algorithmVersion = algorithmVersion;
    }
}
