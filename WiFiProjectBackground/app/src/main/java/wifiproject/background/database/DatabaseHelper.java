package wifilocation.background.database;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import wifilocation.background.MainActivity;
import wifilocation.background.barcode.Barcode;
import wifilocation.background.database.EstimatedResult;
import wifilocation.background.database.ItemInfo;
import wifilocation.background.serverconnection.RetrofitAPI;
import wifilocation.background.serverconnection.RetrofitClient;


/**
 * example)
 * DatabaseHelper dbHelper = new DatabaseHelper(context);
 * dbHelper.insertIntoWiFiInfo(parameters);
 * List<WiFiItem> items = dbHelper.searchFromWiFiInfo(parameters);
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    Context context;
    public static final String DBNAME = "wifilocation1.db";
    public static int VERSION = 1;

    public static final String TABLE_WIFIINFO = "wifiinfo";
    public static final String POS_X = "pos_x";
    public static final String POS_Y = "pos_y";
    public static final String SSID = "SSID";
    public static final String BSSID = "BSSID";
    public static final String FREQUENCY = "frequency";
    public static final String LEVEL = "level";
    public static final String DATE = "date";
    public static final String UUID = "uuid";
    public static final String BUILDING = "building";
    public static final String METHOD = "method";

    public static final String TABLE_FINGERPRINT = "fingerprint";
    // POS_X
    // POS_Y
    // UUID
    // DATE
    public static final String EST_X = "est_x";
    public static final String EST_Y = "est_y";
    public static final String K = "k";
    public static final String THRESHOLD = "threshold";
    // BUILDING
    // SSID
    public static final String ALGORITHM_VERSION = "algorithmVersion";
    // METHOD

    public static final String TABLE_BARCODE = "barcode";
    public static final String BARCODE_SERIAL = "barcode_serial";
    // BUILDING
    // POS_X
    // POS_Y
    // DATE

    public DatabaseHelper(@Nullable Context context) {
        super(context, DBNAME, null, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // wifiinfo 테이블 생성
        sqLiteDatabase.execSQL("create table if not exists " + TABLE_WIFIINFO + " (" +
                "id integer PRIMARY KEY autoincrement, " +
                POS_X + " real, " +
                POS_Y + " real, " +
                SSID + " text, " +
                BSSID + " text, " +
                FREQUENCY + " integer, " +
                LEVEL + " integer, " +
                DATE + " integer, " +
                UUID + " text, " +
                BUILDING + " text, " +
                METHOD + " text)");

        // fingerprint 테이블 생성
        sqLiteDatabase.execSQL("create table if not exists " + TABLE_FINGERPRINT + " (" +
                "id integer PRIMARY KEY autoincrement, " +
                POS_X + " real, " +
                POS_Y + " real, " +
                UUID + " text, " +
                DATE + " integer, " +
                EST_X + " real, " +
                EST_Y + " real, " +
                K + " integer, " +
                THRESHOLD + " integer, " +
                BUILDING + " text, " +
                SSID + " text, " +
                ALGORITHM_VERSION + " integer, " +
                METHOD + " text)");
        
        // barcode 테이블 생성
        sqLiteDatabase.execSQL("create table if not exists " + TABLE_BARCODE + " (" +
                "id integer primary key autoincrement, " +
                BARCODE_SERIAL + " text, " +
                BUILDING + " text, " +
                POS_X + " real, " +
                POS_Y + " real, " +
                DATE + " date)");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (newVersion > 1) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_WIFIINFO);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_FINGERPRINT);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_BARCODE);
            onCreate(sqLiteDatabase);
        }
    }

    /**
     * wifiinfo 테이블에 데이터 추가
     *
     * @param items 스캔한 WiFiItem 전달, DB에 저장
     */
    public void insertIntoWiFiInfo(List<ItemInfo> items) {
        if (items.size() == 0) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        StringBuilder sql = new StringBuilder("insert into " + TABLE_WIFIINFO + String.format(" (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s) ", POS_X, POS_Y, SSID, BSSID, FREQUENCY, LEVEL, DATE, UUID, BUILDING, METHOD) + " values ");
        int sqlLength = sql.length();
        for (int i = 1; i <= items.size(); i++) {
            ItemInfo item = items.get(i - 1);
            sql.append(String.format(" (%f, %f, '%s', '%s', %d, %d, %d, '%s', '%s', '%s'), ",
                    item.getPos_x(), item.getPos_y(), item.getSSID().replace("'", "''"), item.getBSSID(), item.getFrequency(), item.getLevel(), item.getDate().getTime(), item.getUuid(), item.getBuilding(), item.getMethod()));

            if (i % 500 == 0) {
                try {
                    db.execSQL(sql.substring(0, sql.length() - 2));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    sql.setLength(sqlLength);
                }
            }
        }
        if (items.size() % 500 != 0) {
            db.execSQL(sql.substring(0, sql.length() - 2));
        }
    }

    /**
     * wifiinfo 테이블로부터 데이터 조회
     *
     * @param building building이 일치하는 row들만 조회
     * @param ssid     ssid가 일치하는 row들만 조회
     * @param x        x - 1 < pos_x < x + 1 인 row들만 조회
     * @param y        y - 1 < pos_y < y + 1 인 row들만 조회, x와 y 모두 null 전달 시 위치에 제한 없음
     * @param from     from 잉후에 등록된 row들만 조회, null 전달 시 제한 없음
     * @param to       to 이전에 등록된 row들만 조회, null 전달 시 제한 없음
     * @return sql문 실행 결과로 나온 row들을 List<WiFiItem> 으로 변환하여 반환
     */
    @SuppressLint("Range")
    public List<ItemInfo> searchFromWiFiInfo(String building, String ssid, Float x, Float y, String from, String to) {
        StringBuilder sql = new StringBuilder("select " + String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s", POS_X, POS_Y, SSID, BSSID, LEVEL, FREQUENCY, UUID, BUILDING, METHOD, DATE) + " from " + TABLE_WIFIINFO);
        List<String> conditions = new ArrayList<String>();
        // 빌딩 조건 추가
        if (building != null) {
            conditions.add(String.format(" (%s = '%s') ", BUILDING, building));
        }
        // SSID 조건 추가
        if (ssid != null) {
            conditions.add(String.format(" (%s = '%s') ", SSID, ssid));
        }
        // 날짜 조건 추가
        if (from != null || to != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                long timestampFrom, timestampTo;
                timestampFrom = sdf.parse(from == null ? "20020202" : from).getTime();
                timestampTo = sdf.parse(to == null ? "20300303" : to).getTime();
                conditions.add(String.format(" (%s between %d and %d) ", DATE, timestampFrom, timestampTo));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        // 위치 조건 추가
        if (x != null && y != null) {
            conditions.add(String.format(" ((%s between %f and %f) and (%s between %f and %f)) ", POS_X, x - 1, x + 1, POS_Y, y - 1, y + 1));
        }
        if (conditions.size() != 0) {
            sql.append(" where ");
        }
        for (int i = 0; i < conditions.size(); i++) {
            sql.append(conditions.get(i));
            if (i != conditions.size() - 1) {
                sql.append(" and ");
            }
        }

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql.toString(), null);

        List<ItemInfo> result = new ArrayList<ItemInfo>();

        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            cursor.moveToNext();
            result.add(new ItemInfo(cursor.getFloat(cursor.getColumnIndex(POS_X)),
                    cursor.getFloat(cursor.getColumnIndex(POS_Y)),
                    cursor.getString(cursor.getColumnIndex(SSID)),
                    cursor.getString(cursor.getColumnIndex(BSSID)),
                    cursor.getInt(cursor.getColumnIndex(LEVEL)),
                    cursor.getInt(cursor.getColumnIndex(FREQUENCY)),
                    cursor.getString(cursor.getColumnIndex(UUID)),
                    cursor.getString(cursor.getColumnIndex(BUILDING)),
                    cursor.getString(cursor.getColumnIndex(METHOD)),
                    cursor.getLong(cursor.getColumnIndex(DATE))));
        }
        return result;
    }

    public void logAllWiFiInfo() {
        String sql = "select " + String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s", POS_X, POS_Y, SSID, BSSID, LEVEL, FREQUENCY, UUID, BUILDING, METHOD) + " from " + TABLE_WIFIINFO;
        SQLiteDatabase readableDatabase = getReadableDatabase();
        Cursor cursor = readableDatabase.rawQuery(sql, null);
        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            cursor.moveToNext();
            ItemInfo wiFiItem = new ItemInfo(cursor.getFloat(0),
                    cursor.getFloat(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getInt(4),
                    cursor.getInt(5),
                    cursor.getString(6),
                    cursor.getString(7),
                    cursor.getString(8));
            Log.d(getClass().getName(), wiFiItem.toString());
        }
    }

    /**
     * fingerprint 테이블에서 데이터 삭제
     */
    public void deleteWiFiInfo() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + TABLE_WIFIINFO);
    }

    /**
     * fingerprint 테이블에 데이터 추가
     *
     * @param items 추정된 위치 정보 리스트 전달
     */
    public void insertIntoFingerprint(List<EstimatedResult> items) {
        if (items.size() == 0) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        StringBuilder sql = new StringBuilder("insert into " + TABLE_FINGERPRINT + String.format(" (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)", POS_X, POS_Y, UUID, DATE, EST_X, EST_Y, K, THRESHOLD, BUILDING, SSID, ALGORITHM_VERSION, METHOD) + " values ");
        int sqlLength = sql.length();
        for (int i = 1; i <= items.size(); i++) {
            EstimatedResult item = items.get(i - 1);
            sql.append(String.format(" (%f, %f, '%s', %d, %f, %f, %d, %d, '%s', '%s', %d, '%s'), ",
                    item.getPos_x(), item.getPos_y(), item.getUuid(), item.getDate().getTime(), item.getEst_x(), item.getEst_y(),
                    item.getK(), item.getThreshold(), item.getBuilding(), item.getSsid().replace("'", "''"), item.getAlgorithmVersion(), item.getMethod()));

            if (i % 500 == 0) {
                try {
                    db.execSQL(sql.substring(0, sql.length() - 2));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    sql.setLength(sqlLength);
                }
            }
        }
        if (items.size() % 500 != 0) {
            db.execSQL(sql.substring(0, sql.length() - 2));
        }
    }

    /**
     * fingerprint 테이블에서 데이터 조회
     *
     * @return sql문 실행 결과로 나온 row들을 List<EstimateResult> 로 바꿔서 반환
     */
    @SuppressLint("Range")
    public List<EstimatedResult> searchFromFingerprint() {
        StringBuilder sql = new StringBuilder("select " + String.format(" %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s ", POS_X, POS_Y, UUID, DATE, EST_X, EST_Y, K, THRESHOLD, BUILDING, SSID, ALGORITHM_VERSION, METHOD) +
                " from " + TABLE_FINGERPRINT);
        List<String> conditions = new ArrayList<String>();
        if (conditions.size() != 0) {
            sql.append(" where ");
        }
        for (int i = 0; i < conditions.size(); i++) {
            sql.append(conditions.get(i));
            if (i != conditions.size() - 1) {
                sql.append(" and ");
            }
        }


        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql.toString(), null);

        List<EstimatedResult> result = new ArrayList<EstimatedResult>();
        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            cursor.moveToNext();
            result.add(new EstimatedResult(cursor.getString(cursor.getColumnIndex(BUILDING)),
                    cursor.getString(cursor.getColumnIndex(SSID)),
                    cursor.getFloat(cursor.getColumnIndex(POS_X)),
                    cursor.getFloat(cursor.getColumnIndex(POS_Y)),
                    cursor.getFloat(cursor.getColumnIndex(EST_X)),
                    cursor.getFloat(cursor.getColumnIndex(EST_Y)),
                    cursor.getString(cursor.getColumnIndex(UUID)),
                    cursor.getString(cursor.getColumnIndex(METHOD)),
                    cursor.getInt(cursor.getColumnIndex(K)),
                    cursor.getInt(cursor.getColumnIndex(THRESHOLD)),
                    cursor.getInt(cursor.getColumnIndex(ALGORITHM_VERSION)),
                    cursor.getLong(cursor.getColumnIndex(DATE))));
        }
        return result;
    }

    /**
     * 오늘 측정된 fingerprint 테이블에서 서버에 아직 올라가지 않은 row를 반환
     *
     * @return sql문 실행 결과로 나온 row들을 List<EstimateResult> 로 바꿔서 반환
     */
    @SuppressLint("Range")
    public List<EstimatedResult> loadItemsAfter(Long count, Date today) {
        StringBuilder sql = new StringBuilder("select" + String.format(" %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s ", POS_X, POS_Y, UUID, DATE, EST_X, EST_Y, K, THRESHOLD, BUILDING, SSID, ALGORITHM_VERSION, METHOD) +
                "from " + TABLE_FINGERPRINT +
                " where date >= " + today.getTime() +
                " LIMIT -1 OFFSET " + count);
        List<String> conditions = new ArrayList<String>();

        Log.d("ㅎㅎ", sql.toString());

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql.toString(), null);

        List<EstimatedResult> result = new ArrayList<EstimatedResult>();
        int cnt = cursor.getCount();
        for (int i = 0; i < cnt; i++) {
            cursor.moveToNext();
            result.add(new EstimatedResult(cursor.getString(cursor.getColumnIndex(BUILDING)),
                    cursor.getString(cursor.getColumnIndex(SSID)),
                    cursor.getFloat(cursor.getColumnIndex(POS_X)),
                    cursor.getFloat(cursor.getColumnIndex(POS_Y)),
                    cursor.getFloat(cursor.getColumnIndex(EST_X)),
                    cursor.getFloat(cursor.getColumnIndex(EST_Y)),
                    cursor.getString(cursor.getColumnIndex(UUID)),
                    cursor.getString(cursor.getColumnIndex(METHOD)),
                    cursor.getInt(cursor.getColumnIndex(K)),
                    cursor.getInt(cursor.getColumnIndex(THRESHOLD)),
                    cursor.getInt(cursor.getColumnIndex(ALGORITHM_VERSION)),
                    cursor.getLong(cursor.getColumnIndex(DATE))));
        }
        return result;
    }

    /**
     * fingerprint 테이블에서 데이터 삭제
     */
    public void deleteFingerprint() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + TABLE_FINGERPRINT);
    }

    /**
     * barcode 테이블에 데이터 추가
     * @param items
     */
    public void insertIntoBarcode(List<Barcode> items) {
        if (items.size() == 0) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        StringBuilder sql = new StringBuilder("insert into " + TABLE_BARCODE + String.format(" (%s, %s, %s, %s, %s)", BARCODE_SERIAL, BUILDING, POS_X, POS_Y, DATE) + " values ");
        int sqlLength = sql.length();
        for (int i = 1; i <= items.size(); i++) {
            Barcode item = items.get(i - 1);
            sql.append(String.format(" ('%s', '%s', %f, %f, %d), ", item.getSerial(), MainActivity.building, item.getPosX(), item.getPosY(), item.getDate().getTime()));
            if (i % 500 == 0) {
                try {
                    db.execSQL(sql.substring(0, sql.length() - 2));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    sql.setLength(sqlLength);
                }
            }
        }
        if (items.size() % 500 != 0) {
            db.execSQL(sql.substring(0, sql.length() - 2));
        }
    }

    /**
     * barcode 테이블에서 데이터 조회
     * @return 실행 결과로 나온 row들을 List<Barcode>로 반환
     */
    @SuppressLint("Range")
    public List<Barcode> searchFromBarcode() {
        StringBuilder sql = new StringBuilder("select " + String.format(" %s, %s, %s, %s ", BARCODE_SERIAL, POS_X, POS_Y, DATE) + " from " + TABLE_BARCODE + " where " + String.format(" (%s = '%s') ", BUILDING, MainActivity.building));

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sql.toString(), null);

        List<Barcode> result = new ArrayList<>();
        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            cursor.moveToNext();
            result.add(new Barcode(cursor.getString(cursor.getColumnIndex(BARCODE_SERIAL)),
                    cursor.getFloat(cursor.getColumnIndex(POS_X)),
                    cursor.getFloat(cursor.getColumnIndex(POS_Y)),
                    cursor.getLong(cursor.getColumnIndex(DATE))));
        }
        return result;
    }

    /**
     * barcode 테이블에서 데이터 삭제
     */
    public void deleteBarcode() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + TABLE_BARCODE);
    }
}
