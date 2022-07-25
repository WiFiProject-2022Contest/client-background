package wifilocation.background.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

/**
 * wifiinfo 테이블 DAO
 */
@Dao
public interface ItemInfoDao {

    /**
     * 인자로 전달해준 리스트의 요소를 모두 INSERT
     */
    @Insert
    public void insertAll(List<ItemInfo> items);

    /**
     * 모든 row를 List로 반환
     */
    @Query("SELECT * FROM wifiinfo")
    public List<ItemInfo> loadAllItems();

    /**
     * 특정 기간(from ~ to)의 row를 List로 반환
     */
    @Query("SELECT * FROM wifiinfo WHERE date BETWEEN :from AND :to")
    public List<ItemInfo> loadItemsDuring(Date from, Date to);

    /**
     * 특정 지점(pos_x 근처 1미터, pos_y 근처 1미터)의 row를 List로 반환
     */
    @Query("SELECT * FROM wifiinfo " +
            "WHERE (pos_x BETWEEN :pos_x - 1 AND :pos_x + 1) AND (pos_y BETWEEN :pos_y - 1 AND :pos_y + 1)")
    public List<ItemInfo> loadItemsAt(Float pos_x, Float pos_y);
}
