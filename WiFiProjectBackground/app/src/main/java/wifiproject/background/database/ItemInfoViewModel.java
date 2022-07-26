package wifilocation.background.database;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;

public class ItemInfoViewModel extends AndroidViewModel {

    private ItemInfoRepository itemInfoRepository;

    public ItemInfoViewModel(Application application) {
        super(application);

        itemInfoRepository = new ItemInfoRepository(application);
    }

    public void insertAll(List<ItemInfo> items) {
        itemInfoRepository.insertAll(items);
    }

    public LiveData<List<ItemInfo>> loadAllItems() {
        return itemInfoRepository.loadAllItems();
    }

    public LiveData<List<ItemInfo>> loadItemsDuring(Date from, Date to) {
        return itemInfoRepository.loadItemsDuring(from, to);
    }

    public LiveData<List<ItemInfo>> loadItemsAt(Float pos_x, Float pos_y) {
        return itemInfoRepository.loadItemsAt(pos_x, pos_y);
    }
}
