package wifilocation.background.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

/**
 * - 데이터베이스 인스턴스 획득
 * AppDatabase db = AppDatabase.getInstance(context);
 *
 * - 획득 후 사용법
 * ItemInfoDao itemInfoDao = db.itemInfoDao();
 * List<ItemInfo> items = itemInfoDao.loadAllItems();
 */
@Database(entities = {ItemInfo.class, EstimatedResult.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance = null;

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
