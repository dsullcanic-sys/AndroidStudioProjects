package com.ryuk.taxifb;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOCATION = 1;
    private Button btnCliente, btnAdmin, btnActualizarConductores;
    private TextView tvEstado;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    // Variables para compartir datos
    public static double ubicacionLat = 0;
    public static double ubicacionLon = 0;
    public static boolean datosListos = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // Configurar para permitir conexiones de red en el hilo principal
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        // Inicializar ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // UI
        btnCliente = findViewById(R.id.btnCliente);
        btnAdmin = findViewById(R.id.btnAdmin);
        btnActualizarConductores = findViewById(R.id.btnActualizarConductores);
        tvEstado = findViewById(R.id.tvEstado);

        // Deshabilitar botones hasta que carguen los datos
        btnCliente.setEnabled(false);
        btnAdmin.setEnabled(false);
        tvEstado.setText("Cargando datos...");

        // Listeners
        btnCliente.setOnClickListener(v -> {
            startActivity(new Intent(this, ClienteActivity.class));
        });

        btnAdmin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        btnActualizarConductores.setOnClickListener(v -> {
            actualizarConductoresDesdeWeb();
        });

        // INICIAR CARGA DE DATOS
        inicializarDatos();
    }

    private void inicializarDatos() {
        tvEstado.setText("Obteniendo ubicación GPS...");
        solicitarUbicacion();
    }

    private void solicitarUbicacion() {
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
                        ubicacionLat = location.getLatitude();
                        ubicacionLon = location.getLongitude();
                        Log.d(TAG, "Ubicación obtenida: " + ubicacionLat + ", " + ubicacionLon);
                        tvEstado.setText("Ubicación obtenida. Cargando taxis...");
                        cargarTaxisDesdeWeb();
                    } else {
                        Log.w(TAG, "No se obtuvo ubicación");
                        tvEstado.setText("No se obtuvo ubicación. Cargando taxis...");
                        cargarTaxisDesdeWeb();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacion();
            } else {
                tvEstado.setText("Permiso denegado. Cargando taxis...");
                cargarTaxisDesdeWeb();
            }
        }
    }

    // ========== CARGAR TAXIS DESDE WEB ==========
    private void cargarTaxisDesdeWeb() {
        // PRIMERO: Borrar taxis antiguos
        tvEstado.setText("Limpiando taxis antiguos...");
        db.collection("taxis")
                .get()
                .addOnSuccessListener(result -> {
                    for (QueryDocumentSnapshot doc : result) {
                        db.collection("taxis").document(doc.getId()).delete();
                    }
                    Log.d(TAG, "Taxis antiguos eliminados: " + result.size());
                    // LUEGO: Descargar taxis actuales
                    descargarTaxisActuales();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error borrando taxis: " + e.getMessage());
                    descargarTaxisActuales(); // Continuar de todos modos
                });
    }

    private void descargarTaxisActuales() {
        new Thread(() -> {
            try {
                Log.d(TAG, "Iniciando descarga de taxis...");
                URL url = new URL("https://clasespersonales.com/taxis/");
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuilder html = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) html.append(line).append("\n");
                reader.close();

                String content = html.toString();
                Log.d(TAG, "HTML descargado, tamaño: " + content.length() + " caracteres");
                Log.d(TAG, "Primeros 200 chars: " + content.substring(0, Math.min(200, content.length())));

                String[] rows = content.split("<tr");

                Log.d(TAG, "Total filas encontradas: " + rows.length);
                runOnUiThread(() -> tvEstado.setText("Procesando " + rows.length + " taxis..."));

                int taxisGuardados = 0;
                for (int i = 0; i < rows.length; i++) {
                    String row = rows[i];
                    if (!row.contains("<td")) {
                        Log.d(TAG, "Fila " + i + " saltada (no tiene <td)");
                        continue;
                    }

                    Log.d(TAG, "Procesando fila " + i + ": " + row.substring(0, Math.min(100, row.length())));

                    try {
                        List<String> valores = new ArrayList<>();
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(">([^<>]+)<");
                        java.util.regex.Matcher matcher = pattern.matcher(row);

                        while (matcher.find()) {
                            String v = matcher.group(1).replace("&nbsp;", "").replace(",", ".").trim();
                            if (!v.isEmpty()) {
                                valores.add(v);
                                Log.d(TAG, "  Valor encontrado: " + v);
                            }
                        }

                        Log.d(TAG, "  Total valores: " + valores.size());

                        if (valores.size() >= 4) {
                            String movilStr = valores.get(0).replaceAll("[^\\d]", "");
                            String carnet = valores.get(1).replaceAll("\\D", "").trim();
                            String latStr = valores.get(2).trim();
                            String lonStr = valores.get(3).trim();

                            Log.d(TAG, "  Parseando: movil=" + movilStr + ", carnet=" + carnet + ", lat=" + latStr + ", lon=" + lonStr);

                            int movil = Integer.parseInt(movilStr);
                            double lat = Double.parseDouble(latStr);
                            double lon = Double.parseDouble(lonStr);

                            // CREAR DOCUMENTO CON MOVIL COMO ID
                            Map<String, Object> taxiData = new HashMap<>();
                            taxiData.put("movil", movil);
                            taxiData.put("carnet", carnet);
                            taxiData.put("lat", lat);
                            taxiData.put("lon", lon);

                            String docId = String.valueOf(movil);
                            Log.d(TAG, ">>> Guardando taxi con ID: " + docId + " (carnet: " + carnet + ")");

                            // IMPORTANTE: usar .document(ID).set() NO .add()
                            db.collection("taxis")
                                    .document(docId)
                                    .set(taxiData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✓✓✓ Taxi " + docId + " guardado correctamente en Firebase");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "✗✗✗ Error guardando taxi " + docId + ": " + e.getMessage());
                                        e.printStackTrace();
                                    });

                            taxisGuardados++;
                        } else {
                            Log.w(TAG, "  Fila saltada: solo tiene " + valores.size() + " valores (necesita 4)");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error procesando fila " + i + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                final int total = taxisGuardados;
                Log.d(TAG, "Total taxis procesados: " + total);

                runOnUiThread(() -> {
                    tvEstado.setText(total + " taxis activos cargados. Creando conductores...");
                    // Esperar 2 segundos para que Firebase termine de guardar
                    new android.os.Handler().postDelayed(() -> {
                        crearConductoresEjemplo();
                    }, 2000);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error cargando taxis: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvEstado.setText("Error: " + e.getMessage());
                    crearConductoresEjemplo();
                });
            }
        }).start();
    }

    // ========== CREAR CONDUCTORES DE EJEMPLO ==========
    private void crearConductoresEjemplo() {
        Log.d(TAG, "Verificando conductores...");

        // Verificar si ya existen conductores
        db.collection("conductores")
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        Log.d(TAG, "No hay conductores, cargando desde web...");
                        tvEstado.setText("Cargando conductores desde web...");
                        actualizarConductoresDesdeWebInicial();
                    } else {
                        Log.d(TAG, "Ya existen conductores: " + result.size());
                        tvEstado.setText("Conductores existentes. Creando usuarios...");
                        crearUsuariosEjemplo();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error verificando conductores: " + e.getMessage());
                    tvEstado.setText("Error verificando conductores");
                });
    }

    // ========== CARGA INICIAL DE CONDUCTORES ==========
    private void actualizarConductoresDesdeWebInicial() {
        new Thread(() -> {
            try {
                Log.d(TAG, "Descargando conductores...");
                URL url = new URL("https://clasespersonales.com/taxis/listacon.php");
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();

                String jsonStr = json.toString();
                Log.d(TAG, "JSON recibido (primeros 100 chars): " + jsonStr.substring(0, Math.min(100, jsonStr.length())));

                // Parsear JSON manualmente
                if (jsonStr.contains("\"conductores\":[")) {
                    String conductoresArray = jsonStr.substring(
                            jsonStr.indexOf("\"conductores\":[") + 15,
                            jsonStr.lastIndexOf("]")
                    );

                    String[] conductores = conductoresArray.split("\\},\\{");
                    int count = 0;

                    runOnUiThread(() -> tvEstado.setText("Guardando " + conductores.length + " conductores..."));

                    for (String conductor : conductores) {
                        try {
                            String carnet = extraerValor(conductor, "carnet");
                            String paterno = extraerValor(conductor, "paterno");
                            String materno = extraerValor(conductor, "materno");
                            String nombres = extraerValor(conductor, "nombres");

                            if (!carnet.isEmpty()) {
                                Map<String, Object> conductorData = new HashMap<>();
                                conductorData.put("carnet", carnet);
                                conductorData.put("nombre", nombres);
                                conductorData.put("apellido", paterno + " " + materno);

                                // USAR CARNET COMO ID
                                db.collection("conductores")
                                        .document(carnet)
                                        .set(conductorData);

                                count++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando conductor: " + e.getMessage());
                        }
                    }

                    final int totalConductores = count;
                    Log.d(TAG, "Total conductores procesados: " + totalConductores);

                    runOnUiThread(() -> {
                        tvEstado.setText(totalConductores + " conductores cargados. Creando usuarios...");
                        new android.os.Handler().postDelayed(() -> {
                            crearUsuariosEjemplo();
                        }, 2000);
                    });
                } else {
                    throw new Exception("JSON inválido - no contiene 'conductores'");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error descargando conductores: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvEstado.setText("Error con conductores. Creando usuarios...");
                    crearUsuariosEjemplo();
                });
            }
        }).start();
    }

    // ========== ACTUALIZAR CONDUCTORES DESDE WEB (BOTÓN) ==========
    private void actualizarConductoresDesdeWeb() {
        tvEstado.setText("Descargando conductores desde web...");

        new Thread(() -> {
            try {
                Log.d(TAG, "Descargando conductores (botón)...");
                URL url = new URL("https://clasespersonales.com/taxis/listacon.php");
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();

                String jsonStr = json.toString();
                Log.d(TAG, "JSON recibido: " + jsonStr.substring(0, Math.min(100, jsonStr.length())) + "...");

                // Parsear JSON manualmente
                if (jsonStr.contains("\"conductores\":[")) {
                    String conductoresArray = jsonStr.substring(
                            jsonStr.indexOf("\"conductores\":[") + 15,
                            jsonStr.lastIndexOf("]")
                    );

                    String[] conductores = conductoresArray.split("\\},\\{");
                    int count = 0;

                    runOnUiThread(() -> tvEstado.setText("Guardando " + conductores.length + " conductores..."));

                    for (String conductor : conductores) {
                        try {
                            String carnet = extraerValor(conductor, "carnet");
                            String paterno = extraerValor(conductor, "paterno");
                            String materno = extraerValor(conductor, "materno");
                            String nombres = extraerValor(conductor, "nombres");

                            if (!carnet.isEmpty()) {
                                Map<String, Object> conductorData = new HashMap<>();
                                conductorData.put("carnet", carnet);
                                conductorData.put("nombre", nombres);
                                conductorData.put("apellido", paterno + " " + materno);

                                // USAR CARNET COMO ID
                                db.collection("conductores")
                                        .document(carnet)
                                        .set(conductorData)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✓ Conductor guardado: " + carnet);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "✗ Error guardando conductor: " + e.getMessage());
                                        });

                                count++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error procesando conductor: " + e.getMessage());
                        }
                    }

                    final int totalConductores = count;
                    Log.d(TAG, "Total conductores procesados: " + totalConductores);

                    runOnUiThread(() -> {
                        tvEstado.setText("✓ " + totalConductores + " conductores actualizados");
                    });
                } else {
                    throw new Exception("JSON inválido");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error descargando conductores: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvEstado.setText("Error descargando conductores: " + e.getMessage());
                });
            }
        }).start();
    }

    // Método auxiliar para extraer valores del JSON
    private String extraerValor(String json, String campo) {
        try {
            String patron = "\"" + campo + "\":\"";
            int inicio = json.indexOf(patron);
            if (inicio == -1) return "";
            inicio += patron.length();
            int fin = json.indexOf("\"", inicio);
            if (fin == -1) return "";
            return json.substring(inicio, fin);
        } catch (Exception e) {
            return "";
        }
    }

    // ========== CREAR USUARIOS (CLIENTES) DE EJEMPLO ==========
    private void crearUsuariosEjemplo() {
        Log.d(TAG, "Creando usuarios de ejemplo...");

        String[][] usuarios = {
                {"11111111", "Roberto", "Sánchez", "70000001"},
                {"22222222", "Laura", "Martínez", "70000002"},
                {"33333333", "Diego", "Flores", "70000003"}
        };

        for (String[] u : usuarios) {
            Map<String, Object> usuario = new HashMap<>();
            usuario.put("carnet", u[0]);
            usuario.put("nombre", u[1]);
            usuario.put("apellido", u[2]);
            usuario.put("telefono", u[3]);

            Log.d(TAG, "Guardando usuario con carnet: " + u[0]);

            // USAR CARNET COMO ID
            db.collection("usuarios")
                    .document(u[0])
                    .set(usuario)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✓ Usuario " + u[0] + " guardado");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "✗ Error guardando usuario: " + e.getMessage());
                    });
        }

        // DATOS LISTOS
        new android.os.Handler().postDelayed(() -> {
            runOnUiThread(() -> {
                datosListos = true;
                btnCliente.setEnabled(true);
                btnAdmin.setEnabled(true);
                tvEstado.setText("✓ Datos cargados correctamente\nSelecciona una opción:");
                Log.d(TAG, "=== CARGA COMPLETA ===");
            });
        }, 1000);
    }
}