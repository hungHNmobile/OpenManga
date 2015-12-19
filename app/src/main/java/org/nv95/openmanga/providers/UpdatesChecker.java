package org.nv95.openmanga.providers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.nv95.openmanga.utils.ErrorReporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nv95 on 19.12.15.
 */
public class UpdatesChecker extends AsyncTask<Void,Void,UpdatesChecker.MangaUpdate[]> {
    private final Context context;

    public UpdatesChecker(Context context) {
        this.context = context;
    }

    public interface OnMangaUpdatedListener {
        void onMangaUpdated(MangaUpdate[] updates);
    }

    public static class MangaUpdate {
        public MangaInfo manga;
        public int lastChapters;
        public int chapters;

        public MangaUpdate(MangaInfo mangaInfo) {
            this.manga = mangaInfo;
        }
    }

    @NonNull
    private HashMap<Integer,Integer> getChaptersMap() {
        HashMap<Integer,Integer> map = new HashMap<>();
        StorageHelper storageHelper = null;
        Cursor cursor = null;
        SQLiteDatabase database = null;
        try {
            storageHelper = new StorageHelper(context);
            database = storageHelper.getReadableDatabase();
            cursor = database.query("updates", null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    map.put(cursor.getInt(0), cursor.getInt(1));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            ErrorReporter.getInstance().report(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null) {
                database.close();
            }
            if (storageHelper != null) {
                storageHelper.close();
            }
        }
        return map;
    }

    private void saveChaptersMap(@NonNull HashMap<Integer,Integer> map) {
        StorageHelper storageHelper = null;
        SQLiteDatabase database = null;
        try {
            storageHelper = new StorageHelper(context);
            database = storageHelper.getWritableDatabase();
            for (Map.Entry<Integer,Integer> o:map.entrySet()) {
                ContentValues cv = new ContentValues();
                cv.put("id", o.getKey());
                cv.put("chapters", o.getValue());
                if (database.update("updates", cv, "id=?", new String[]{String.valueOf(o.getKey())}) == 0) {
                    database.insert("updates", null, cv);
                }
            }
        } catch (Exception e) {
            ErrorReporter.getInstance().report(e);
        } finally {
            if (database != null) {
                database.close();
            }
            if (storageHelper != null) {
                storageHelper.close();
            }
        }
    }

    @Override
    protected MangaUpdate[] doInBackground(Void... params) {
        FavouritesProvider favs = FavouritesProvider.getInstacne(context);
        try {
            MangaList mangas = favs.getList(0,0,0);
            HashMap<Integer,Integer> map = getChaptersMap();
            MangaProvider provider;
            int key;
            ArrayList<MangaUpdate> updates = new ArrayList<>();
            for (MangaInfo o:mangas) {
                if (o.getProvider().equals(LocalMangaProvider.class)) {
                    continue;
                }
                try {
                    provider = (MangaProvider) o.getProvider().newInstance();
                    key = o.getPath().hashCode();
                    MangaUpdate upd = new MangaUpdate(o);
                    upd.lastChapters = map.containsKey(key) ? map.get(key) : 0;
                    upd.chapters = provider.getDetailedInfo(o).getChapters().size();
                    map.put(key, upd.chapters);
                    if (upd.chapters > upd.lastChapters) {
                        updates.add(upd);
                    }
                } catch (Exception e) {
                    ErrorReporter.getInstance().report(e);
                }
            }
            saveChaptersMap(map);
            return updates.toArray(new MangaUpdate[updates.size()]);
        } catch (Exception e) {
            return null;
        }
    }

    public static void CheckUpdates(Context context, @NonNull final OnMangaUpdatedListener resultsListener) {
        new UpdatesChecker(context) {
            @Override
            protected void onPostExecute(MangaUpdate[] updates) {
                super.onPostExecute(updates);
                if (updates != null) {
                    resultsListener.onMangaUpdated(updates);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
