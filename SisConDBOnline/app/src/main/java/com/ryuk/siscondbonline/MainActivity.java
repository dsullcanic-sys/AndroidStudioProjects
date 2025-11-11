package com.ryuk.siscondbonline;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    LinearLayout contenedorPrincipal;
    EditText searchCarnet;
    Button btnBuscar;
    ArrayList<JSONObject> conductores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        contenedorPrincipal = findViewById(R.id.contenedorPrincipal);
        searchCarnet = findViewById(R.id.searchCarnet);
        btnBuscar = findViewById(R.id.btnBuscar);

        new cargaURL().execute("https://clasespersonales.com/taxis/listacon.php");

        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String carnetBuscado = searchCarnet.getText().toString().trim();
                if(!carnetBuscado.isEmpty()){
                    JSONObject conductor = buscarPorCarnet(carnetBuscado);
                    if(conductor != null){
                        mostrarVistaDetalle(conductor);
                    } else {
                        mostrarNoEncontrado();
                    }
                } else {
                    mostrarTablaTodos();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    class cargaURL extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... arg0){
            return BajarDatos(arg0[0]);
        }
        @Override
        protected void onPostExecute(String resultado){
            procesarJSON(resultado);
        }
    }

    public String BajarDatos(String url){
        InputStream pt = null;
        StringBuilder r = new StringBuilder();
        try{
            URL sitio = new URL(url);
            HttpURLConnection uc = (HttpURLConnection) sitio.openConnection();
            uc.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36");
            uc.setRequestProperty("Accept-Language", "es-ES,es;q=0.9,en;q=0.8");
            uc.setRequestProperty("Accept", "application/json");
            uc.setRequestProperty("Connection", "keep-alive");
            uc.setConnectTimeout(10000);
            uc.setReadTimeout(10000);
            pt = uc.getInputStream();
            if(pt != null){
                BufferedReader br = new BufferedReader(new InputStreamReader(pt, "UTF-8"));
                String linea;
                while ((linea = br.readLine()) != null) {
                    r.append(linea);
                }
                br.close();
            }
        }catch(Exception e){
            r.append("Error: ").append(e.getMessage());
        }
        return r.toString();
    }

    private void procesarJSON(String jsonStr){
        try {
            JSONObject objeto = new JSONObject(jsonStr);
            JSONArray arr = objeto.getJSONArray("conductores");
            conductores.clear();
            for(int i=0;i<arr.length();i++){
                conductores.add(arr.getJSONObject(i));
            }
            mostrarTablaTodos();
        } catch (Exception e) {
            mostrarNoEncontrado();
        }
    }

    // Vista de tabla simple para mostrar todos
    private void mostrarTablaTodos(){
        contenedorPrincipal.removeAllViews();

        TableLayout tableLayout = new TableLayout(this);
        tableLayout.setStretchAllColumns(true);

        // Cabecera
        TableRow cabecera = new TableRow(this);
        agregarCeldaCabecera(cabecera, "Carnet");
        agregarCeldaCabecera(cabecera, "Paterno");
        agregarCeldaCabecera(cabecera, "Materno");
        agregarCeldaCabecera(cabecera, "Nombres");
        tableLayout.addView(cabecera);

        for (JSONObject c : conductores) {
            TableRow row = new TableRow(this);
            agregarCeldaDato(row, c.optString("carnet",""));
            agregarCeldaDato(row, c.optString("paterno",""));
            agregarCeldaDato(row, c.optString("materno",""));
            agregarCeldaDato(row, c.optString("nombres",""));
            tableLayout.addView(row);
        }

        contenedorPrincipal.addView(tableLayout);
    }

    // Vista de detalle (cuando se busca por carnet)
    private void mostrarVistaDetalle(JSONObject conductor){
        contenedorPrincipal.removeAllViews();

        // Contenedor tipo tarjeta
        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(30, 30, 30, 30);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(20, 20, 20, 20);
        tarjeta.setLayoutParams(params);

        // Título "INFORMACIÓN DEL CONDUCTOR"
        TextView titulo = new TextView(this);
        titulo.setText("INFORMACIÓN");
        titulo.setTextSize(22);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setGravity(Gravity.CENTER);
        titulo.setPadding(0, 0, 0, 20);
        titulo.setTextColor(0xFF1565C0); // Azul
        tarjeta.addView(titulo);

        // Espacio
        agregarEspacio(tarjeta, 20);

        // Carnet (destacado)
        agregarCampoDestacado(tarjeta, "CARNET:", conductor.optString("carnet",""));

        agregarEspacio(tarjeta, 15);

        // Apellido Paterno
        agregarCampo(tarjeta, "Ap. Paterno:", conductor.optString("paterno",""));

        // Apellido Materno
        agregarCampo(tarjeta, "Ap. Materno:", conductor.optString("materno",""));

        // Nombres
        agregarCampo(tarjeta, "Nombres:", conductor.optString("nombres",""));

        contenedorPrincipal.addView(tarjeta);
    }

    // Método auxiliar para agregar campo destacado (carnet)
    private void agregarCampoDestacado(LinearLayout contenedor, String etiqueta, String valor) {
        LinearLayout campo = new LinearLayout(this);
        campo.setOrientation(LinearLayout.VERTICAL);
        campo.setGravity(Gravity.CENTER);
        campo.setPadding(15, 15, 15, 15);

        TextView tvEtiqueta = new TextView(this);
        tvEtiqueta.setText(etiqueta);
        tvEtiqueta.setTextSize(16);
        tvEtiqueta.setTypeface(null, Typeface.BOLD);
        tvEtiqueta.setGravity(Gravity.CENTER);
        campo.addView(tvEtiqueta);

        TextView tvValor = new TextView(this);
        tvValor.setText(valor);
        tvValor.setTextSize(28);
        tvValor.setTypeface(null, Typeface.BOLD);
        tvValor.setGravity(Gravity.CENTER);
        tvValor.setPadding(0, 5, 0, 0);
        campo.addView(tvValor);

        contenedor.addView(campo);
    }

    // Método auxiliar para agregar campo normal
    private void agregarCampo(LinearLayout contenedor, String etiqueta, String valor) {
        LinearLayout campo = new LinearLayout(this);
        campo.setOrientation(LinearLayout.HORIZONTAL);
        campo.setPadding(10, 8, 10, 8);

        TextView tvEtiqueta = new TextView(this);
        tvEtiqueta.setText(etiqueta);
        tvEtiqueta.setTextSize(16);
        tvEtiqueta.setTypeface(null, Typeface.BOLD);
        tvEtiqueta.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f));
        campo.addView(tvEtiqueta);

        TextView tvValor = new TextView(this);
        tvValor.setText(valor);
        tvValor.setTextSize(16);
        tvValor.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f));
        campo.addView(tvValor);

        contenedor.addView(campo);
    }

    // Método auxiliar para agregar espacio
    private void agregarEspacio(LinearLayout contenedor, int altura) {
        View espacio = new View(this);
        espacio.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, altura));
        contenedor.addView(espacio);
    }

    private void mostrarNoEncontrado(){
        contenedorPrincipal.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("No se encontró el conductor con ese carnet");
        tv.setTextSize(18);
        tv.setPadding(20, 20, 20, 20);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(0xFFD32F2F); // Rojo
        contenedorPrincipal.addView(tv);
    }

    private void agregarCeldaCabecera(TableRow row, String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(20,10,20,10);
        tv.setTypeface(null, Typeface.BOLD);
        row.addView(tv);
    }

    private void agregarCeldaDato(TableRow row, String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(20,10,20,10);
        row.addView(tv);
    }

    private JSONObject buscarPorCarnet(String carnet){
        for(JSONObject c : conductores){
            if(c.optString("carnet","").equals(carnet)){
                return c;
            }
        }
        return null;
    }
}
