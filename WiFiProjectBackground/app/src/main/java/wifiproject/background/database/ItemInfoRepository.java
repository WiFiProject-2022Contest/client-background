package wifilocation.background.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

public class ItemInfoRepository {

    private ItemInfoDao itemInfoDao;

    public ItemInfoRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        itemInfoDao = db.itemInfoDao();
    }

    public void insertAll(List<ItemInfo> items) {
        AppDatabase.databaseWriteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                itemInfoDao.insertAll(items);
            }
        });
    }

    public LiveData<List<ItemInfo>> loadAllItems() {
        return itemInfoDao.loadAllItems();
    }

    public LiveData<List<ItemInfo>> loadItemsDuring(Date from, Date to) {
        return itemInfoDao.loadItemsDuring(from, to);
    }

    public LiveData<List<ItemInfo>> loadItemsAt(Float pos_x, Float pos_y) {
        return itemInfoDao.loadItemsAt(pos_x, pos_y);
    }
}
