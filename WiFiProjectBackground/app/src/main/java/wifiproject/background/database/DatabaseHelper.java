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
     * 서버와 DB 동기화
     */
    public void synchronize() {
        SynchronizeTask synchronizeTask = new SynchronizeTask();
        synchronizeTask.execute();
    }

    private class SynchronizeTask extends AsyncTask<String, String, String> {

        ProgressDialog progressDialog = new ProgressDialog(context);

        @Override
        protected void onPreExecute() {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            // 서버에 new가 1인 데이터 올리기
            publishProgress("서버에 신규 데이터 업로드 중...");
            pushRemoteNewData();
            // 로컬에 있는 데이터 삭제
            publishProgress("로컬 DB의 데이터 삭제 중...");
            deleteAllLocal();
            // 서버로부터 데이터 받아오기
            publishProgress("서버로부터 데이터를 받아오는 중...");
            getAllFromRemote();
            return "동기화 완료";
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            if (progress.length > 0) {
                progressDialog.setMessage(progress[0]);
            }
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
            super.onPostExecute(result);
        }

        private void pushRemoteNewData() {
            List<ItemInfo> wiFiItems = searchFromWiFiInfo(null, null, null, null, null, null);
            List<EstimatedResult> estimatedResults = searchFromFingerprint();
            RetrofitAPI retrofitAPI = RetrofitClient.getRetrofitAPI();
            try {
                retrofitAPI.postDataWiFiItem(wiFiItems).execute();
                retrofitAPI.postDataEstimatedResult(estimatedResults).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void deleteAllLocal() {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("delete from " + TABLE_WIFIINFO);
            db.execSQL("delete from " + TABLE_FINGERPRINT);
        }

        private void getAllFromRemote() {
            RetrofitAPI retrofitAPI = RetrofitClient.getRetrofitAPI();
            try {
                List<ItemInfo> wiFiItems = retrofitAPI.getDataWiFiItem(null, null, null, null, null, null).execute().body();
                List<EstimatedResult> estimatedResults = retrofitAPI.getDataEstimateResult(null, null).execute().body();
                insertIntoWiFiInfo(wiFiItems);
                insertIntoFingerprint(estimatedResults);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
