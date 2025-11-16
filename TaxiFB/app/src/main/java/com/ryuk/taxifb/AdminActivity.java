package com.ryuk.taxifb;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.Typeface;
import android.graphics.Color;
import android.app.AlertDialog;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.*;
import java.text.SimpleDateFormat;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminActivity";
    private FirebaseFirestore db;
    private TableLayout tablaReservas, tablaConductores, tablaTaxis;

    static class Reserva {
        String id;
        String clienteNombre, clienteApellido;
        int taxiMovil;
        String conductorNombre;
        String estado;
        long timestamp;
    }

    static class Conductor {
        String carnet;
        String nombre;
        String apellido;
    }

    static class Taxi {
        int movil;
        String carnet;
        double lat, lon;
        boolean reservado;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        tablaReservas = findViewById(R.id.tablaReservas);
        tablaConductores = findViewById(R.id.tablaConductores);
        tablaTaxis = findViewById(R.id.tablaTaxis);

        cargarTodo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarTodo();
    }

    private void cargarTodo() {
        cargarReservas();
        cargarTaxis();      // Primero taxis
        cargarConductores(); // Después conductores
    }

    // ========== RESERVAS ==========
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
                    addHeaderCell(header, "Estado");
                    addHeaderCell(header, "Acciones");
                    tablaReservas.addView(header);

                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Reserva r = new Reserva();
                            r.id = doc.getString("id");
                            r.clienteNombre = doc.getString("cliente_nombre");
                            r.clienteApellido = doc.getString("cliente_apellido");
                            Long taxiMovilLong = doc.getLong("taxi_movil");
                            r.taxiMovil = taxiMovilLong != null ? taxiMovilLong.intValue() : 0;
                            r.conductorNombre = doc.getString("conductor_nombre");
                            r.estado = doc.getString("estado");
                            Long timestampLong = doc.getLong("timestamp");
                            r.timestamp = timestampLong != null ? timestampLong : 0;

                            agregarFilaReserva(r);
                        }
                    } else {
                        Toast.makeText(this, "Error cargando reservas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void agregarFilaReserva(Reserva r) {
        TableRow row = new TableRow(this);

        // Color según estado
        int bgColor;
        if ("Pendiente".equals(r.estado)) {
            bgColor = Color.parseColor("#FFF9C4"); // Amarillo
        } else if ("En Curso".equals(r.estado)) {
            bgColor = Color.parseColor("#C8E6C9"); // Verde
        } else {
            bgColor = Color.parseColor("#E0E0E0"); // Gris
        }
        row.setBackgroundColor(bgColor);

        addDataCell(row, r.id);
        addDataCell(row, r.clienteNombre + " " + r.clienteApellido);
        addDataCell(row, "Taxi " + r.taxiMovil);
        addDataCell(row, r.estado);

        // Botón de acción
        Button btnAccion = new Button(this);
        btnAccion.setTextSize(11);
        btnAccion.setPadding(8, 4, 8, 4);

        if ("Pendiente".equals(r.estado)) {
            btnAccion.setText("Iniciar");
            btnAccion.setOnClickListener(v -> cambiarEstadoReserva(r.id, "En Curso"));
        } else if ("En Curso".equals(r.estado)) {
            btnAccion.setText("Completar");
            btnAccion.setOnClickListener(v -> cambiarEstadoReserva(r.id, "Completada"));
        } else {
            btnAccion.setText("Eliminar");
            btnAccion.setBackgroundColor(Color.parseColor("#FF5252"));
            btnAccion.setOnClickListener(v -> eliminarReserva(r.id));
        }

        row.addView(btnAccion);
        tablaReservas.addView(row);
    }

    private void cambiarEstadoReserva(String id, String nuevoEstado) {
        db.collection("reservas").document(id)
                .update("estado", nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show();
                    cargarReservas();
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
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ========== CONDUCTORES ==========
    private void cargarConductores() {
        db.collection("conductores")
                .get()
                .addOnCompleteListener(task -> {
                    tablaConductores.removeAllViews();

                    // Header
                    TableRow header = new TableRow(this);
                    addHeaderCell(header, "Carnet");
                    addHeaderCell(header, "Nombre");
                    addHeaderCell(header, "Apellido");
                    tablaConductores.addView(header);

                    if (task.isSuccessful()) {
                        List<Conductor> conductores = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Conductor c = new Conductor();
                            c.carnet = doc.getString("carnet");
                            c.nombre = doc.getString("nombre");
                            c.apellido = doc.getString("apellido");
                            conductores.add(c);
                        }

                        // Ordenar por carnet
                        Collections.sort(conductores, (a, b) -> a.carnet.compareTo(b.carnet));

                        for (Conductor c : conductores) {
                            TableRow row = new TableRow(this);
                            addDataCell(row, c.carnet);
                            addDataCell(row, c.nombre);
                            addDataCell(row, c.apellido);
                            tablaConductores.addView(row);
                        }
                    } else {
                        Toast.makeText(this, "Error cargando conductores", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ========== TAXIS ==========
    private void cargarTaxis() {
        // Primero obtener reservas activas (para saber cuáles están reservados)
        db.collection("reservas")
                .whereIn("estado", Arrays.asList("Pendiente", "En Curso"))
                .get()
                .addOnCompleteListener(taskReservas -> {
                    Set<Integer> taxisReservados = new HashSet<>();
                    if (taskReservas.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : taskReservas.getResult()) {
                            Long taxiMovil = doc.getLong("taxi_movil");
                            if (taxiMovil != null) {
                                taxisReservados.add(taxiMovil.intValue());
                            }
                        }
                    }

                    // Ahora cargar taxis
                    db.collection("taxis")
                            .get()
                            .addOnCompleteListener(taskTaxis -> {
                                tablaTaxis.removeAllViews();

                                // Encabezado
                                TableRow header = new TableRow(this);
                                addHeaderCell(header, "Móvil");
                                addHeaderCell(header, "Carnet");
                                addHeaderCell(header, "Ubicación");
                                addHeaderCell(header, "Estado");
                                addHeaderCell(header, "Acción");
                                addHeaderCell(header, "");
                                tablaTaxis.addView(header);

                                if (taskTaxis.isSuccessful()) {
                                    List<Taxi> taxis = new ArrayList<>();
                                    for (QueryDocumentSnapshot doc : taskTaxis.getResult()) {
                                        Taxi t = new Taxi();
                                        Long movilLong = doc.getLong("movil");
                                        t.movil = movilLong != null ? movilLong.intValue() : 0;
                                        t.carnet = doc.getString("carnet");
                                        Double latDouble = doc.getDouble("lat");
                                        Double lonDouble = doc.getDouble("lon");
                                        t.lat = latDouble != null ? latDouble : 0;
                                        t.lon = lonDouble != null ? lonDouble : 0;
                                        // Consultar estado reservado
                                        t.reservado = taxisReservados.contains(t.movil)
                                                || (doc.getBoolean("reservado") != null && doc.getBoolean("reservado"));
                                        taxis.add(t);
                                    }

                                    // Ordenar por móvil
                                    Collections.sort(taxis, (a, b) -> Integer.compare(a.movil, b.movil));

                                    for (Taxi t : taxis) {
                                        TableRow row = new TableRow(this);

                                        if (t.reservado) {
                                            row.setBackgroundColor(Color.parseColor("#E1BEE7")); // Morado claro
                                        }

                                        addDataCell(row, String.valueOf(t.movil));
                                        addDataCell(row, t.carnet);
                                        addDataCell(row, String.format("%.4f, %.4f", t.lat, t.lon));
                                        addDataCell(row, t.reservado ? "Reservado" : "Libre");

                                        // Botón reservar/liberar
                                        Button btnEstado = new Button(this);
                                        btnEstado.setTextSize(11);
                                        if (t.reservado) {
                                            btnEstado.setText("Liberar");
                                            btnEstado.setOnClickListener(v -> cambiarEstadoTaxi(t.movil, false));
                                        } else {
                                            btnEstado.setText("Reservar");
                                            btnEstado.setOnClickListener(v -> cambiarEstadoTaxi(t.movil, true));
                                        }
                                        row.addView(btnEstado);

                                        // Botón eliminar
                                        Button btnEliminar = new Button(this);
                                        btnEliminar.setTextSize(11);
                                        btnEliminar.setText("Eliminar");
                                        btnEliminar.setTextColor(Color.WHITE);
                                        btnEliminar.setBackgroundColor(Color.parseColor("#F44336"));
                                        btnEliminar.setOnClickListener(v -> eliminarTaxi(t.movil));
                                        row.addView(btnEliminar);

                                        tablaTaxis.addView(row);
                                    }
                                } else {
                                    Toast.makeText(this, "Error cargando taxis", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    // ========== UTILIDADES ==========
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

    // MÉTODOS NUEVOS PARA TAXI (reservar/liberar y eliminar)
    private void cambiarEstadoTaxi(int movil, boolean reservadoNuevo) {
        db.collection("taxis").whereEqualTo("movil", movil).limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        doc.getReference().update("reservado", reservadoNuevo)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, reservadoNuevo ? "Taxi reservado" : "Taxi liberado", Toast.LENGTH_SHORT).show();
                                    cargarTaxis();
                                });
                    }
                });
    }

    private void eliminarTaxi(int movil) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Taxi")
                .setMessage("¿Eliminar el taxi " + movil + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    db.collection("taxis").whereEqualTo("movil", movil).limit(1).get()
                            .addOnSuccessListener(snapshot -> {
                                for (QueryDocumentSnapshot doc : snapshot) {
                                    doc.getReference().delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Taxi eliminado", Toast.LENGTH_SHORT).show();
                                                cargarTaxis();
                                            });
                                }
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
