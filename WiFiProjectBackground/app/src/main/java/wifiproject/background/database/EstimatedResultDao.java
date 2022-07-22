package wifilocation.background.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

/**
 * fingerprint 테이블 DAO
 */
@Dao
public interface EstimatedResultDao {

    /**
     * 인자로 전달해준 리스트의 요소를 모두 INSERT
     */
    @Insert
    public void insertAll(List<EstimatedResult> items);

    /**
     * 모든 row를 List로 반환
     * @return
     */
    @Query("SELECT * FROM fingerprint")
    public List<EstimatedResult> loadAllItems();

    /**
     * 특정 기간(from ~ to)의 row를 List로 반환
     */
    @Query("SELECT * FROM fingerprint WHERE date BETWEEN :from AND :to")
    public List<EstimatedResult> loadItemsDuring(Date from, Date to);
}
