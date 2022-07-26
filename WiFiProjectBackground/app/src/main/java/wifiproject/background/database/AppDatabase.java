package wifilocation.background.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 데이터베이스 클래스
 * 사용 가능한 메서드들은 각 DAO 참고
 *
 * - 사용 예시 (fingerprint 테이블 접근)
 * EstimatedResultRepository r = new EstimatedResultRepository(application);
 * r.insertAll(items);
 *
 * List<EstimatedResult> items = r.loadAllItems().getValue();
 */
@Database(entities = {ItemInfo.class, EstimatedResult.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance = null;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract ItemInfoDao itemInfoDao();

    public abstract EstimatedResultDao estimatedResultDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context, AppDatabase.class, "wifilocation1.db").build();
        }
        return instance;
    }

    public static void destroyInstance() {
        instance = null;
    }
}
