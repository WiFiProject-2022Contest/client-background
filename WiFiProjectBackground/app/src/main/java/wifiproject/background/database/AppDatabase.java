package wifilocation.background.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

/**
 * - 데이터베이스 생성
 * AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "database-name").build();
 *
 * - 사용법
 * ItemInfoDao itemInfoDao = db.itemInfoDao();
 * List<ItemInfo> items = itemInfoDao.loadAllItems();
 */
@Database(entities = {ItemInfo.class, EstimatedResult.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract ItemInfoDao itemInfoDao();

    public abstract EstimatedResultDao estimatedResultDao();
}
