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
    private static final String URL_TAXIS = "https://clasespersonales.com/taxis/";
    private static final String JSON_CONDUCTORES =
            "{\"conductores\":[{\"carnet\":\"6144341\",\"paterno\":\"RODAS\",\"materno\":\"AGUIRRE\",\"nombres\":\"ALEJANDRO AARON\"},{\"carnet\":\"5998313\",\"paterno\":\"QUISPE\",\"materno\":\"AJNO\",\"nombres\":\"ERICK\"},{\"carnet\":\"6869689\",\"paterno\":\"BORDA\",\"materno\":\"ALABY\",\"nombres\":\"JAVIER ALEJANDRO\"},{\"carnet\":\"8321673\",\"paterno\":\"ZAPATA\",\"materno\":\"ALANOCA\",\"nombres\":\"ELMER SHAMIR\"},{\"carnet\":\"6853401\",\"paterno\":\"RAMOS\",\"materno\":\"ALI\",\"nombres\":\"JUAN ROLANDO\"},{\"carnet\":\"6905510\",\"paterno\":\"YUJRA\",\"materno\":\"ALI\",\"nombres\":\"ADALID IRVIN\"},{\"carnet\":\"6764063\",\"paterno\":\"RADA\",\"materno\":\"ARANIBAR\",\"nombres\":\"JAIME JUNIOR\"},{\"carnet\":\"6755399\",\"paterno\":\"TERRAZAS\",\"materno\":\"BUSTAMANTE\",\"nombres\":\"ELIO RODO\"},{\"carnet\":\"6095371\",\"paterno\":\"DAZA\",\"materno\":\"CHUQUIMIA\",\"nombres\":\"LUIS FERNANDO\"},{\"carnet\":\"2622011\",\"paterno\":\"CORO\",\"materno\":\"CONDE\",\"nombres\":\"HECTOR MOISES\"},{\"carnet\":\"5966709\",\"paterno\":\"ZARCO\",\"materno\":\"CONDE\",\"nombres\":\"JUAN FERNANDO\"},{\"carnet\":\"6765608\",\"paterno\":\"AYALA\",\"materno\":\"CORTEZ\",\"nombres\":\"RODRIGO GABRIEL\"},{\"carnet\":\"6009256\",\"paterno\":\"USNAYO\",\"materno\":\"FALGA\",\"nombres\":\"JAIME\"},{\"carnet\":\"4312014\",\"paterno\":\"GARCIA\",\"materno\":\"FERNANDEZ\",\"nombres\":\"ALVARO CARLOS\"},{\"carnet\":\"6759045\",\"paterno\":\"CONTRERAS\",\"materno\":\"GALLEGOS\",\"nombres\":\"MIGUEL ADALID\"},{\"carnet\":\"6030223\",\"paterno\":\"ASPIAZU\",\"materno\":\"GUTIERREZ\",\"nombres\":\"ALVARO ADALID\"},{\"carnet\":\"4856270\",\"paterno\":\"LARA\",\"materno\":\"HUANCA\",\"nombres\":\"JASMANY MILTON\"},{\"carnet\":\"6833022\",\"paterno\":\"CORONEL\",\"materno\":\"LAZO\",\"nombres\":\"REYNALDO\"},{\"carnet\":\"6824136\",\"paterno\":\"CORONEL\",\"materno\":\"LIMA\",\"nombres\":\"ELMER\"},{\"carnet\":\"4758753\",\"paterno\":\"SALAZAR\",\"materno\":\"LIMACHI\",\"nombres\":\"EDWIN\"},{\"carnet\":\"4830306\",\"paterno\":\"VELASQUEZ\",\"materno\":\"LUIS\",\"nombres\":\"RENATO\"},{\"carnet\":\"6888435\",\"paterno\":\"CHINO\",\"materno\":\"MAMANI\",\"nombres\":\"MIGUEL ANGEL\"},{\"carnet\":\"6837306\",\"paterno\":\"COPA\",\"materno\":\"MAMANI\",\"nombres\":\"GHILMAR EMIGDIO\"},{\"carnet\":\"6798702\",\"paterno\":\"HUANCA\",\"materno\":\"MAMANI\",\"nombres\":\"DANIEL REYNALDO\"},{\"carnet\":\"6169847\",\"paterno\":\"QUISPE\",\"materno\":\"MAMANI\",\"nombres\":\"ALVARO\"},{\"carnet\":\"4837464\",\"paterno\":\"QUISBERT\",\"materno\":\"MENDOZA\",\"nombres\":\"VICTOR VLADIMIR\"},{\"carnet\":\"6948542\",\"paterno\":\"CUSSI\",\"materno\":\"MERLO\",\"nombres\":\"HECTOR ALBERTO\"},{\"carnet\":\"8441626\",\"paterno\":\"BARRA\",\"materno\":\"PAREDES\",\"nombres\":\"DIEGO ADRIAN\"},{\"carnet\":\"8306245\",\"paterno\":\"POMA\",\"materno\":\"PEREZ\",\"nombres\":\"VLADIMIR GERARDO\"},{\"carnet\":\"4928737\",\"paterno\":\"AGNE\",\"materno\":\"QUISPE\",\"nombres\":\"EDDY RICHARD\"},{\"carnet\":\"4760216\",\"paterno\":\"APAZA\",\"materno\":\"QUISPE\",\"nombres\":\"JORGE RONY\"},{\"carnet\":\"6826200\",\"paterno\":\"CHOQUE\",\"materno\":\"QUISPE\",\"nombres\":\"YRVIN ELIAS\"},{\"carnet\":\"5977396\",\"paterno\":\"ZULETA\",\"materno\":\"QUISPE\",\"nombres\":\"CRISTOBAL\"},{\"carnet\":\"6776752\",\"paterno\":\"MAMANI\",\"materno\":\"RODRIGUEZ\",\"nombres\":\"DAMIAN\"},{\"carnet\":\"6765381\",\"paterno\":\"SANZ GUERRERO\",\"materno\":\"SELAEZ\",\"nombres\":\"GUERY\"},{\"carnet\":\"6736701\",\"paterno\":\"TAPIA\",\"materno\":\"TIRINA\",\"nombres\":\"JHOEL DAVID\"},{\"carnet\":\"6150980\",\"paterno\":\"CONDORI\",\"materno\":\"TOPOCO\",\"nombres\":\"RUDDY\"},{\"carnet\":\"6745690\",\"paterno\":\"MAMANI\",\"materno\":\"UTURUNCO\",\"nombres\":\"MIGUEL\"},{\"carnet\":\"6720620\",\"paterno\":\"CUELLAR\",\"materno\":\"VALENCIA\",\"nombres\":\"CLAUDIO ADHEMAR\"},{\"carnet\":\"4875775\",\"paterno\":\"IRIARTE\",\"materno\":\"VALENZUELA\",\"nombres\":\"MAURICIO FELIX\"}],\"exito\":1}";
    private double myLat = 0, myLon = 0;
    FusedLocationProviderClient fusedLocationClient;
    private MapView mapView;
    private Map<String, String> conductoresMap = new HashMap<>();

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

    // Descarga lista de conductores desde JSON local
    private void descargarConductores() {
        conductoresMap.clear();
        try {
            JSONObject obj = new JSONObject(JSON_CONDUCTORES);
            if (obj.has("conductores")) {
                JSONArray conductoresArray = obj.getJSONArray("conductores");
                for (int i = 0; i < conductoresArray.length(); i++) {
                    JSONObject c = conductoresArray.getJSONObject(i);
                    String carnet = c.getString("carnet").trim();
                    String nombreCompleto = c.getString("nombres").trim()
                            + " " + c.getString("paterno").trim()
                            + " " + c.getString("materno").trim();
                    conductoresMap.put(carnet, nombreCompleto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Descarga taxis (carnet como String) y cruza con JSON local
    private List<Taxi> descargarTaxisWeb() {
        descargarConductores();
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
                        String nombre = conductoresMap.get(carnetStr);
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

            // Header para los más cercanos (VERDE)
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

            // Header para lejanos (NARANJA)
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

            // AZUL: tu ubicación (usuario)
            GeoPoint myGeo = new GeoPoint(myLat, myLon);
            Marker myMarker = new Marker(mapView);
            myMarker.setPosition(myGeo);
            myMarker.setTitle("Tú estás aquí");
            myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            myMarker.setIcon(crearIconoCirculo(Color.BLUE)); // AZUL
            mapView.getOverlays().add(myMarker);

            mapView.getController().setZoom(13.5);
            mapView.getController().setCenter(myGeo);

            // VERDE: los 3 taxis más cercanos
            for (int i = 0; i < Math.min(3, taxis.size()); i++) {
                Taxi t = taxis.get(i);
                GeoPoint taxiGeo = new GeoPoint(t.lat, t.lon);
                Marker taxiMarker = new Marker(mapView);
                taxiMarker.setPosition(taxiGeo);
                taxiMarker.setTitle((t.nombreConductor != null ? t.nombreConductor : "")
                        + "\nDist: " + String.format("%.2f km", t.dist));
                taxiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                taxiMarker.setIcon(crearIconoCirculo(Color.GREEN)); // VERDE
                mapView.getOverlays().add(taxiMarker);
            }

            // NARANJA: los demás (lejanos)
            for (int i = 3; i < taxis.size(); i++) {
                Taxi t = taxis.get(i);
                GeoPoint taxiGeo = new GeoPoint(t.lat, t.lon);
                Marker taxiMarker = new Marker(mapView);
                taxiMarker.setPosition(taxiGeo);
                taxiMarker.setTitle("Taxi " + t.movil + " - " + (t.nombreConductor != null ? t.nombreConductor : "")
                        + "\nDist: " + String.format("%.2f km", t.dist));
                taxiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                taxiMarker.setIcon(crearIconoCirculo(Color.parseColor("#FF9800"))); // NARANJA
                mapView.getOverlays().add(taxiMarker);
            }

            mapView.invalidate();
        });
    }

    private android.graphics.drawable.Drawable crearIconoCirculo(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(40, 40); // Tamaño del círculo
        drawable.setStroke(4, Color.WHITE); // Borde blanco
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
            tv.setTextColor(Color.parseColor("#1B5E20")); // Verde oscuro
        } else {
            tv.setTextColor(Color.parseColor("#E65100")); // Naranja oscuro
        }
        row.addView(tv);
    }

    public static double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Radio de la Tierra en km
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
