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
import android.graphics.drawable.GradientDrawable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import android.location.Geocoder;
import android.location.Address;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.Locale;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION = 1;
    private static final String URL_CONDUCTORES = "https://clasespersonales.com/taxis/listacon.php";
    private static final String URL_TAXIS = "https://clasespersonales.com/taxis/";
    private double myLat = 0, myLon = 0;
    FusedLocationProviderClient fusedLocationClient;
    private MapView mapView;
    DBHelper dbHelper; // DBHelper para conductores

    static class Taxi {
        int movil;
        String carnet;
        double lat, lon, dist;
        String nombreConductor;
        Taxi(int m, String c, double la, double lo, double d, String nombre) {
            movil = m; carnet = c; lat = la; lon = lo; dist = d;
            nombreConductor = nombre;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map);
        mapView.setMultiTouchControls(true);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        dbHelper = new DBHelper(this); // inicializa DBHelper

        descargarConductoresDesdeWeb(); // carga conductores al iniciar

        Button btnActualizar = findViewById(R.id.btnActualizar);
        btnActualizar.setOnClickListener(v -> solicitarUbicacion());

        solicitarUbicacion();
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        myLat = location.getLatitude();
                        myLon = location.getLongitude();
                        cargarTaxisYMostrar();
                    } else {
                        mostrarMsg("No se obtuvo tu ubicación. Intenta de nuevo.");
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

    // Descarga lista de conductores directamente del sitio y los guarda en SQLite
    private void descargarConductoresDesdeWeb() {
        try {
            URL url = new URL(URL_CONDUCTORES);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
            reader.close();

            JSONObject obj = new JSONObject(json.toString());
            if (obj.has("conductores")) {
                JSONArray conductoresArray = obj.getJSONArray("conductores");
                for (int i = 0; i < conductoresArray.length(); i++) {
                    JSONObject c = conductoresArray.getJSONObject(i);
                    String carnet = c.getString("carnet").trim();
                    String paterno = c.getString("paterno").trim();
                    String materno = c.getString("materno").trim();
                    String nombres = c.getString("nombres").trim();
                    dbHelper.insertarConductor(carnet, paterno, materno, nombres);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Descarga taxis y cruza con conductores usando la DB
    private List<Taxi> descargarTaxisWeb() {
        List<Taxi> result = new ArrayList<>();
        try {
            URL url = new URL(URL_TAXIS);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder html = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                html.append(line).append("\n");
            }
            in.close();

            String content = html.toString();
            String[] rows = content.split("<tr");

            for (String row : rows) {
                if (!row.contains("<td")) continue;

                try {
                    List<String> valores = new ArrayList<>();
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(">([^<>]+)<");
                    java.util.regex.Matcher matcher = pattern.matcher(row);

                    while (matcher.find()) {
                        String v = matcher.group(1).replace("&nbsp;", "").replace(",", ".").trim();
                        if (!v.isEmpty()) valores.add(v);
                    }
                    if (valores.size() == 4) {
                        int movil = Integer.parseInt(valores.get(0).replaceAll("[^\\d]", ""));
                        String carnetStr = valores.get(1).replaceAll("\\D", "").trim();
                        double lat = Double.parseDouble(valores.get(2));
                        double lon = Double.parseDouble(valores.get(3));
                        String nombre = dbHelper.obtenerNombreConductor(carnetStr);
                        result.add(new Taxi(movil, carnetStr, lat, lon, 0, nombre == null ? "(Sin conductor)" : nombre));
                    }
                } catch (Exception e) {
                    // Ignorar filas con errores
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    void cargarTaxisYMostrar() {
        List<Taxi> taxis = descargarTaxisWeb();
        if (taxis.isEmpty()) {
            mostrarMsg("No se pudo leer taxis de la web.");
            return;
        }

        // Calcular distancias
        for (Taxi t : taxis) {
            t.dist = calcularDistancia(myLat, myLon, t.lat, t.lon);
        }

        // Ordenar por distancia
        Collections.sort(taxis, Comparator.comparingDouble(t -> t.dist));

        String direccion = obtenerDireccion(myLat, myLon);

        runOnUiThread(() -> {
            ((TextView) findViewById(R.id.tvCodigo)).setText("CODE: 349");
            ((TextView) findViewById(R.id.tvDireccion)).setText(
                    "Tu ubicación:\n" + direccion + String.format("\n(%.5f, %.5f)", myLat, myLon));

            TableLayout tablaCercanos = findViewById(R.id.tableCercanos);
            TableLayout tablaResto = findViewById(R.id.tableResto);
            tablaCercanos.removeAllViews();
            tablaResto.removeAllViews();

            // Header para más cercanos
            TableRow header = new TableRow(this);
            addCell(header, "MÓVIL", true, true);
            addCell(header, "CARNET", true, true);
            addCell(header, "NOMBRE", true, true);
            addCell(header, "Distancia(km)", true, true);
            tablaCercanos.addView(header);

            // Los 3 taxis más cercanos
            for (int i = 0; i < 3 && i < taxis.size(); i++) {
                Taxi t = taxis.get(i);
                TableRow row = new TableRow(this);
                addCell(row, String.valueOf(t.movil), false, true);
                addCell(row, t.carnet, false, true);
                addCell(row, t.nombreConductor, false, true);
                addCell(row, String.format("%.2f", t.dist), false, true);
                tablaCercanos.addView(row);
            }

            // Header para lejanos
            if (taxis.size() > 3) {
                TableRow header2 = new TableRow(this);
                addCell(header2, "MÓVIL", true, false);
                addCell(header2, "CARNET", true, false);
                addCell(header2, "NOMBRE", true, false);
                addCell(header2, "Distancia(km)", true, false);
                tablaResto.addView(header2);

                // Los taxis restantes (lejanos)
                for (int i = 3; i < taxis.size(); i++) {
                    Taxi t = taxis.get(i);
                    TableRow row = new TableRow(this);
                    addCell(row, String.valueOf(t.movil), false, false);
                    addCell(row, t.carnet, false, false);
                    addCell(row, t.nombreConductor, false, false);
                    addCell(row, String.format("%.2f", t.dist), false, false);
                    tablaResto.addView(row);
                }
            }

            // ==== MARCADORES EN EL MAPA ====
            mapView.getOverlays().clear();

            // AZUL: tu ubicación
            GeoPoint myGeo = new GeoPoint(myLat, myLon);
            Marker myMarker = new Marker(mapView);
            myMarker.setPosition(myGeo);
            myMarker.setTitle("Tú estás aquí");
            myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            myMarker.setIcon(crearIconoCirculo(Color.BLUE));
            mapView.getOverlays().add(myMarker);

            mapView.getController().setZoom(13.5);
            mapView.getController().setCenter(myGeo);

            // VERDE: taxis más cercanos, MISMO FORMATO que lejanos
            for (int i = 0; i < Math.min(3, taxis.size()); i++) {
                Taxi t = taxis.get(i);
                GeoPoint taxiGeo = new GeoPoint(t.lat, t.lon);
                Marker taxiMarker = new Marker(mapView);
                taxiMarker.setPosition(taxiGeo);
                taxiMarker.setTitle("Taxi " + t.movil + " - " + (t.nombreConductor != null ? t.nombreConductor : "")
                        + "\nDist: " + String.format("%.2f km", t.dist));
                taxiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                taxiMarker.setIcon(crearIconoCirculo(Color.GREEN));
                mapView.getOverlays().add(taxiMarker);
            }

            // NARANJA: lejanos
            for (int i = 3; i < taxis.size(); i++) {
                Taxi t = taxis.get(i);
                GeoPoint taxiGeo = new GeoPoint(t.lat, t.lon);
                Marker taxiMarker = new Marker(mapView);
                taxiMarker.setPosition(taxiGeo);
                taxiMarker.setTitle("Taxi " + t.movil + " - " + (t.nombreConductor != null ? t.nombreConductor : "")
                        + "\nDist: " + String.format("%.2f km", t.dist));
                taxiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                taxiMarker.setIcon(crearIconoCirculo(Color.parseColor("#FF9800")));
                mapView.getOverlays().add(taxiMarker);
            }

            mapView.invalidate();
        });
    }

    private android.graphics.drawable.Drawable crearIconoCirculo(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(40, 40);
        drawable.setStroke(4, Color.WHITE);
        return drawable;
    }

    void mostrarMsg(String msg) {
        runOnUiThread(() -> {
            ((TextView) findViewById(R.id.tvDireccion)).setText(msg);
            ((TableLayout) findViewById(R.id.tableCercanos)).removeAllViews();
            ((TableLayout) findViewById(R.id.tableResto)).removeAllViews();
            if (mapView != null) mapView.getOverlays().clear();
        });
    }

    void addCell(TableRow row, String text, boolean isHeader, boolean destacado) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 4, 8, 4);
        tv.setTextSize(isHeader ? 15 : 14);
        tv.setTypeface(null, isHeader ? Typeface.BOLD : Typeface.NORMAL);
        if (destacado) {
            tv.setTextColor(Color.parseColor("#1B5E20"));
        } else {
            tv.setTextColor(Color.parseColor("#E65100"));
        }
        row.addView(tv);
    }

    public static double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

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
            dir = "Error obteniendo dirección: " + e.getMessage();
        }
        return dir;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
