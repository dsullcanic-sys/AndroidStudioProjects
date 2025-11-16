package com.ryuk.taxifb;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.EditText;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Geocoder;
import android.location.Address;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.Locale;

public class ClienteActivity extends AppCompatActivity {
    private static final String TAG = "ClienteActivity";
    private static final int REQUEST_LOCATION = 1;
    private double myLat = 0, myLon = 0;
    FusedLocationProviderClient fusedLocationClient;
    private MapView mapView;
    private FirebaseFirestore dbF;
    private Map<String, Conductor> conductoresMap = new HashMap<>();
    private Set<Integer> taxisReservados = new HashSet<>(); // Taxis con reserva activa

    static class Conductor {
        String carnet;
        String nombre;
        String apellido;
        String nombreCompleto() {
            return nombre + " " + apellido;
        }
    }

    static class Taxi {
        int movil;
        String carnet;
        double lat, lon, dist;
        String nombreConductor;
        boolean reservado; // Si tiene reserva activa
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_cliente);

        mapView = findViewById(R.id.mapContainer);
        mapView.setMultiTouchControls(true);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        FirebaseApp.initializeApp(this);
        dbF = FirebaseFirestore.getInstance();

        Button btnActualizar = findViewById(R.id.btnActualizar);
        btnActualizar.setOnClickListener(v -> actualizarDatos());

        // CARGAR DATOS INICIAL
        cargarDatosIniciales();
    }

    // ========== CARGA INICIAL ==========
    private void cargarDatosIniciales() {
        if (MainActivity.datosListos && MainActivity.ubicacionLat != 0) {
            myLat = MainActivity.ubicacionLat;
            myLon = MainActivity.ubicacionLon;
            Log.d(TAG, "Usando ubicación de MainActivity");
            cargarReservasActivas();
        } else {
            mostrarMsg("Obteniendo ubicación...");
            solicitarUbicacion();
        }
    }

    // ========== ACTUALIZAR DATOS ==========
    private void actualizarDatos() {
        mostrarMsg("Actualizando...");
        obtenerUbicacionYActualizarTaxis();
    }

    private void obtenerUbicacionYActualizarTaxis() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            mostrarMsg("Permiso de ubicación requerido");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        myLat = location.getLatitude();
                        myLon = location.getLongitude();
                        Log.d(TAG, "Ubicación actualizada");
                        actualizarTaxisDesdeWeb();
                    } else {
                        mostrarMsg("No se obtuvo ubicación");
                        actualizarTaxisDesdeWeb();
                    }
                });
    }

    private void actualizarTaxisDesdeWeb() {
        mostrarMsg("Borrando taxis antiguos...");
        dbF.collection("taxis")
                .get()
                .addOnSuccessListener(result -> {
                    for (QueryDocumentSnapshot doc : result) {
                        dbF.collection("taxis").document(doc.getId()).delete();
                    }
                    descargarTaxisActuales();
                });
    }

    private void descargarTaxisActuales() {
        new Thread(() -> {
            try {
                URL url = new URL("https://clasespersonales.com/taxis/");
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuilder html = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) html.append(line).append("\n");
                reader.close();

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

                        if (valores.size() >= 4) {
                            int movil = Integer.parseInt(valores.get(0).replaceAll("[^\\d]", ""));
                            String carnet = valores.get(1).replaceAll("\\D", "").trim();
                            double lat = Double.parseDouble(valores.get(2));
                            double lon = Double.parseDouble(valores.get(3));

                            Map<String, Object> taxiData = new HashMap<>();
                            taxiData.put("movil", movil);
                            taxiData.put("carnet", carnet);
                            taxiData.put("lat", lat);
                            taxiData.put("lon", lon);

                            dbF.collection("taxis")
                                    .document(String.valueOf(movil))
                                    .set(taxiData);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando fila: " + e.getMessage());
                    }
                }

                runOnUiThread(() -> {
                    mostrarMsg("Taxis actualizados. Recargando...");
                    new android.os.Handler().postDelayed(() -> {
                        cargarReservasActivas();
                    }, 1000);
                });

            } catch (Exception e) {
                runOnUiThread(() -> mostrarMsg("Error actualizando taxis"));
            }
        }).start();
    }

    // ========== UBICACIÓN GPS ==========
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
                        cargarReservasActivas();
                    } else {
                        mostrarMsg("No se obtuvo ubicación");
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
            mostrarMsg("Permiso denegado");
        }
    }

    // ========== CARGAR RESERVAS ACTIVAS ==========
    private void cargarReservasActivas() {
        mostrarMsg("Verificando reservas activas...");
        dbF.collection("reservas")
                .whereIn("estado", Arrays.asList("Pendiente", "En Curso"))
                .get()
                .addOnCompleteListener(task -> {
                    taxisReservados.clear();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Long taxiMovil = doc.getLong("taxi_movil");
                            if (taxiMovil != null) {
                                taxisReservados.add(taxiMovil.intValue());
                                Log.d(TAG, "Taxi " + taxiMovil + " está reservado");
                            }
                        }
                        Log.d(TAG, "Total taxis reservados: " + taxisReservados.size());
                        cargarConductoresYTaxis();
                    } else {
                        mostrarMsg("Error verificando reservas");
                    }
                });
    }

    // ========== CARGAR CONDUCTORES Y TAXIS ==========
    private void cargarConductoresYTaxis() {
        mostrarMsg("Cargando conductores...");
        dbF.collection("conductores")
                .get()
                .addOnCompleteListener(task -> {
                    conductoresMap.clear();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Conductor c = new Conductor();
                            c.carnet = doc.getString("carnet");
                            c.nombre = doc.getString("nombre");
                            c.apellido = doc.getString("apellido");

                            String carnetLimpio = c.carnet != null ? c.carnet.replaceAll("\\D", "").trim() : "";
                            conductoresMap.put(carnetLimpio, c);
                        }
                        cargarTaxisDesdeFirebase();
                    } else {
                        mostrarMsg("Error cargando conductores");
                    }
                });
    }

    private void cargarTaxisDesdeFirebase() {
        mostrarMsg("Cargando taxis...");
        dbF.collection("taxis")
                .get()
                .addOnCompleteListener(task -> {
                    List<Taxi> taxis = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Taxi t = new Taxi();
                            t.movil = doc.getLong("movil") != null ? doc.getLong("movil").intValue() : 0;
                            t.carnet = doc.getString("carnet") != null ? doc.getString("carnet").replaceAll("\\D", "").trim() : "";
                            t.lat = doc.getDouble("lat") != null ? doc.getDouble("lat") : 0;
                            t.lon = doc.getDouble("lon") != null ? doc.getDouble("lon") : 0;
                            t.dist = calcularDistancia(myLat, myLon, t.lat, t.lon);

                            // Corrección: Ahora considera también el campo 'reservado' del taxi
                            Boolean reservadoCampo = doc.getBoolean("reservado");
                            t.reservado = taxisReservados.contains(t.movil)
                                    || (reservadoCampo != null && reservadoCampo);

                            Conductor conductor = conductoresMap.get(t.carnet);
                            if (conductor != null) {
                                t.nombreConductor = conductor.nombreCompleto();
                            } else {
                                t.nombreConductor = "(Sin conductor)";
                            }

                            taxis.add(t);
                        }
                        Collections.sort(taxis, Comparator.comparingDouble(tx -> tx.dist));
                        mostrarTaxisConBoton(taxis);
                    } else {
                        mostrarMsg("Error cargando taxis");
                    }
                });
    }

    // ========== MOSTRAR TAXIS EN TABLAS ==========
    void mostrarTaxisConBoton(List<Taxi> taxis) {
        String direccion = obtenerDireccion(myLat, myLon);

        runOnUiThread(() -> {
            // Solo mostrar dirección (sin CODE)
            ((TextView) findViewById(R.id.tvDireccion)).setText(
                    direccion + String.format(" (%.5f, %.5f)", myLat, myLon));

            TableLayout tablaCercanos = findViewById(R.id.tableCercanos);
            TableLayout tablaResto = findViewById(R.id.tableResto);
            tablaCercanos.removeAllViews();
            tablaResto.removeAllViews();

            // Header tabla cercanos
            TableRow header = new TableRow(this);
            addCell(header, "MÓVIL", true, true);
            addCell(header, "CARNET", true, true);
            addCell(header, "NOMBRE", true, true);
            addCell(header, "Dist(km)", true, true);
            addCell(header, "ACCIÓN", true, true);
            tablaCercanos.addView(header);

            // Filas taxis cercanos
            for (int i = 0; i < 3 && i < taxis.size(); i++) {
                Taxi t = taxis.get(i);
                TableRow row = new TableRow(this);
                addCell(row, String.valueOf(t.movil), false, !t.reservado);
                addCell(row, t.carnet, false, !t.reservado);
                addCell(row, t.nombreConductor, false, !t.reservado);
                addCell(row, String.format("%.2f", t.dist), false, !t.reservado);

                Button btnReservar = new Button(this);
                if (t.reservado) {
                    btnReservar.setText("Reservado");
                    btnReservar.setEnabled(false);
                    btnReservar.setBackgroundColor(Color.parseColor("#9C27B0")); // Morado
                } else {
                    btnReservar.setText("Reservar");
                    btnReservar.setEnabled(true);
                    btnReservar.setOnClickListener(v -> mostrarDialogoReserva(t));
                }
                btnReservar.setTextSize(12);
                btnReservar.setPadding(16, 8, 16, 8);
                row.addView(btnReservar);

                tablaCercanos.addView(row);
            }

            // Tabla resto
            if (taxis.size() > 3) {
                TableRow header2 = new TableRow(this);
                addCell(header2, "MÓVIL", true, false);
                addCell(header2, "CARNET", true, false);
                addCell(header2, "NOMBRE", true, false);
                addCell(header2, "Dist(km)", true, false);
                addCell(header2, "ACCIÓN", true, false);
                tablaResto.addView(header2);

                for (int i = 3; i < taxis.size(); i++) {
                    Taxi t = taxis.get(i);
                    TableRow row = new TableRow(this);
                    addCell(row, String.valueOf(t.movil), false, !t.reservado);
                    addCell(row, t.carnet, false, !t.reservado);
                    addCell(row, t.nombreConductor, false, !t.reservado);
                    addCell(row, String.format("%.2f", t.dist), false, !t.reservado);

                    Button btnReservar = new Button(this);
                    if (t.reservado) {
                        btnReservar.setText("Reservado");
                        btnReservar.setEnabled(false);
                        btnReservar.setBackgroundColor(Color.parseColor("#9C27B0"));
                    } else {
                        btnReservar.setText("Reservar");
                        btnReservar.setEnabled(true);
                        btnReservar.setOnClickListener(v -> mostrarDialogoReserva(t));
                    }
                    btnReservar.setTextSize(12);
                    btnReservar.setPadding(16, 8, 16, 8);
                    row.addView(btnReservar);

                    tablaResto.addView(row);
                }
            }

            // MAPA con popup personalizado
            mapView.getOverlays().clear();

            // Listener para cerrar InfoWindow al tocar el mapa


            GeoPoint myGeo = new GeoPoint(myLat, myLon);
            Marker myMarker = new Marker(mapView);
            myMarker.setPosition(myGeo);
            myMarker.setTitle("Tú estás aquí");
            myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            myMarker.setIcon(crearIconoCirculo(Color.BLUE));
            mapView.getOverlays().add(myMarker);

            mapView.getController().setZoom(13.5);
            mapView.getController().setCenter(myGeo);

            // Marcadores de taxis
            for (int i = 0; i < taxis.size(); i++) {
                Taxi t = taxis.get(i);
                GeoPoint taxiGeo = new GeoPoint(t.lat, t.lon);
                Marker taxiMarker = new Marker(mapView);
                taxiMarker.setPosition(taxiGeo);
                taxiMarker.setTitle("Taxi " + t.movil);
                taxiMarker.setSnippet(t.nombreConductor + "\nDist: " + String.format("%.2f km", t.dist));
                taxiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // Color según estado
                int color;
                if (t.reservado) {
                    color = Color.parseColor("#9C27B0"); // Morado = Reservado
                } else if (i < 3) {
                    color = Color.GREEN; // Verde = Libre y cercano
                } else {
                    color = Color.parseColor("#FF9800"); // Naranja = Libre y lejos
                }
                taxiMarker.setIcon(crearIconoCirculo(color));

                // Info Window personalizado con botón - PASAR REFERENCIA DEL MARKER
                taxiMarker.setInfoWindow(new CustomInfoWindow(mapView, t, this, taxiMarker));

                mapView.getOverlays().add(taxiMarker);
            }
            mapView.invalidate();
        });
    }

    // ========== INFO WINDOW PERSONALIZADO ==========
    class CustomInfoWindow extends org.osmdroid.views.overlay.infowindow.InfoWindow {
        Taxi taxi;
        ClienteActivity activity;
        Marker parentMarker;

        public CustomInfoWindow(MapView mapView, Taxi t, ClienteActivity act, Marker marker) {
            super(R.layout.custom_info_window, mapView);
            this.taxi = t;
            this.activity = act;
            this.parentMarker = marker;
        }

        @Override
        public void onOpen(Object item) {
            TextView tvTitulo = mView.findViewById(R.id.tvTituloInfo);
            TextView tvDetalle = mView.findViewById(R.id.tvDetalleInfo);
            Button btnReservarMapa = mView.findViewById(R.id.btnReservarMapa);
            Button btnCerrar = mView.findViewById(R.id.btnCerrarInfo);

            tvTitulo.setText("Taxi " + taxi.movil);
            tvDetalle.setText(taxi.nombreConductor + "\nDistancia: " + String.format("%.2f km", taxi.dist));

            // Botón cerrar - usar closeInfoWindow del marker
            btnCerrar.setOnClickListener(v -> {
                if (parentMarker != null) {
                    parentMarker.closeInfoWindow();
                }
            });

            if (taxi.reservado) {
                btnReservarMapa.setText("Reservado");
                btnReservarMapa.setEnabled(false);
                btnReservarMapa.setBackgroundColor(Color.parseColor("#9C27B0"));
            } else {
                btnReservarMapa.setText("Reservar");
                btnReservarMapa.setEnabled(true);
                btnReservarMapa.setOnClickListener(v -> {
                    if (parentMarker != null) {
                        parentMarker.closeInfoWindow();
                    }
                    activity.mostrarDialogoReserva(taxi);
                });
            }
        }

        @Override
        public void onClose() {
            // Limpiar referencias
        }
    }

    // ========== DIÁLOGO DE RESERVA ==========
    private void mostrarDialogoReserva(Taxi t) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reservar Taxi " + t.movil);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reserva, null, false);
        final EditText inputNombre = view.findViewById(R.id.inputNombre);
        final EditText inputApellido = view.findViewById(R.id.inputApellido);

        builder.setView(view);
        builder.setPositiveButton("Reservar", (dialog, which) -> {
            String nombre = inputNombre.getText().toString().trim();
            String apellido = inputApellido.getText().toString().trim();

            if (nombre.isEmpty() || apellido.isEmpty()) {
                mostrarMsg("Complete todos los campos");
                return;
            }

            guardarReservaFirebase(t, nombre, apellido);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ========== GUARDAR RESERVA CON ID SECUENCIAL ==========
    private void guardarReservaFirebase(Taxi t, String nombre, String apellido) {
        mostrarMsg("Creando reserva...");

        // Obtener contador actual
        dbF.collection("sistema")
                .document("contadores")
                .get()
                .addOnSuccessListener(doc -> {
                    long ultimoNumero = 0;
                    if (doc.exists() && doc.contains("ultimo_reserva")) {
                        ultimoNumero = doc.getLong("ultimo_reserva");
                    }

                    long nuevoNumero = ultimoNumero + 1;
                    String nuevoId = String.format("R%03d", nuevoNumero); // R001, R002...

                    // Crear reserva
                    Map<String, Object> reserva = new HashMap<>();
                    reserva.put("id", nuevoId);
                    reserva.put("cliente_nombre", nombre);
                    reserva.put("cliente_apellido", apellido);
                    reserva.put("taxi_movil", t.movil);
                    reserva.put("conductor_carnet", t.carnet);
                    reserva.put("conductor_nombre", t.nombreConductor);
                    reserva.put("estado", "Pendiente");
                    reserva.put("timestamp", System.currentTimeMillis());

                    // Guardar reserva con ID personalizado
                    dbF.collection("reservas")
                            .document(nuevoId)
                            .set(reserva)
                            .addOnSuccessListener(aVoid -> {
                                // Actualizar contador
                                Map<String, Object> contador = new HashMap<>();
                                contador.put("ultimo_reserva", nuevoNumero);

                                dbF.collection("sistema")
                                        .document("contadores")
                                        .set(contador)
                                        .addOnSuccessListener(aVoid2 -> {
                                            Log.d(TAG, "Reserva " + nuevoId + " creada");
                                            mostrarMsg("Reserva " + nuevoId + " creada correctamente");
                                            // Recargar para actualizar estado
                                            new android.os.Handler().postDelayed(() -> {
                                                cargarReservasActivas();
                                            }, 1000);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al reservar: " + e.getMessage());
                                mostrarMsg("Error al reservar");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error obteniendo contador: " + e.getMessage());
                    mostrarMsg("Error al reservar");
                });
    }

    // ========== UTILIDADES ==========
    private android.graphics.drawable.Drawable crearIconoCirculo(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(40, 40);
        drawable.setStroke(4, Color.WHITE);
        return drawable;
    }

    void mostrarMsg(String msg) {
        runOnUiThread(() -> ((TextView) findViewById(R.id.tvDireccion)).setText(msg));
    }

    void addCell(TableRow row, String text, boolean isHeader, boolean destacado) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 4, 8, 4);
        tv.setTextSize(isHeader ? 15 : 14);
        tv.setTypeface(null, isHeader ? Typeface.BOLD : Typeface.NORMAL);

        // Color según estado
        if (isHeader) {
            tv.setTextColor(destacado ? Color.parseColor("#1B5E20") : Color.parseColor("#E65100"));
        } else {
            tv.setTextColor(destacado ? Color.parseColor("#1B5E20") : Color.parseColor("#9C27B0"));
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
            dir = "Error obteniendo dirección";
        }
        return dir;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}