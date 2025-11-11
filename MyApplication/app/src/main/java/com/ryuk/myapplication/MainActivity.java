package com.ryuk.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

public class MainActivity extends AppCompatActivity {

    EditText etCodigo;
    TextView tvResultado;
    Button btnBuscar, btnCargarCSV, btnImportarBD, btnExportarBD;
    SQLiteDatabase db;

    private static final int PICK_CSV = 1;
    private static final int PICK_DB = 2;
    private static final int PICK_EXPORT_DIR = 3;
    private final String DB_NAME = "productos.db";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCodigo = findViewById(R.id.etCodigo);
        tvResultado = findViewById(R.id.tvResultado);

        btnBuscar = findViewById(R.id.btnBuscar);
        btnCargarCSV = findViewById(R.id.btnCargarCSV);
        btnImportarBD = findViewById(R.id.btnImportarBD);
        btnExportarBD = findViewById(R.id.btnExportarBD);

        db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS productos (codigo TEXT PRIMARY KEY, descripcion TEXT, und TEXT, undxenv TEXT, linea TEXT, existencia TEXT);");

        btnBuscar.setOnClickListener(v -> {
            String codigo = etCodigo.getText().toString().trim();
            listarProducto(codigo);
        });

        btnCargarCSV.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("text/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_CSV);
        });

        btnImportarBD.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_DB);
        });

        btnExportarBD.setOnClickListener(v -> {
            // Selección de carpeta destino por picker (requiere crear nuevo archivo con CreateDocument)
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("application/x-sqlite3");
            intent.putExtra(Intent.EXTRA_TITLE, DB_NAME);
            startActivityForResult(intent, PICK_EXPORT_DIR);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if(requestCode == PICK_CSV) {
                importarCSV(uri);
            } else if(requestCode == PICK_DB) {
                importarBD(uri);
            } else if(requestCode == PICK_EXPORT_DIR) {
                exportarBDPicker(uri);
            }
        }
    }

    // Buscar producto por código
    private void listarProducto(String codigo) {
        Cursor cursor = db.rawQuery("SELECT * FROM productos WHERE codigo=?", new String[]{codigo});
        if (cursor.moveToFirst()) {
            String resultado =
                    "Código: " + cursor.getString(0) + "\n"
                            + "Descripción: " + cursor.getString(1) + "\n"
                            + "UND: " + cursor.getString(2) + "\n"
                            + "UNDXENV: " + cursor.getString(3) + "\n"
                            + "Línea: " + cursor.getString(4) + "\n"
                            + "Existencia: " + cursor.getString(5);
            tvResultado.setText(resultado);
        } else {
            tvResultado.setText("Producto no encontrado");
        }
        cursor.close();
    }

    // Importa CSV usando picker
    private void importarCSV(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            db.execSQL("DELETE FROM productos"); // Limpia la tabla

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("CODIGO")) continue; // Salta cabecera
                String[] partes = line.split(";");
                if (partes.length == 6) {
                    db.execSQL("INSERT OR REPLACE INTO productos VALUES (?,?,?,?,?,?)",
                            new Object[]{partes[0], partes[1], partes[2], partes[3], partes[4], partes[5]});
                }
            }
            Toast.makeText(this, "CSV cargado correctamente.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Importa BD usando picker (sobrescribe la BD interna)
    private void importarBD(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            File dbFile = getDatabasePath(DB_NAME);
            try (OutputStream output = new FileOutputStream(dbFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }
            Toast.makeText(this, "Base de datos importada exitosamente.", Toast.LENGTH_SHORT).show();
            db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null); // Re-abre BD
        } catch (Exception e) {
            Toast.makeText(this, "Error al importar BD: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Exporta la BD usando picker a uri destino
    private void exportarBDPicker(Uri uri) {
        try {
            File dbFile = getDatabasePath(DB_NAME);
            try (InputStream input = new FileInputStream(dbFile);
                 OutputStream output = getContentResolver().openOutputStream(uri)) {
                byte[] buffer = new byte[1024];
                int length;
                while((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }
            Toast.makeText(this, "BD exportada a destino seleccionado.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al exportar BD: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
