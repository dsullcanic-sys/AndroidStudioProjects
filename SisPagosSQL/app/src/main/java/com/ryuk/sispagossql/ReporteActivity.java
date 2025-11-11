package com.ryuk.sispagossql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ReporteActivity extends AppCompatActivity {

    private static final String DB_NAME = "mi_base.bd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporte);

        String tabla = getIntent().getStringExtra("tabla");
        ((TextView) findViewById(R.id.tvReporteTitulo)).setText("Reporte: " + tabla);
        findViewById(R.id.btnRetornar).setOnClickListener(v -> finish());

        mostrarReporte(tabla, findViewById(R.id.tableReporte));
    }

    private void mostrarReporte(String tabla, TableLayout tl) {
        SQLiteDatabase db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);

        // Verificar si existe la tabla
        Cursor tc = null;
        try {
            tc = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{tabla});
            if (!tc.moveToFirst()) {
                tl.addView(crearFila(new String[]{"Sin datos"}, true));
                return;
            }
        } finally {
            if (tc != null) tc.close();
        }

        // Obtener nombres de columnas
        String[] cols;
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + tabla + ")", null);
            cols = new String[c.getCount()];
            for (int i = 0; c.moveToNext(); i++) cols[i] = c.getString(1);
        } finally {
            if (c != null) c.close();
        }

        // Header
        tl.addView(crearFila(cols, true));

        // Data
        Cursor data = null;
        try {
            data = db.rawQuery("SELECT * FROM " + tabla, null);
            while (data.moveToNext()) {
                String[] vals = new String[cols.length];
                for (int i = 0; i < cols.length; i++) vals[i] = data.getString(i);
                tl.addView(crearFila(vals, false));
            }
        } finally {
            if (data != null) data.close();
        }

        db.close();
    }

    private TableRow crearFila(String[] textos, boolean bold) {
        TableRow row = new TableRow(this);
        for (String txt : textos) {
            TextView tv = new TextView(this);
            tv.setText(txt);
            tv.setPadding(16, bold ? 10 : 8, 16, bold ? 10 : 8);
            if (bold) tv.setTypeface(null, Typeface.BOLD);
            row.addView(tv);
        }
        return row;
    }
}