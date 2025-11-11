package com.ryuk.lectura;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ContentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        String uriString = getIntent().getStringExtra("fileUri");
        if (uriString == null) {
            Toast.makeText(this, "Error: No se recibió archivo", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            BufferedReader lee = new BufferedReader(new InputStreamReader(
                    getContentResolver().openInputStream(Uri.parse(uriString)), "ISO-8859-1"));

            ListView lista = findViewById(R.id.listView1);
            TextView tv = findViewById(R.id.textView4);
            ArrayList<ItemArchivo> items = new ArrayList<>();
            StringBuilder buffer = new StringBuilder();
            String currentTitulo = null;

            String linea;
            while ((linea = lee.readLine()) != null) {
                if (linea.startsWith("XYZ")) {
                    addItem(items, currentTitulo, buffer);
                    currentTitulo = linea.replace("XYZ", "");
                    buffer.setLength(0);
                } else if (linea.startsWith("YYY")) {
                    addItem(items, currentTitulo, buffer);
                    currentTitulo = "   " + linea.replace("YYY", "");
                    buffer.setLength(0);
                } else if (linea.startsWith("ZZZ")) {
                    addItem(items, currentTitulo, buffer);
                    currentTitulo = "      " + linea.replace("ZZZ", "");
                    buffer.setLength(0);
                } else {
                    buffer.append(linea).append("\n");
                }
            }
            addItem(items, currentTitulo, buffer);
            lee.close();

            if (items.isEmpty()) {
                Toast.makeText(this, "Archivo vacío", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
            lista.setOnItemClickListener((parent, view, position, id) ->
                    tv.setText(items.get(position).contenido));

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void addItem(ArrayList<ItemArchivo> items, String titulo, StringBuilder buffer) {
        if (titulo != null) {
            items.add(new ItemArchivo(titulo, buffer.toString().trim()));
        }
    }

    class ItemArchivo {
        String titulo, contenido;
        ItemArchivo(String t, String c) {
            titulo = t;
            contenido = c;
        }
        @Override
        public String toString() { return titulo; }
    }
}