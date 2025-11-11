package com.ryuk.sistemaventas;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_CSV_FILES = 1;
    private TableLayout tableLayout;
    private BDHelper dbHelper;
    private EditText edtCodigo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tableLayout = findViewById(R.id.tableLayoutResultados);
        edtCodigo = findViewById(R.id.edtCodigo);
        dbHelper = new BDHelper(this);

        findViewById(R.id.btnSubirArchivos).setOnClickListener(v -> abrirPickerArchivos());
        findViewById(R.id.btnBuscar).setOnClickListener(v -> buscar());
        findViewById(R.id.btnAgregarProducto).setOnClickListener(v -> dialogoAgregarProducto());
        findViewById(R.id.btnAgregarPrecio).setOnClickListener(v -> dialogoAgregarPrecio());

        tableLayout.removeAllViews();
    }

    private void abrirPickerArchivos() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_CSV_FILES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CSV_FILES && resultCode == RESULT_OK && data != null) {
            java.util.ArrayList<Uri> uris = new java.util.ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++)
                    uris.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
            for (Uri uri : uris) {
                if (esArchivoProductos(uri)) leerCSVProductos(uri);
                else if (esArchivoPrecios(uri)) leerCSVPrecios(uri);
            }
        }
    }

    private boolean esArchivoProductos(Uri uri) {
        return chequearCabecera(uri, "CODIGO;DESCRIPCION;UND;UNDXENV;LINEA;EXISTENCIA");
    }
    private boolean esArchivoPrecios(Uri uri) {
        return chequearCabecera(uri, "COD;TIPO;DESDE;PRECIO");
    }
    private boolean chequearCabecera(Uri uri, String cabecera) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String primera = reader.readLine();
            return primera != null && primera.trim().replaceAll("\\s+", "").contains(cabecera.replaceAll("\\s+", ""));
        } catch(Exception e) { return false; }
    }

    private void leerCSVProductos(Uri uri) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String line = reader.readLine();
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            while ((line = reader.readLine()) != null) {
                String[] t = line.split(";");
                db.execSQL("INSERT OR IGNORE INTO productos (codigo, descripcion, und, undxenv, linea, existencia) VALUES (?, ?, ?, ?, ?, ?)",
                        new Object[]{ Integer.parseInt(t[0].trim()), t[1].trim(), t[2].trim(), Integer.parseInt(t[3].trim()), Integer.parseInt(t[4].trim()), Integer.parseInt(t[5].trim()) });
            }
        } catch (Exception e) { mostrarError(e); }
    }

    private void leerCSVPrecios(Uri uri) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String line = reader.readLine();
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            while ((line = reader.readLine()) != null) {
                String[] t = line.split(";");
                db.execSQL("INSERT OR IGNORE INTO precios (codigo, tipo, fecha, precio) VALUES (?, ?, ?, ?)",
                        new Object[]{ Integer.parseInt(t[0].trim()), t[1].trim(), t[2].trim(), Double.parseDouble(t[3].trim()) });
            }
        } catch (Exception e) { mostrarError(e); }
    }

    private void buscar() {
        String codigoStr = edtCodigo.getText().toString().trim();
        tableLayout.removeAllViews();
        if (codigoStr.isEmpty())
            mostrarTodosProductos();
        else
            mostrarProductoConHistorial(Integer.parseInt(codigoStr));
        edtCodigo.setText("");
    }

    // Muestra todos los productos verticalmente con etiquetas
    private void mostrarTodosProductos() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT codigo, descripcion, und, undxenv, linea, existencia FROM productos ORDER BY codigo";
        Cursor c = db.rawQuery(query, null);
        agregarCabecera("Código", "Descripción", "UND", "UNDxENV", "Línea", "Existencia");
        while (c.moveToNext()) {
            agregarFila(
                    c.getString(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getString(4), c.getString(5)
            );
        }
        c.close();
    }

    // Muestra producto en formato vertical y precios ordenados por fecha real
    private void mostrarProductoConHistorial(int codigo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor pc = db.rawQuery("SELECT * FROM productos WHERE codigo=?", new String[]{ String.valueOf(codigo) });
        if (!pc.moveToFirst()) {
            agregarCabecera("No existe producto con ese código");
            pc.close(); return;
        }
        String[] etiquetas = {"Código", "Descripción", "UND", "UNDxENV", "Línea", "Existencia"};
        for (int i = 0; i < 6; i++) {
            agregarFilaEtiqueta(etiquetas[i], pc.getString(i));
        }
        pc.close();

        Cursor pr = db.rawQuery(
                "SELECT tipo, precio, fecha FROM precios WHERE codigo=? " +
                        "ORDER BY substr(fecha,7,4)||'-'||substr(fecha,4,2)||'-'||substr(fecha,1,2) DESC",
                new String[]{ String.valueOf(codigo) }
        );

        if (pr.moveToFirst()) {
            agregarCabecera("Precio actual", "Tipo", "Fecha");
            agregarFila(pr.getString(1), pr.getString(0), pr.getString(2));
            boolean tieneAnteriores = false;
            while (pr.moveToNext()) {
                if (!tieneAnteriores) {
                    agregarCabecera("Precios anteriores");
                    tieneAnteriores = true;
                }
                agregarFilaEtiqueta3("Precio", pr.getString(1),
                        "Tipo", pr.getString(0),
                        "Fecha", pr.getString(2));
            }
        } else {
            agregarCabecera("Sin precios registrados para este producto");
        }
        pr.close();
    }

    private void agregarCabecera(String... textos) { agregarFilaBold(textos); }
    private void agregarFila(String... textos) { agregarFilaNormal(textos); }
    private void agregarFilaBold(String... textos) {
        TableRow row = new TableRow(this);
        for (String t : textos) {
            TextView tv = new TextView(this);
            tv.setText(t); tv.setPadding(8,8,8,8); tv.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(tv);
        }
        tableLayout.addView(row, new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
    private void agregarFilaNormal(String... textos) {
        TableRow row = new TableRow(this);
        for (String t : textos) {
            TextView tv = new TextView(this);
            tv.setText(t); tv.setPadding(8,8,8,8);
            row.addView(tv);
        }
        tableLayout.addView(row, new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
    private void agregarFilaEtiqueta(String label, String valor) {
        TableRow row = new TableRow(this);
        TextView tv1 = new TextView(this);
        TextView tv2 = new TextView(this);
        tv1.setText(label + ": ");
        tv2.setText(valor == null ? "" : valor);
        tv1.setTypeface(null, android.graphics.Typeface.BOLD);
        tv1.setPadding(8,8,8,8);
        tv2.setPadding(8,8,8,8);
        row.addView(tv1);
        row.addView(tv2);
        tableLayout.addView(row);
    }
    private void agregarFilaEtiqueta3(String l1, String v1, String l2, String v2, String l3, String v3) {
        TableRow row = new TableRow(this);
        TextView t1 = new TextView(this);
        TextView t2 = new TextView(this);
        TextView t3 = new TextView(this);
        TextView t4 = new TextView(this);
        TextView t5 = new TextView(this);
        TextView t6 = new TextView(this);
        t1.setText(l1 + ": "); t1.setTypeface(null, android.graphics.Typeface.BOLD);
        t2.setText(v1); t3.setText(l2 + ": "); t3.setTypeface(null, android.graphics.Typeface.BOLD);
        t4.setText(v2); t5.setText(l3 + ": "); t5.setTypeface(null, android.graphics.Typeface.BOLD);
        t6.setText(v3);
        TableRow.LayoutParams lp = new TableRow.LayoutParams();
        t1.setPadding(8,8,8,8); t2.setPadding(8,8,8,8);
        t3.setPadding(8,8,8,8); t4.setPadding(8,8,8,8);
        t5.setPadding(8,8,8,8); t6.setPadding(8,8,8,8);
        row.addView(t1); row.addView(t2);
        row.addView(t3); row.addView(t4);
        row.addView(t5); row.addView(t6);
        tableLayout.addView(row);
    }

    private void dialogoAgregarProducto() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        String[] hints = {"Código", "Descripción", "UND", "UNDXENV", "Línea", "Existencia"};
        EditText[] edits = new EditText[6];
        for (int i = 0; i < 6; i++) {
            edits[i] = new EditText(this);
            edits[i].setHint(hints[i]);
            if (i==0 || i==3 || i==4 || i==5) edits[i].setInputType(InputType.TYPE_CLASS_NUMBER);
            layout.addView(edits[i]);
        }
        new AlertDialog.Builder(this)
                .setTitle("Nuevo producto")
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    try {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.execSQL("INSERT INTO productos (codigo, descripcion, und, undxenv, linea, existencia) VALUES (?, ?, ?, ?, ?, ?)",
                                new Object[] {
                                        Integer.parseInt(edits[0].getText().toString().trim()),
                                        edits[1].getText().toString().trim(),
                                        edits[2].getText().toString().trim(),
                                        Integer.parseInt(edits[3].getText().toString().trim()),
                                        Integer.parseInt(edits[4].getText().toString().trim()),
                                        Integer.parseInt(edits[5].getText().toString().trim())
                                });
                    } catch (Exception e) { mostrarError(e); }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void dialogoAgregarPrecio() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText edtCodigo = new EditText(this);
        edtCodigo.setHint("Código de producto");
        edtCodigo.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText edtTipo = new EditText(this);
        edtTipo.setHint("Tipo de precio ej: A");
        EditText edtFecha = new EditText(this);
        edtFecha.setHint("Fecha (dd/MM/yyyy)");
        EditText edtPrecio = new EditText(this);
        edtPrecio.setHint("Precio");
        edtPrecio.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
        layout.addView(edtCodigo);
        layout.addView(edtTipo);
        layout.addView(edtFecha);
        layout.addView(edtPrecio);
        new AlertDialog.Builder(this)
                .setTitle("Agregar nuevo precio")
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    try {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.execSQL("INSERT INTO precios (codigo, tipo, fecha, precio) VALUES (?, ?, ?, ?)",
                                new Object[] {
                                        Integer.parseInt(edtCodigo.getText().toString().trim()),
                                        edtTipo.getText().toString().trim(),
                                        edtFecha.getText().toString().trim(),
                                        Double.parseDouble(edtPrecio.getText().toString().trim())
                                });
                    } catch (Exception e) { mostrarError(e); }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarError(Exception e) {
        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
    }
}