package com.ryuk.sisprofydep;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "sisprofydep.db";
    public static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS departamentos (" +
                "CODEPTO INTEGER PRIMARY KEY, DESCRIPCION TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS profesiones (" +
                "CODPROF INTEGER PRIMARY KEY, DESCRIPCION TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS clientes (" +
                "CARNET INTEGER PRIMARY KEY, NOMBRES TEXT, CODPROF INTEGER, CODEPTO INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS cuentas (" +
                "CUENTA TEXT PRIMARY KEY, CARNET INTEGER, FAPERTURA TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS movimientos (" +
                "CUENTA TEXT, MONTO INTEGER, FECHA TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    // Limpia todas las tablas
    public void resetBD() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM movimientos");
            db.execSQL("DELETE FROM cuentas");
            db.execSQL("DELETE FROM clientes");
            db.execSQL("DELETE FROM profesiones");
            db.execSQL("DELETE FROM departamentos");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Inserciones
    public void agregarDepartamento(Departamento d) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO departamentos VALUES (?, ?)",
                new Object[]{d.codDepto, d.descripcion});
    }

    public void agregarProfesion(Profesion p) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO profesiones VALUES (?, ?)",
                new Object[]{p.codProf, p.descripcion});
    }

    public void agregarCliente(Cliente c) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO clientes VALUES (?, ?, ?, ?)",
                new Object[]{c.carnet, c.nombres, c.codProf, c.codDepto});
    }

    public void agregarCuenta(Cuenta c) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO cuentas VALUES (?, ?, ?)",
                new Object[]{c.cuenta, c.carnet, c.fApertura});
    }

    public void agregarMovimiento(Movimiento m) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO movimientos VALUES (?, ?, ?)",
                new Object[]{m.cuenta, m.monto, m.fecha});
    }

    // Consultas
    public int getSumaMontos(int codProf, int codDepto) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT SUM(m.MONTO) " +
                "FROM movimientos m " +
                "JOIN cuentas c ON m.CUENTA = c.CUENTA " +
                "JOIN clientes cl ON c.CARNET = cl.CARNET " +
                "WHERE cl.CODPROF=? AND cl.CODEPTO=?";
        Cursor cursor = db.rawQuery(sql, new String[]{
                String.valueOf(codProf), String.valueOf(codDepto)
        });
        int suma = 0;
        if (cursor.moveToFirst()) suma = cursor.isNull(0) ? 0 : cursor.getInt(0);
        cursor.close();
        return suma;
    }

    public String getDescripcionProfesion(int codProf) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT DESCRIPCION FROM profesiones WHERE CODPROF=?",
                new String[]{String.valueOf(codProf)});
        String desc = "(NO ENCONTRADA)";
        if (c.moveToFirst()) desc = c.getString(0);
        c.close();
        return desc;
    }

    public String getDescripcionDepartamento(int codDepto) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT DESCRIPCION FROM departamentos WHERE CODEPTO=?",
                new String[]{String.valueOf(codDepto)});
        String desc = "(NO ENCONTRADO)";
        if (c.moveToFirst()) desc = c.getString(0);
        c.close();
        return desc;
    }

    // Modelos
    public static class Departamento {
        public int codDepto;
        public String descripcion;
        public Departamento(int codDepto, String descripcion) {
            this.codDepto = codDepto;
            this.descripcion = descripcion;
        }
    }

    public static class Profesion {
        public int codProf;
        public String descripcion;
        public Profesion(int codProf, String descripcion) {
            this.codProf = codProf;
            this.descripcion = descripcion;
        }
    }

    public static class Cliente {
        public int carnet, codProf, codDepto;
        public String nombres;
        public Cliente(int carnet, String nombres, int codProf, int codDepto) {
            this.carnet = carnet;
            this.nombres = nombres;
            this.codProf = codProf;
            this.codDepto = codDepto;
        }
    }

    public static class Cuenta {
        public String cuenta;
        public int carnet;
        public String fApertura;
        public Cuenta(String cuenta, int carnet, String fApertura) {
            this.cuenta = cuenta;
            this.carnet = carnet;
            this.fApertura = fApertura;
        }
    }

    public static class Movimiento {
        public String cuenta, fecha;
        public int monto;
        public Movimiento(String cuenta, int monto, String fecha) {
            this.cuenta = cuenta;
            this.monto = monto;
            this.fecha = fecha;
        }
    }
}
