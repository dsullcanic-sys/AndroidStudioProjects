package com.ryuk.taxigps;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.database.Cursor;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "taxiDB.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_CONDUCTORES = "conductores";
    private static final String COL_CARNET = "carnet";
    private static final String COL_PATERNO = "paterno";
    private static final String COL_MATERNO = "materno";
    private static final String COL_NOMBRES = "nombres";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_CONDUCTORES + " (" +
                COL_CARNET + " TEXT PRIMARY KEY," +
                COL_PATERNO + " TEXT," +
                COL_MATERNO + " TEXT," +
                COL_NOMBRES + " TEXT);";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONDUCTORES);
        onCreate(db);
    }

    public void insertarConductor(String carnet, String paterno, String materno, String nombres) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CARNET, carnet);
        values.put(COL_PATERNO, paterno);
        values.put(COL_MATERNO, materno);
        values.put(COL_NOMBRES, nombres);
        db.insertWithOnConflict(TABLE_CONDUCTORES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public String obtenerNombreConductor(String carnet) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CONDUCTORES,
                new String[]{COL_NOMBRES, COL_PATERNO, COL_MATERNO},
                COL_CARNET + "=?",
                new String[]{carnet},
                null, null, null);
        String nombreCompleto = null;
        if (cursor != null && cursor.moveToFirst()) {
            nombreCompleto = cursor.getString(0) + " " + cursor.getString(1) + " " + cursor.getString(2);
            cursor.close();
        }
        db.close();
        return nombreCompleto;
    }
}
