package com.ryuk.sistemaventas;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BDHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ventas.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE_PRODUCTOS = "productos";
    public static final String TABLE_PRECIOS = "precios";

    public BDHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTablaProductos = "CREATE TABLE " + TABLE_PRODUCTOS + " (" +
                "codigo INTEGER PRIMARY KEY," +
                "descripcion TEXT," +
                "und TEXT," +
                "undxenv INTEGER," +
                "linea INTEGER," +
                "existencia INTEGER" +
                ")";
        db.execSQL(createTablaProductos);

        String createTablaPrecios = "CREATE TABLE " + TABLE_PRECIOS + " (" +
                "codigo INTEGER," +
                "tipo TEXT," +
                "fecha TEXT," +
                "precio REAL," +
                "PRIMARY KEY (codigo, fecha)," +
                "FOREIGN KEY(codigo) REFERENCES " + TABLE_PRODUCTOS + "(codigo)" +
                ")";
        db.execSQL(createTablaPrecios);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRECIOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTOS);
        onCreate(db);
    }
}
