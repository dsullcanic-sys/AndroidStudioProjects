package com.ryuk.siscoosql;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private Button btnCargar, btnClientes, btnProfesiones, btnDepartamentos, btnSaldos;
    private TextView tvEstadoCarga;
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private static final int PICK_CSV_REQUEST_CODE = 100;

    private enum ArchivoTipo { PROFESIONES, DEPARTAMENTOS, CLIENTES, CUENTAS, MOVIMIENTOS }
    private Set<ArchivoTipo> tiposCargados = new HashSet<>();
    private Set<ArchivoTipo> tiposEsperados = new HashSet<>(Arrays.asList(
            ArchivoTipo.PROFESIONES, ArchivoTipo.DEPARTAMENTOS, ArchivoTipo.CLIENTES, ArchivoTipo.CUENTAS, ArchivoTipo.MOVIMIENTOS
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        btnCargar = findViewById(R.id.btnCargar);
        btnClientes = findViewById(R.id.btnClientes);
        btnProfesiones = findViewById(R.id.btnProfesiones);
        btnDepartamentos = findViewById(R.id.btnDepartamentos);
        btnSaldos = findViewById(R.id.btnSaldos);
        tvEstadoCarga = findViewById(R.id.tvEstadoCarga);

        btnCargar.setOnClickListener(v -> abrirPickerCsv());

        btnClientes.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ClientesActivity.class))
        );
        btnProfesiones.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfesionesActivity.class))
        );
        btnDepartamentos.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DepartamentosActivity.class))
        );
        btnSaldos.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SaldosActivity.class))
        );
    }

    private void abrirPickerCsv() {
        tiposCargados.clear();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_CSV_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CSV_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            borrarDatosPrevios();
            boolean huboError = false;
            StringBuilder errorBuilder = new StringBuilder();

            if(data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for(int i=0; i<count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    String resultado = importarCsvAutoDetect(uri);
                    if (!resultado.isEmpty()) {
                        huboError = true;
                        errorBuilder.append(resultado).append("\n");
                    }
                }
            } else if(data.getData() != null) {
                Uri uri = data.getData();
                String resultado = importarCsvAutoDetect(uri);
                if (!resultado.isEmpty()) {
                    huboError = true;
                    errorBuilder.append(resultado).append("\n");
                }
            }

            // Informar archivos faltantes y resultado
            Set<ArchivoTipo> tiposFaltantes = new HashSet<>(tiposEsperados);
            tiposFaltantes.removeAll(tiposCargados);

            StringBuilder resultado = new StringBuilder();
            resultado.append("Archivos cargados exitosamente: ");
            if (!tiposCargados.isEmpty()) {
                for (ArchivoTipo t : tiposCargados)
                    resultado.append(t.name()).append(", ");
                resultado.delete(resultado.length()-2, resultado.length());
            } else {
                resultado.append("Ninguno.");
            }
            if (!tiposFaltantes.isEmpty()) {
                resultado.append("\nFaltó cargar: ");
                for (ArchivoTipo t : tiposFaltantes)
                    resultado.append(t.name()).append(", ");
                resultado.delete(resultado.length()-2, resultado.length());
            }
            if (huboError) resultado.append("\nErrores:\n").append(errorBuilder);
            tvEstadoCarga.setText(resultado.toString());
        }
    }

    private void borrarDatosPrevios() {
        db.delete("Profesiones", null, null);
        db.delete("Departamentos", null, null);
        db.delete("Clientes", null, null);
        db.delete("Cuentas", null, null);
        db.delete("Movimientos", null, null);
    }

    private String importarCsvAutoDetect(Uri uri) {
        String filename = obtenerNombreArchivo(uri).toLowerCase();
        ArchivoTipo tipo = null;
        if(filename.contains("profesion")) tipo = ArchivoTipo.PROFESIONES;
        else if(filename.contains("departamento")) tipo = ArchivoTipo.DEPARTAMENTOS;
        else if(filename.contains("cliente")) tipo = ArchivoTipo.CLIENTES;
        else if(filename.contains("cuenta")) tipo = ArchivoTipo.CUENTAS;
        else if(filename.contains("movimiento")) tipo = ArchivoTipo.MOVIMIENTOS;
        if(tipo != null) {
            String error = importarCsv(uri, tipo);
            if (error.isEmpty()) tiposCargados.add(tipo);
            return error;
        } else {
            return "No se reconoce el tipo: " + filename;
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String result = "";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if(nameIndex != -1) result = cursor.getString(nameIndex);
            }
        }
        return result.isEmpty() ? uri.getLastPathSegment() : result;
    }

    private String importarCsv(Uri uri, ArchivoTipo tipo) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, java.nio.charset.Charset.forName("windows-1252")))) {

            db.beginTransaction();
            try {
                String line;
                boolean primera = true;
                SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                SimpleDateFormat formatoISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                while ((line = reader.readLine()) != null) {
                    if (primera) { primera = false; continue; }
                    String[] p = line.split(";");
                    ContentValues values = new ContentValues();

                    switch (tipo) {
                        case PROFESIONES:
                            values.put("codprof", Integer.parseInt(p[0]));
                            values.put("descripcion", p[1]);
                            db.insert("Profesiones", null, values);
                            break;
                        case DEPARTAMENTOS:
                            values.put("codepto", Integer.parseInt(p[0]));
                            values.put("descripcion", p[1]);
                            db.insert("Departamentos", null, values);
                            break;
                        case CLIENTES:
                            values.put("carnet", Integer.parseInt(p[0]));
                            values.put("nombres", p[1]);
                            values.put("codprof", Integer.parseInt(p[2]));
                            values.put("codepto", Integer.parseInt(p[3]));
                            db.insert("Clientes", null, values);
                            break;
                        case CUENTAS:
                            values.put("cuenta", p[0]);
                            values.put("carnet", Integer.parseInt(p[1]));
                            values.put("fapertura", formatoISO.format(formatoEntrada.parse(p[2])));
                            db.insert("Cuentas", null, values);
                            break;
                        case MOVIMIENTOS:
                            values.put("cuenta", p[0]);
                            values.put("monto", Integer.parseInt(p[1]));
                            values.put("fecha", formatoISO.format(formatoEntrada.parse(p[2])));
                            db.insert("Movimientos", null, values);
                            break;
                    }
                }
                db.setTransactionSuccessful();
                return "";
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error importando: " + tipo.name();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
