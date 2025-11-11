package com.ryuk.siscoosql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "coosql.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Profesiones (codprof INTEGER PRIMARY KEY, descripcion TEXT NOT NULL)");
        db.execSQL("CREATE TABLE Departamentos (codepto INTEGER PRIMARY KEY, descripcion TEXT NOT NULL)");
        db.execSQL("CREATE TABLE Clientes (carnet INTEGER PRIMARY KEY, nombres TEXT, codprof INTEGER NOT NULL REFERENCES Profesiones(codprof), codepto INTEGER NOT NULL REFERENCES Departamentos(codepto))");
        db.execSQL("CREATE TABLE Cuentas (cuenta TEXT PRIMARY KEY, carnet INTEGER NOT NULL REFERENCES Clientes(carnet), fapertura TEXT NOT NULL)");
        db.execSQL("CREATE TABLE Movimientos (id INTEGER PRIMARY KEY AUTOINCREMENT, cuenta TEXT NOT NULL REFERENCES Cuentas(cuenta), monto INTEGER NOT NULL, fecha TEXT NOT NULL)");
        db.execSQL("CREATE INDEX idx_cliente_nombre ON Clientes(nombres)");
        db.execSQL("CREATE INDEX idx_cuenta_cliente ON Cuentas(carnet)");
        db.execSQL("CREATE INDEX idx_movimiento_cuenta ON Movimientos(cuenta)");
        db.execSQL("CREATE INDEX idx_movimiento_fecha ON Movimientos(fecha)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Movimientos");
        db.execSQL("DROP TABLE IF EXISTS Cuentas");
        db.execSQL("DROP TABLE IF EXISTS Clientes");
        db.execSQL("DROP TABLE IF EXISTS Profesiones");
        db.execSQL("DROP TABLE IF EXISTS Departamentos");
        onCreate(db);
    }
}
