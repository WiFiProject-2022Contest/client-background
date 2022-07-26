package wifilocation.background.database;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

public class EstimatedResultViewModel extends AndroidViewModel {

    private EstimatedResultRepository estimatedResultRepository;

    public EstimatedResultViewModel(Application application) {
        super(application);

        estimatedResultRepository = new EstimatedResultRepository(application);
    }

    public void insertAll(List<EstimatedResult> items) {
        estimatedResultRepository.insertAll(items);
    }

    public void deleteAll() {
        estimatedResultRepository.deleteAll();
    }


    public LiveData<List<EstimatedResult>> loadAllItems() {
        return estimatedResultRepository.loadAllItems();
    }

    public LiveData<List<EstimatedResult>> loadItemsDuring(Date from, Date to) {
        return estimatedResultRepository.loadItemsDuring(from, to);
    }

    public LiveData<List<EstimatedResult>> loadItemsAfter(Long count, Date today) {
        return estimatedResultRepository.loadItemsAfter(count, today);
    }
}
