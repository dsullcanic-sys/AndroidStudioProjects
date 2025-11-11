package com.ryuk.sisprofydep;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

public class MainActivity extends AppCompatActivity {

    private EditText etCodProf, etCodDepto;
    private TextView tvResultado;
    private Button btnCargar, btnConsultar;
    private DBHelper dbHelper;
    private static final int PICK_DB = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        setupUI();
        setupListeners();
    }

    private void setupUI() {
        etCodProf = findViewById(R.id.etCodProf);
        etCodDepto = findViewById(R.id.etCodDepto);
        tvResultado = findViewById(R.id.tvResultado);
        btnCargar = findViewById(R.id.btnCargar);
        btnConsultar = findViewById(R.id.btnConsultar);
    }

    private void setupListeners() {
        btnCargar.setOnClickListener(v -> {
            dbHelper.resetBD();  // Vacía las tablas internas
            Toast.makeText(this, "Datos anteriores eliminados. Selecciona la nueva base.", Toast.LENGTH_SHORT).show();
            abrirSelectorArchivo();  // Abre el selector
        });

        btnConsultar.setOnClickListener(v -> consultar());
    }

    private void abrirSelectorArchivo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_DB);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DB && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            importarDB(data.getData());
        }
    }

    private void importarDB(Uri uri) {
        try {
            String dbPath = getDatabasePath(DBHelper.DB_NAME).getAbsolutePath();

            // Sobrescribe el archivo interno con la BD seleccionada
            try (InputStream input = getContentResolver().openInputStream(uri);
                 OutputStream output = new FileOutputStream(dbPath, false)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }

            dbHelper = new DBHelper(this);
            Toast.makeText(this, "Nueva BD cargada correctamente.", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al importar BD: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void consultar() {
        String sp = etCodProf.getText().toString().trim();
        String sd = etCodDepto.getText().toString().trim();

        if (sp.isEmpty() || sd.isEmpty()) {
            Toast.makeText(this, "Debes ingresar ambos códigos.", Toast.LENGTH_SHORT).show();
            return;
        }

        int codProf = Integer.parseInt(sp);
        int codDepto = Integer.parseInt(sd);

        String prof = dbHelper.getDescripcionProfesion(codProf);
        String depto = dbHelper.getDescripcionDepartamento(codDepto);
        int suma = dbHelper.getSumaMontos(codProf, codDepto);

        tvResultado.setText(
                "Profesión: " + codProf + "\n" +
                "" + prof + "\n\n" +
                "Departamento: " + codDepto + "\n" +
                "" + depto + "\n\n" +
                "Monto: " + suma + "Bs"
        );

        etCodProf.setText("");
        etCodDepto.setText("");
    }
}
