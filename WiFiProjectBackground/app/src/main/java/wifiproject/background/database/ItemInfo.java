package wifilocation.background.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity(tableName = "wifiinfo")
@Getter
@Setter
@ToString
public class ItemInfo {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @SerializedName("pos_x")
    private Float pos_x;

    @SerializedName("pos_y")
    private Float pos_y;

    @SerializedName("SSID")
    private String SSID;

    @SerializedName("BSSID")
    private String BSSID;

    @SerializedName("level")
    private Integer level;

    @SerializedName("frequency")
    private Integer frequency;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("building")
    private String building;

    @SerializedName("method")
    private String method;

    @SerializedName("date")
    private Date date;

    private Integer _new;

    public ItemInfo(Float pos_x, Float pos_y, String SSID, String BSSID, Integer level, Integer frequency, String uuid, String building, String method, Integer _new) {
        this.pos_x = pos_x;
        this.pos_y = pos_y;
        this.SSID = SSID;
        this.BSSID = BSSID;
        this.level = level;
        this.frequency = frequency;
        this.uuid = uuid;
        this.building = building;
        this.method = method;
        this.date = new Date(System.currentTimeMillis());
    }

    public ItemInfo(Float x, Float y, String SSID, String BSSID, Integer RSSI, Integer frequency, String uuid, String building, String method, Integer _new, long date) {
        this(x, y, SSID, BSSID, RSSI, frequency, uuid, building, method, _new);
        this.date = new Date(date);
    }
}