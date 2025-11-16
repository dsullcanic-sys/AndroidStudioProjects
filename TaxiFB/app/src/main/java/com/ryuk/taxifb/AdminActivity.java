package com.ryuk.taxifb;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.app.AlertDialog;
import java.util.*;
import java.util.Locale;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminActivity extends AppCompatActivity {
    private TableLayout tablaReservas, tablaTaxis, tablaConductores;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        tablaReservas = findViewById(R.id.tablaReservas);
        tablaTaxis = findViewById(R.id.tablaTaxis);
        tablaConductores = findViewById(R.id.tablaConductores);

        cargarReservas();
        cargarTaxis();
        cargarConductores();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarReservas();
        cargarTaxis();
        cargarConductores();
    }

    // ========== CARGAR RESERVAS ==========
    private void cargarReservas() {
        db.collection("reservas")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    tablaReservas.removeAllViews();

                    // Header
                    TableRow header = new TableRow(this);
                    addHeaderCell(header, "ID");
                    addHeaderCell(header, "Cliente");
                    addHeaderCell(header, "Taxi");
                    addHeaderCell(header, "Conductor");
                    addHeaderCell(header, "Estado");
                    addHeaderCell(header, "Acción");
                    tablaReservas.addView(header);

                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            TableRow row = new TableRow(this);

                            String id = doc.getString("id");
                            String clienteNombre = doc.getString("cliente_nombre");
                            String clienteApellido = doc.getString("cliente_apellido");
                            Long taxiMovil = doc.getLong("taxi_movil");
                            String conductorNombre = doc.getString("conductor_nombre");
                            String estado = doc.getString("estado");

                            // Color según estado
                            int bgColor;
                            if ("Pendiente".equals(estado)) {
                                bgColor = Color.parseColor("#FFF9C4"); // Amarillo
                            } else if ("En Curso".equals(estado)) {
                                bgColor = Color.parseColor("#C8E6C9"); // Verde
                            } else {
                                bgColor = Color.parseColor("#E0E0E0"); // Gris
                            }
                            row.setBackgroundColor(bgColor);

                            addDataCell(row, id != null ? id : "");
                            addDataCell(row, clienteNombre + " " + clienteApellido);
                            addDataCell(row, taxiMovil != null ? String.valueOf(taxiMovil) : "");
                            addDataCell(row, conductorNombre != null ? conductorNombre : "");
                            addDataCell(row, estado != null ? estado : "");

                            // Botón según estado
                            Button btnAccion = crearBotonEstilizado(this);

                            if ("Pendiente".equals(estado)) {
                                btnAccion.setText("Iniciar");
                                setBotonColor(btnAccion, "#4CAF50");
                                btnAccion.setOnClickListener(v -> cambiarEstadoReserva(id, "En Curso"));
                            } else if ("En Curso".equals(estado)) {
                                btnAccion.setText("Completar");
                                setBotonColor(btnAccion, "#2196F3");
                                btnAccion.setOnClickListener(v -> cambiarEstadoReserva(id, "Completada"));
                            } else {
                                btnAccion.setText("Eliminar");
                                setBotonColor(btnAccion, "#F44336");
                                btnAccion.setOnClickListener(v -> eliminarReserva(id));
                            }

                            row.addView(btnAccion);
                            tablaReservas.addView(row);
                        }
                    } else {
                        Toast.makeText(this, "Error cargando reservas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cambiarEstadoReserva(String id, String nuevoEstado) {
        db.collection("reservas").document(id)
                .update("estado", nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show();
                    cargarReservas();
                    cargarTaxis(); // ACTUALIZAR TAMBIÉN LA TABLA DE TAXIS
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error actualizando estado", Toast.LENGTH_SHORT).show();
                });
    }

    private void eliminarReserva(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Reserva")
                .setMessage("¿Eliminar la reserva " + id + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    db.collection("reservas").document(id)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Reserva eliminada", Toast.LENGTH_SHORT).show();
                                cargarReservas();
                                cargarTaxis(); // ACTUALIZAR TAMBIÉN LA TABLA DE TAXIS
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ========== CARGAR TAXIS ==========
    private void cargarTaxis() {
        // Primero verificar qué taxis tienen reservas activas
        db.collection("reservas")
                .whereIn("estado", Arrays.asList("Pendiente", "En Curso"))
                .get()
                .addOnSuccessListener(reservasSnap -> {
                    Set<Integer> taxisReservados = new HashSet<>();
                    for (QueryDocumentSnapshot doc : reservasSnap) {
                        Long taxiMovil = doc.getLong("taxi_movil");
                        if (taxiMovil != null) {
                            taxisReservados.add(taxiMovil.intValue());
                        }
                    }

                    // Ahora cargar taxis
                    db.collection("taxis")
                            .get()
                            .addOnCompleteListener(task -> {
                                tablaTaxis.removeAllViews();
                                TableRow header = new TableRow(this);
                                addHeaderCell(header, "Móvil");
                                addHeaderCell(header, "Carnet");
                                addHeaderCell(header, "Ubicación");
                                addHeaderCell(header, "Dirección");
                                addHeaderCell(header, "Estado");
                                addHeaderCell(header, "Estado");
                                addHeaderCell(header, "Eliminar");
                                tablaTaxis.addView(header);

                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot doc : task.getResult()) {
                                        TableRow row = new TableRow(this);

                                        Long movilLong = doc.getLong("movil");
                                        int movil = movilLong != null ? movilLong.intValue() : 0;
                                        String carnet = doc.getString("carnet");
                                        Double latDouble = doc.getDouble("lat");
                                        Double lonDouble = doc.getDouble("lon");
                                        double lat = latDouble != null ? latDouble : 0;
                                        double lon = lonDouble != null ? lonDouble : 0;
                                        boolean reservado = taxisReservados.contains(movil);

                                        // Color si está reservado
                                        if (reservado) {
                                            row.setBackgroundColor(Color.parseColor("#E1BEE7")); // Morado claro
                                        }

                                        addDataCell(row, String.valueOf(movil));
                                        addDataCell(row, carnet != null ? carnet : "");
                                        addDataCell(row, String.format("%.4f, %.4f", lat, lon));

                                        // Dirección asincrónica
                                        TextView tvDir = new TextView(this);
                                        tvDir.setText("Buscando...");
                                        tvDir.setPadding(8, 8, 8, 8);
                                        tvDir.setTextSize(13);
                                        row.addView(tvDir);
                                        new Thread(() -> {
                                            String direccion = obtenerDireccion(lat, lon);
                                            runOnUiThread(() -> tvDir.setText(direccion));
                                        }).start();

                                        addDataCell(row, reservado ? "Reservado" : "Libre");

                                        // Botón Estado (Reservar/Liberar)
                                        Button btnEstado = crearBotonEstilizado(this);
                                        btnEstado.setText(reservado ? "Liberar" : "Reservar");
                                        setBotonColor(btnEstado, reservado ? "#9E9E9E" : "#4CAF50");

                                        final int finalMovil = movil;
                                        final boolean finalReservado = reservado;
                                        btnEstado.setOnClickListener(v -> {
                                            if (finalReservado) {
                                                // Liberar: buscar y eliminar la reserva activa
                                                liberarTaxi(finalMovil);
                                            } else {
                                                // Reservar: mostrar diálogo para crear reserva manual
                                                mostrarDialogoReservaManual(finalMovil, carnet);
                                            }
                                        });
                                        row.addView(btnEstado);

                                        // Botón Eliminar
                                        Button btnEliminar = crearBotonEstilizado(this);
                                        btnEliminar.setText("Eliminar");
                                        setBotonColor(btnEliminar, "#F44336");
                                        btnEliminar.setOnClickListener(v -> eliminarTaxi(movil));
                                        row.addView(btnEliminar);

                                        tablaTaxis.addView(row);
                                    }
                                }
                            });
                });
    }

    private String obtenerDireccion(double lat, double lon) {
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
                return sb.toString();
            }
        } catch (Exception e) {
            return "Sin dirección";
        }
        return "Sin dirección";
    }

    private void eliminarTaxi(int movil) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Taxi")
                .setMessage("¿Eliminar el taxi " + movil + " de la lista actual?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    db.collection("taxis").document(String.valueOf(movil))
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Taxi eliminado", Toast.LENGTH_SHORT).show();
                                cargarTaxis();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Liberar taxi: eliminar reserva activa
    private void liberarTaxi(int movil) {
        db.collection("reservas")
                .whereEqualTo("taxi_movil", movil)
                .whereIn("estado", Arrays.asList("Pendiente", "En Curso"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            doc.getReference().update("estado", "Completada")
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Taxi liberado", Toast.LENGTH_SHORT).show();
                                        cargarReservas(); // ACTUALIZAR RESERVAS
                                        cargarTaxis(); // ACTUALIZAR TAXIS
                                    });
                        }
                    } else {
                        Toast.makeText(this, "No hay reserva activa", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Crear reserva manual desde admin
    private void mostrarDialogoReservaManual(int movil, String carnet) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reservar Taxi " + movil);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final android.widget.EditText inputNombre = new android.widget.EditText(this);
        inputNombre.setHint("Nombre del cliente");
        layout.addView(inputNombre);

        final android.widget.EditText inputApellido = new android.widget.EditText(this);
        inputApellido.setHint("Apellido del cliente");
        layout.addView(inputApellido);

        builder.setView(layout);
        builder.setPositiveButton("Reservar", (dialog, which) -> {
            String nombre = inputNombre.getText().toString().trim();
            String apellido = inputApellido.getText().toString().trim();

            if (nombre.isEmpty() || apellido.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            crearReservaManual(movil, carnet, nombre, apellido);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void crearReservaManual(int movil, String carnet, String nombre, String apellido) {
        // Obtener contador
        db.collection("sistema")
                .document("contadores")
                .get()
                .addOnSuccessListener(doc -> {
                    long ultimoNumero = 0;
                    if (doc.exists() && doc.contains("ultimo_reserva")) {
                        ultimoNumero = doc.getLong("ultimo_reserva");
                    }

                    long nuevoNumero = ultimoNumero + 1;
                    String nuevoId = String.format("R%03d", nuevoNumero);

                    // Obtener nombre del conductor
                    db.collection("conductores")
                            .document(carnet)
                            .get()
                            .addOnSuccessListener(conductorDoc -> {
                                String conductorNombre = "Desconocido";
                                if (conductorDoc.exists()) {
                                    String nombreCond = conductorDoc.getString("nombre");
                                    String apellidoCond = conductorDoc.getString("apellido");
                                    conductorNombre = nombreCond + " " + apellidoCond;
                                }

                                // Crear reserva
                                Map<String, Object> reserva = new HashMap<>();
                                reserva.put("id", nuevoId);
                                reserva.put("cliente_nombre", nombre);
                                reserva.put("cliente_apellido", apellido);
                                reserva.put("taxi_movil", movil);
                                reserva.put("conductor_carnet", carnet);
                                reserva.put("conductor_nombre", conductorNombre);
                                reserva.put("estado", "Pendiente");
                                reserva.put("timestamp", System.currentTimeMillis());

                                db.collection("reservas")
                                        .document(nuevoId)
                                        .set(reserva)
                                        .addOnSuccessListener(aVoid -> {
                                            // Actualizar contador
                                            Map<String, Object> contador = new HashMap<>();
                                            contador.put("ultimo_reserva", nuevoNumero);
                                            db.collection("sistema")
                                                    .document("contadores")
                                                    .set(contador)
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        Toast.makeText(this, "Reserva " + nuevoId + " creada", Toast.LENGTH_SHORT).show();
                                                        cargarReservas();
                                                        cargarTaxis();
                                                    });
                                        });
                            });
                });
    }

    // ========== CARGAR CONDUCTORES ==========
    private void cargarConductores() {
        db.collection("taxis").get().addOnSuccessListener(taxiSnap -> {
            Map<String, List<Integer>> taxisPorConductor = new HashMap<>();
            for (QueryDocumentSnapshot doc : taxiSnap) {
                String carnet = doc.getString("carnet");
                Long movil = doc.getLong("movil");
                if (carnet != null && movil != null) {
                    String carnetKey = carnet.replaceAll("\\D", "").trim();
                    if (!taxisPorConductor.containsKey(carnetKey)) {
                        taxisPorConductor.put(carnetKey, new ArrayList<>());
                    }
                    taxisPorConductor.get(carnetKey).add(movil.intValue());
                }
            }

            db.collection("conductores").get().addOnSuccessListener(conSnap -> {
                tablaConductores.removeAllViews();
                TableRow header = new TableRow(this);
                addHeaderCell(header, "Carnet");
                addHeaderCell(header, "Nombre");
                addHeaderCell(header, "Apellido");
                addHeaderCell(header, "Taxis asignados");
                tablaConductores.addView(header);

                // Crear lista para ordenar
                List<ConductorConTaxis> conductoresOrdenados = new ArrayList<>();

                for (QueryDocumentSnapshot doc : conSnap) {
                    String carnet = doc.getString("carnet");
                    String nombre = doc.getString("nombre");
                    String apellido = doc.getString("apellido");

                    List<Integer> taxisAsignados = taxisPorConductor.getOrDefault(
                            carnet != null ? carnet.replaceAll("\\D", "").trim() : "",
                            new ArrayList<>());

                    ConductorConTaxis conductor = new ConductorConTaxis();
                    conductor.carnet = carnet != null ? carnet : "";
                    conductor.nombre = nombre != null ? nombre : "";
                    conductor.apellido = apellido != null ? apellido : "";
                    conductor.taxis = taxisAsignados;

                    conductoresOrdenados.add(conductor);
                }

                // Ordenar por móvil más bajo asignado
                Collections.sort(conductoresOrdenados, (a, b) -> {
                    int minA = a.taxis.isEmpty() ? Integer.MAX_VALUE : Collections.min(a.taxis);
                    int minB = b.taxis.isEmpty() ? Integer.MAX_VALUE : Collections.min(b.taxis);
                    return Integer.compare(minA, minB);
                });

                // Agregar filas ordenadas
                for (ConductorConTaxis conductor : conductoresOrdenados) {
                    TableRow row = new TableRow(this);
                    addDataCell(row, conductor.carnet);
                    addDataCell(row, conductor.nombre);
                    addDataCell(row, conductor.apellido);
                    addDataCell(row, conductor.taxis.isEmpty() ? "Ninguno" :
                            android.text.TextUtils.join(", ", conductor.taxis));
                    tablaConductores.addView(row);
                }
            });
        });
    }

    // Clase auxiliar para ordenar conductores
    static class ConductorConTaxis {
        String carnet;
        String nombre;
        String apellido;
        List<Integer> taxis;
    }

    // ========== UTILIDADES ==========
    private Button crearBotonEstilizado(android.content.Context context) {
        Button btn = new Button(context);
        btn.setTextSize(11);
        btn.setPadding(16, 8, 16, 8);
        btn.setTextColor(Color.WHITE);
        btn.setAllCaps(false);

        // Ajustar tamaño para que no ocupe toda la casilla
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 4, 4, 4);
        btn.setLayoutParams(params);

        return btn;
    }

    private void setBotonColor(Button btn, String colorHex) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setCornerRadius(8);
        drawable.setColor(Color.parseColor(colorHex));
        btn.setBackground(drawable);
    }

    private void addHeaderCell(TableRow row, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.parseColor("#1976D2"));
        row.addView(tv);
    }

    private void addDataCell(TableRow row, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        tv.setTextSize(13);
        tv.setTextColor(Color.BLACK);
        row.addView(tv);
    }
}