package com.ryuk.sispagossql;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_CSV = 2000, PICK_BD = 1001, EXPORT_BD = 1002;
    private static final String DB_NAME = "mi_base.bd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar botones de reportes
        int[] reportBtns = {R.id.btnPersonal, R.id.btnCargos, R.id.btnBon03, R.id.btnBon04,
                R.id.btnBon05, R.id.btnDes03, R.id.btnDes04, R.id.btnDes05};
        String[] tablas = {"personal", "cargos", "bon0324", "bon0424", "bon0524",
                "des0324", "des0424", "des0524"};

        for (int i = 0; i < reportBtns.length; i++) {
            String tabla = tablas[i];
            findViewById(reportBtns[i]).setOnClickListener(v -> abrirReporte(tabla));
        }

        // Botones de acción
        findViewById(R.id.btnSubirCSV).setOnClickListener(v -> {
            borrarBase();
            pickArchivo(PICK_CSV, "text/csv", true);
        });
        findViewById(R.id.btnImportarBD).setOnClickListener(v -> pickArchivo(PICK_BD, "*/*", false));
        findViewById(R.id.btnExportarBD).setOnClickListener(v -> crearArchivo(EXPORT_BD, "mi_base_exportada.bd"));
        findViewById(R.id.btnBusqueda).setOnClickListener(v ->
                startActivity(new Intent(this, BusquedaActivity.class)));
        findViewById(R.id.btnFinalizar).setOnClickListener(v -> finish());
    }

    private void abrirReporte(String tabla) {
        startActivity(new Intent(this, ReporteActivity.class).putExtra("tabla", tabla));
    }

    private void pickArchivo(int code, String type, boolean multi) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .setType(type)
                .addCategory(Intent.CATEGORY_OPENABLE);
        if (multi) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar"), code);
    }

    private void crearArchivo(int code, String nombre) {
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("*/*")
                .putExtra(Intent.EXTRA_TITLE, nombre), code);
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (result != RESULT_OK || data == null) return;

        if (code == PICK_CSV) procesarCSVs(data);
        else if (code == PICK_BD) copiarArchivo(data.getData(), getDatabasePath(DB_NAME), "importada");
        else if (code == EXPORT_BD) copiarArchivo(getDatabasePath(DB_NAME), data.getData(), "exportada");
    }

    private void procesarCSVs(Intent data) {
        borrarBase();
        ArrayList<Uri> uris = new ArrayList<>();

        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++)
                uris.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }

        for (Uri uri : uris) {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                importarCSV(is, limpiarNombre(getNombre(uri)));
            } catch (Exception e) {
                toast("Error: " + e.getMessage());
            }
        }
        toast("CSV cargados!");
    }

    private void copiarArchivo(Object src, Object dst, String msg) {
        try {
            InputStream in = src instanceof Uri ? getContentResolver().openInputStream((Uri)src) : new FileInputStream((File)src);
            OutputStream out = dst instanceof Uri ? getContentResolver().openOutputStream((Uri)dst) : new FileOutputStream((File)dst);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
            toast("Base " + msg);
        } catch (Exception e) {
            toast("Error: " + e.getMessage());
        }
    }

    private void borrarBase() {
        SQLiteDatabase db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        while (c.moveToNext()) {
            String t = c.getString(0);
            if (!t.equals("android_metadata")) db.execSQL("DROP TABLE IF EXISTS " + t);
        }
        c.close();
        db.close();
    }

    private void importarCSV(InputStream is, String tabla) {
        SQLiteDatabase db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String[] cols = r.readLine().split(";");
            for (int i = 0; i < cols.length; i++) {
                cols[i] = cols[i].trim().replaceAll("[^a-zA-Z0-9_]", "_");
                if (cols[i].isEmpty()) cols[i] = "col" + i;
            }

            StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tabla + " (");
            for (int i = 0; i < cols.length; i++)
                sql.append(cols[i]).append(" TEXT").append(i < cols.length - 1 ? ", " : ")");
            db.execSQL(sql.toString());

            db.beginTransaction();
            String line;
            while ((line = r.readLine()) != null) {
                String[] vals = line.split(";", -1);
                ContentValues cv = new ContentValues();
                for (int i = 0; i < cols.length && i < vals.length; i++)
                    cv.put(cols[i], vals[i]);
                db.insert(tabla, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private String getNombre(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst())
                    return c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            }
        }
        return uri.getLastPathSegment();
    }

    private String limpiarNombre(String n) {
        return n.toLowerCase().replace(".csv", "").replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public static String getDbName() {
        return DB_NAME;
    }
}