package com.ryuk.sispagossql;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

// ===== CLASES MODELO =====

class Empleado {
    String carnet;
    String nombres;
    String paterno;
    String materno;

    Empleado(String carnet, String nombres, String paterno, String materno) {
        this.carnet = carnet;
        this.nombres = nombres;
        this.paterno = paterno;
        this.materno = materno;
    }
}

class Cargo {
    int id;
    String descripcion;
    double basico;

    Cargo(int id, String descripcion, double basico) {
        this.id = id;
        this.descripcion = descripcion;
        this.basico = basico;
    }
}

class Planilla {
    String carnet;
    int cargo;
    String mes; // Formato: "0324" (mes+año)

    Planilla(String carnet, int cargo, String mes) {
        this.carnet = carnet;
        this.cargo = cargo;
        this.mes = mes;
    }
}

class Transaccion {
    String carnet;
    double monto;
    String tipo; // "bono" o "descuento"
    String mes;  // Formato: "0324"

    Transaccion(String carnet, double monto, String tipo, String mes) {
        this.carnet = carnet;
        this.monto = monto;
        this.tipo = tipo;
        this.mes = mes;
    }
}

// ===== DB HELPER =====

public class DBHelper {
    private SQLiteDatabase db;

    public DBHelper(SQLiteDatabase db) {
        this.db = db;
    }

    // ===== MÉTODOS SELECT (Lectura) =====

    public Empleado buscarEmpleado(String carnet) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT CARNET, NOMBRES, PATERNO, MATERNO FROM personal WHERE CARNET = ?",
                    new String[]{carnet});
            if (c.moveToFirst()) {
                return new Empleado(c.getString(0), c.getString(1), c.getString(2), c.getString(3));
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public Empleado buscarEmpleadoPorNombre(String termino) {
        String like = "%" + termino + "%";
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT CARNET, NOMBRES, PATERNO, MATERNO FROM personal " +
                            "WHERE CARNET = ? OR NOMBRES LIKE ? OR PATERNO LIKE ? OR MATERNO LIKE ?",
                    new String[]{termino, like, like, like});
            if (c.moveToFirst()) {
                return new Empleado(c.getString(0), c.getString(1), c.getString(2), c.getString(3));
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public Cargo buscarCargo(int id) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT ID, DESCRIPCION, BASICO FROM cargos WHERE ID = ?",
                    new String[]{String.valueOf(id)});
            if (c.moveToFirst()) {
                return new Cargo(c.getInt(0), c.getString(1), c.getDouble(2));
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public Integer buscarCargoPlanilla(String carnet, String mes) {
        if (!tablaExiste("pla" + mes)) return null;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT CARGO FROM pla" + mes + " WHERE CARNET = ?", new String[]{carnet});
            if (c.moveToFirst() && !c.isNull(0)) {
                String val = c.getString(0);
                try {
                    return Integer.parseInt(val); // Corrige si CARGO está en formato texto
                } catch (NumberFormatException e) {
                    return null; // Si el valor no es numérico, ignora/omite
                }
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public double sumarMontos(String tabla, String carnet) {
        if (!tablaExiste(tabla)) return 0;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT SUM(MONTO) FROM " + tabla + " WHERE CARNET = ?",
                    new String[]{carnet});
            if (c.moveToFirst() && !c.isNull(0)) return c.getDouble(0);
        } finally {
            if (c != null) c.close();
        }
        return 0;
    }

    public boolean tablaExiste(String tabla) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{tabla});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    // ===== MÉTODOS INSERT (Escritura) =====

    public long insertarEmpleado(Empleado emp) {
        ContentValues cv = new ContentValues();
        cv.put("CARNET", emp.carnet);
        cv.put("NOMBRES", emp.nombres);
        cv.put("PATERNO", emp.paterno);
        cv.put("MATERNO", emp.materno);
        return db.insert("personal", null, cv);
    }

    public long insertarCargo(Cargo cargo) {
        ContentValues cv = new ContentValues();
        cv.put("ID", cargo.id);
        cv.put("DESCRIPCION", cargo.descripcion);
        cv.put("BASICO", cargo.basico);
        return db.insert("cargos", null, cv);
    }

    public long insertarPlanilla(Planilla pla) {
        ContentValues cv = new ContentValues();
        cv.put("CARNET", pla.carnet);
        cv.put("CARGO", pla.cargo);
        return db.insert("pla" + pla.mes, null, cv);
    }

    public long insertarBono(Transaccion trans) {
        ContentValues cv = new ContentValues();
        cv.put("CARNET", trans.carnet);
        cv.put("MONTO", trans.monto);
        return db.insert("bon" + trans.mes, null, cv);
    }

    public long insertarDescuento(Transaccion trans) {
        ContentValues cv = new ContentValues();
        cv.put("CARNET", trans.carnet);
        cv.put("MONTO", trans.monto);
        return db.insert("des" + trans.mes, null, cv);
    }
}