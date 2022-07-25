package wifilocation.background.serverconnection;

import java.util.List;

import wifilocation.background.database.EstimatedResult;
import wifilocation.background.database.ItemInfo;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RetrofitAPI {
    @GET("/rssi")
    Call<List<ItemInfo>> getDataWiFiItem(@Query("building") String building, @Query("SSID") String ssid, @Query("pos_x") Float x, @Query("pos_y") Float y, @Query("from") String from, @Query("to") String to);

    @POST("/rssi")
    Call<PushResultModel> postDataWiFiItem(@Body List<ItemInfo> data);

    @GET("/fingerprint")
    Call<List<EstimatedResult>> getDataEstimateResult(@Query("from") String from, @Query("to") String to);

    @POST("/fingerprint")
    Call<PushResultModel> postDataEstimatedResult(@Body List<EstimatedResult> data);
}
