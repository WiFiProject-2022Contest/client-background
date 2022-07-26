package wifilocation.background.database;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

public class EstimatedResultRepository {

    private EstimatedResultDao estimatedResultDao;

    public EstimatedResultRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        estimatedResultDao = db.estimatedResultDao();
    }

    public void insertAll(List<EstimatedResult> items) {
        AppDatabase.databaseWriteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                estimatedResultDao.insertAll(items);
            }
        });
    }

    public void deleteAll() {
        AppDatabase.databaseWriteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                estimatedResultDao.deleteAll();
            }
        });
    }


    public LiveData<List<EstimatedResult>> loadAllItems() {
        return estimatedResultDao.loadAllItems();
    }

    public LiveData<List<EstimatedResult>> loadItemsDuring(Date from, Date to) {
        return estimatedResultDao.loadItemsDuring(from, to);
    }

    public LiveData<List<EstimatedResult>> loadItemsAfter(Long id, Long today) {
        return estimatedResultDao.loadItemsAfter(id, today);
    }
}
