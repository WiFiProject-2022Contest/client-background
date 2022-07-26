package wifilocation.background.serverconnection;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushResultModel {
    @SerializedName("count")
    Integer count;
}
