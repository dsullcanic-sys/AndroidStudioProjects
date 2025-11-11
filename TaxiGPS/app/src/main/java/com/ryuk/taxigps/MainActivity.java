package com.ryuk.taxigps;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.TextView;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.graphics.Typeface;
import android.graphics.Color;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import android.location.Geocoder;
import android.location.Address;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.Locale;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION = 1;
    private static final String URL_TAXIS = "https://clasespersonales.com/taxis/";
    private double myLat = 0, myLon = 0;
    FusedLocationProviderClient fusedLocationClient;

    static class Taxi {
        int movil, carnet;
        double lat, lon, dist;
        Taxi(int m, int c, double la, double lo, double d) {
            movil = m; carnet = c; lat = la; lon = lo; dist = d;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build()); // SOLO PARA DEMO
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Botón actualizar ubicación
        Button btnActualizar = findViewById(R.id.btnActualizar);
        btnActualizar.setOnClickListener(v -> solicitarUbicacion());

        solicitarUbicacion(); // Obtener ubicación y taxis al iniciar
    }

    void solicitarUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            obtenerUbicacion();
        }
    }

    private void obtenerUbicacion() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        myLat = location.getLatitude();
                        myLon = location.getLongitude();
                        cargarTaxisYMostrar();
                    } else {
                        mostrarMsg("No se obtuvo tu ubicación.");
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion();
        } else {
            mostrarMsg("Permiso de ubicación denegado.");
        }
    }

    void cargarTaxisYMostrar() {
        List<Taxi> taxis = descargarTaxisWeb();
        if (taxis.isEmpty()) {
            mostrarMsg("No se pudo leer taxis de la web.");
            return;
        }
        for (Taxi t : taxis) {
            t.dist = calcularDistancia(myLat, myLon, t.lat, t.lon);
        }
        Collections.sort(taxis, Comparator.comparingDouble(t -> t.dist));
        String direccion = obtenerDireccion(myLat, myLon);

        runOnUiThread(() -> {
            // Código arriba (por si quieres modificarlo desde Java)
            ((TextView) findViewById(R.id.tvCodigo)).setText("CODE: 349");

            // Muestra dirección destacada
            ((TextView) findViewById(R.id.tvDireccion)).setText(
                    "Tu ubicación:\n" + direccion + String.format("\n(%.5f, %.5f)", myLat, myLon));

            // Limpia tablas previas
            TableLayout tablaCercanos = findViewById(R.id.tableCercanos);
            TableLayout tablaResto = findViewById(R.id.tableResto);
            tablaCercanos.removeAllViews();
            tablaResto.removeAllViews();

            // Header de tabla para cercanos
            TableRow header = new TableRow(this);
            addCell(header, "MÓVIL", true, true);
            addCell(header, "CARNET", true, true);
            addCell(header, "Distancia(km)", true, true);
            tablaCercanos.addView(header);

            // Taxis más cercanos
            for (int i = 0; i < 3 && i < taxis.size(); i++) {
                Taxi t = taxis.get(i);
                TableRow row = new TableRow(this);
                addCell(row, String.valueOf(t.movil), false, false);
                addCell(row, String.valueOf(t.carnet), false, false);
                addCell(row, String.format("%.2f", t.dist), false, true);
                tablaCercanos.addView(row);
            }

            // Header de tabla para resto
            TableRow header2 = new TableRow(this);
            addCell(header2, "MÓVIL", true, false);
            addCell(header2, "CARNET", true, false);
            addCell(header2, "Distancia(km)", true, false);
            tablaResto.addView(header2);

            // Taxis restantes
            for (int i = 3; i < taxis.size(); i++) {
                Taxi t = taxis.get(i);
                TableRow row = new TableRow(this);
                addCell(row, String.valueOf(t.movil), false, false);
                addCell(row, String.valueOf(t.carnet), false, false);
                addCell(row, String.format("%.2f", t.dist), false, false);
                tablaResto.addView(row);
            }
        });
    }

    void mostrarMsg(String msg) {
        runOnUiThread(() -> {
            ((TextView) findViewById(R.id.tvDireccion)).setText(msg);
            ((TableLayout) findViewById(R.id.tableCercanos)).removeAllViews();
            ((TableLayout) findViewById(R.id.tableResto)).removeAllViews();
        });
    }

    // Ayudante para crear celdas en una fila de tabla
    void addCell(TableRow row, String text, boolean isHeader, boolean destacado) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 4, 8, 4);
        tv.setTextSize(isHeader ? 15 : 14);
        tv.setTypeface(null, isHeader ? Typeface.BOLD : Typeface.NORMAL);
        if (destacado) tv.setTextColor(Color.parseColor("#1B5E20"));
        row.addView(tv);
    }

    // Descarga y parsea la tabla de la web correctamente como HTML
    private List<Taxi> descargarTaxisWeb() {
        List<Taxi> result = new ArrayList<>();
        try {
            URL url = new URL(URL_TAXIS);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder html = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                html.append(line);
            }
            in.close();

            // Buscar filas <tr>
            String table = html.toString();
            String[] rows = table.split("<tr>");
            for (String row : rows) {
                if (row.contains("<td") || row.contains("<th")) {
                    List<String> valores = new ArrayList<>();
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile(">([^<]+)<").matcher(row);
                    while (m.find()) {
                        valores.add(m.group(1).replace("&nbsp;", "").trim());
                    }
                    if (valores.size() == 4) {
                        try {
                            int movil = Integer.parseInt(valores.get(0).replaceAll("[^\\d]", ""));
                            int carnet = Integer.parseInt(valores.get(1).replaceAll("[^\\d]", ""));
                            double lat = Double.parseDouble(valores.get(2).replace(',', '.'));
                            double lon = Double.parseDouble(valores.get(3).replace(',', '.'));
                            result.add(new Taxi(movil, carnet, lat, lon, 0));
                        } catch (Exception ign) {}
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // Distancia Haversine
    public static double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Geocoder: obtiene dirección legible
    private String obtenerDireccion(double lat, double lon) {
        String dir = "";
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (address.getThoroughfare() != null) sb.append(address.getThoroughfare()).append(", ");
                if (address.getSubLocality() != null) sb.append(address.getSubLocality()).append(", ");
                if (address.getLocality() != null) sb.append(address.getLocality()).append(", ");
                if (address.getCountryName() != null) sb.append(address.getCountryName());
                dir = sb.toString();
            } else {
                dir = "Dirección no encontrada";
            }
        } catch (Exception e) {
            dir = "Error obteniendo dirección";
        }
        return dir;
    }
}
