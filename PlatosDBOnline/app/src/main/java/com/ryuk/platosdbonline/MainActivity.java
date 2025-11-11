package com.ryuk.platosdbonline;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.squareup.picasso.Picasso;
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
    EditText searchNombre;
    Button btnBuscar;
    ArrayList<JSONObject> platos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        contenedorPrincipal = findViewById(R.id.contenedorPrincipal);
        searchNombre = findViewById(R.id.searchNombre);
        btnBuscar = findViewById(R.id.btnBuscar);

        new cargaURL().execute("https://raw.githubusercontent.com/dsullcanic-sys/INF-325-Programacion-Virtual/refs/heads/main/15.%20PlatosDBOnline/Platos.json");

        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombreBuscado = searchNombre.getText().toString().trim();
                if(!nombreBuscado.isEmpty()){
                    ArrayList<JSONObject> resultados = buscarPorNombre(nombreBuscado);
                    if(resultados.size() > 0){
                        mostrarVistaDetalle(resultados);
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
            JSONArray arr = objeto.getJSONArray("platos");
            platos.clear();
            for(int i=0;i<arr.length();i++){
                platos.add(arr.getJSONObject(i));
            }
            mostrarTablaTodos();
        } catch (Exception e) {
            mostrarNoEncontrado();
        }
    }

    // Vista de tabla simple (sin imágenes) para mostrar todos
    private void mostrarTablaTodos(){
        contenedorPrincipal.removeAllViews();

        TableLayout tableLayout = new TableLayout(this);
        tableLayout.setStretchAllColumns(true);

        // Cabecera
        TableRow cabecera = new TableRow(this);
        agregarCeldaCabecera(cabecera, "Nombre");
        agregarCeldaCabecera(cabecera, "Precio (Bs)");
        tableLayout.addView(cabecera);

        for (JSONObject p : platos) {
            TableRow row = new TableRow(this);
            agregarCeldaDato(row, p.optString("nombre",""));
            agregarCeldaDato(row, String.valueOf(p.optInt("precio", 0)));
            tableLayout.addView(row);
        }

        contenedorPrincipal.addView(tableLayout);
    }

    // Vista de detalle con imágenes (cuando se busca)
    private void mostrarVistaDetalle(ArrayList<JSONObject> resultados){
        contenedorPrincipal.removeAllViews();

        for (JSONObject p : resultados) {
            // Contenedor para cada plato (tipo tarjeta)
            LinearLayout tarjeta = new LinearLayout(this);
            tarjeta.setOrientation(LinearLayout.VERTICAL);
            tarjeta.setPadding(20, 20, 20, 20);
            tarjeta.setGravity(Gravity.CENTER);

            // Imagen
            ImageView img = new ImageView(this);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,  // Ancho: ocupa toda la pantalla
                    600  // Alto fijo en 600px (puedes ajustar)
            );
            imgParams.setMargins(20, 10, 20, 15);  // Márgenes: izq, arriba, der, abajo
            img.setLayoutParams(imgParams);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);

            String urlImagen = p.optString("imagen","");
            if(!urlImagen.isEmpty()){
                Picasso.get()
                        .load(urlImagen)
                        .fit()  // Ajusta al tamaño del ImageView
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_close_clear_cancel)
                        .into(img);
            }
            tarjeta.addView(img);

            // Nombre
            TextView tvNombre = new TextView(this);
            tvNombre.setText(p.optString("nombre",""));
            tvNombre.setTextSize(20);
            tvNombre.setTypeface(null, Typeface.BOLD);
            tvNombre.setGravity(Gravity.CENTER);
            tarjeta.addView(tvNombre);

            // Precio
            TextView tvPrecio = new TextView(this);
            tvPrecio.setText("Bs. " + p.optInt("precio", 0));
            tvPrecio.setTextSize(18);
            tvPrecio.setGravity(Gravity.CENTER);
            tvPrecio.setPadding(0, 10, 0, 20);
            tarjeta.addView(tvPrecio);

            // Línea divisoria
            View linea = new View(this);
            linea.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 2));
            linea.setBackgroundColor(0xFFCCCCCC);
            tarjeta.addView(linea);

            contenedorPrincipal.addView(tarjeta);
        }
    }

    private void mostrarNoEncontrado(){
        contenedorPrincipal.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("No se encontraron resultados");
        tv.setTextSize(18);
        tv.setPadding(20, 20, 20, 20);
        tv.setGravity(Gravity.CENTER);
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

    private ArrayList<JSONObject> buscarPorNombre(String nombre){
        ArrayList<JSONObject> resultados = new ArrayList<>();
        for(JSONObject p : platos){
            String nombrePlato = p.optString("nombre","").toLowerCase();
            if(nombrePlato.contains(nombre.toLowerCase())){
                resultados.add(p);
            }
        }
        return resultados;
    }
}
