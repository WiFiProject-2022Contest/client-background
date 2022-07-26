package wifilocation.background.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
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
     * 모든 row를 삭제
     * 일자가 바뀔 때 사용
     */
    @Query("DELETE FROM fingerprint")
    public void deleteAll();


    /**
     * 모든 row를 반환
     * @return
     */
    @Query("SELECT * FROM fingerprint")
    public LiveData<List<EstimatedResult>> loadAllItems();

    /**
     * 특정 기간(from ~ to)의 row를 반환
     */
    @Query("SELECT * FROM fingerprint WHERE date BETWEEN :from AND :to")
    public LiveData<List<EstimatedResult>> loadItemsDuring(Date from, Date to);

    /**
     * 오늘 측정된 데이터 중 서버에 아직 올라가지 않은 row를 반환
     */
    @Query("SELECT * FROM fingerprint WHERE date >= :today LIMIT -1 OFFSET :count")
    public LiveData<List<EstimatedResult>> loadItemsAfter(Long count, Long today);
}
