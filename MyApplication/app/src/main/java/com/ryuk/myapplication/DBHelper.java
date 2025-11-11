package com.ryuk.myapplication;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "basedatos.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context, String dbPath) {
        super(context, dbPath, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // No creamos nada aquí, la BD viene ya con las tablas
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No se usa
    }

    // Obtener suma de montos
    public int getSumaMontos(int codProf, int codDepto) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT SUM(movimientos.MONTO) FROM movimientos " +
                "JOIN cuentas ON movimientos.CUENTA = cuentas.CUENTA " +
                "JOIN clientes ON cuentas.CARNET = clientes.CARNET " +
                "WHERE clientes.CODPROF=? AND clientes.CODEPTO=?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(codProf), String.valueOf(codDepto)});
        int suma = 0;
        if (cursor.moveToFirst()) {
            suma = cursor.isNull(0) ? 0 : cursor.getInt(0);
        }
        cursor.close();
        return suma;
    }

    // (Métodos vacíos para agregar registros, aún sin implementación)
    public void agregarDepartamento(int codDepto, String descripcion) {}
    public void agregarMovimiento(String cuenta, int monto, String fecha) {}
    public void agregarProfesion(int codProf, String descripcion) {}
}
