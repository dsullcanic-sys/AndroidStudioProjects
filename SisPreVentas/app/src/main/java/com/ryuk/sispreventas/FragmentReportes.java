package com.ryuk.sispreventas;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FragmentReportes extends Fragment {

    private EditText etFechaDesde, etFechaHasta;
    private Button btnAplicarRango;
    private RecyclerView rvReportesPedidos;

    private PedidoAdapter adapter;
    private List<Pedido> listaPedidosTodas = new ArrayList<>();
    private List<Pedido> listaPedidosMostrada = new ArrayList<>();
    private Map<String, Plato> mapaPlatos = new HashMap<>();

    private DatabaseReference pedidosRef;
    private DatabaseReference platosRef;

    private SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Fechas del rango como String "yyyy-MM-dd"
    private String fechaDesdeStr;
    private String fechaHastaStr;

    public FragmentReportes() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes, container, false);

        etFechaDesde = view.findViewById(R.id.etFechaDesde);
        etFechaHasta = view.findViewById(R.id.etFechaHasta);
        btnAplicarRango = view.findViewById(R.id.btnAplicarRango);
        rvReportesPedidos = view.findViewById(R.id.rvReportesPedidos);

        rvReportesPedidos.setLayoutManager(new LinearLayoutManager(getContext()));

        pedidosRef = FirebaseDatabase
                .getInstance("https://sispreventa-ryuk-default-rtdb.firebaseio.com/")
                .getReference("pedidos"); // [web:343]

        platosRef = FirebaseDatabase
                .getInstance("https://sispreventa-ryuk-default-rtdb.firebaseio.com/")
                .getReference("platos");  // [web:343]

        adapter = new PedidoAdapter(
                listaPedidosMostrada,
                mapaPlatos,
                "Cambiar",
                new PedidoAdapter.OnPedidoClickListener() {
                    @Override
                    public void onItemClick(Pedido pedido) {
                        if (getContext() != null) {
                            Toast.makeText(
                                    getContext(),
                                    "Pedido " + pedido.getId() + " - Estado: " + pedido.getEstado(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onBotonClick(Pedido pedido) {
                        if (getContext() == null) return;

                        if (!"PEDIDO".equals(pedido.getEstado())) {
                            Toast.makeText(
                                    getContext(),
                                    "Pedido " + pedido.getId() + " ya está " + pedido.getEstado(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                                .setTitle("Cambiar estado")
                                .setMessage("¿Qué desea hacer con el pedido " + pedido.getId() + "?")
                                .setPositiveButton("Marcar ENTREGADO",
                                        (dialog, which) -> actualizarEstado(pedido, "ENTREGADO"))
                                .setNegativeButton("Marcar CANCELADO",
                                        (dialog, which) -> actualizarEstado(pedido, "CANCELADO"))
                                .setNeutralButton("Cerrar", null)
                                .show();
                    }
                }
        );

        rvReportesPedidos.setAdapter(adapter);

        // Configurar EditText como solo clicables (no teclado)
        configurarEditTextsFechas();

        // Fechas por defecto: hoy en ambos extremos
        String hoyStr = sdf.format(new Date()); // [web:395][web:396]
        fechaDesdeStr = hoyStr;
        fechaHastaStr = hoyStr;
        etFechaDesde.setText(hoyStr);
        etFechaHasta.setText(hoyStr);

        // Cargar datos
        cargarPlatosYLuegoPedidos();

        // Listeners de fecha
        etFechaDesde.setOnClickListener(v -> mostrarDatePicker(true));
        etFechaHasta.setOnClickListener(v -> mostrarDatePicker(false));

        // Aplicar rango
        btnAplicarRango.setOnClickListener(v -> {
            try {
                Date dDesde = sdf.parse(fechaDesdeStr);
                Date dHasta = sdf.parse(fechaHastaStr);
                if (dDesde != null && dHasta != null) {
                    if (dDesde.after(dHasta)) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Fecha desde no puede ser mayor que fecha hasta",
                                    Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    filtrarPorRangoFechas(dDesde, dHasta);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });

        return view;
    }

    private void configurarEditTextsFechas() {
        etFechaDesde.setFocusable(false);
        etFechaDesde.setClickable(true);
        etFechaDesde.setLongClickable(false);

        etFechaHasta.setFocusable(false);
        etFechaHasta.setClickable(true);
        etFechaHasta.setLongClickable(false);
    }

    private void mostrarDatePicker(boolean esDesde) {
        if (getContext() == null) return;

        Calendar cal = Calendar.getInstance();
        try {
            String baseStr = esDesde ? fechaDesdeStr : fechaHastaStr;
            Date base = sdf.parse(baseStr);
            if (base != null) {
                cal.setTime(base);
            }
        } catch (ParseException ignored) { }

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH); // 0-11
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int y, int m, int d) {
                        Calendar c = Calendar.getInstance();
                        c.set(y, m, d, 0, 0, 0);
                        String seleccionadaStr = sdf.format(c.getTime());
                        if (esDesde) {
                            fechaDesdeStr = seleccionadaStr;
                            etFechaDesde.setText(seleccionadaStr);
                        } else {
                            fechaHastaStr = seleccionadaStr;
                            etFechaHasta.setText(seleccionadaStr);
                        }
                    }
                },
                year, month, day
        );
        dialog.show();
    }

    private void cargarPlatosYLuegoPedidos() {
        platosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mapaPlatos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Plato p = ds.getValue(Plato.class);
                    if (p != null) {
                        String idPlato = ds.getKey(); // p01, p02...
                        mapaPlatos.put(idPlato, p);
                    }
                }
                cargarPedidosDesdeFirebase();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(
                            getContext(),
                            "Error al cargar platos: " + error.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    private void cargarPedidosDesdeFirebase() {
        pedidosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaPedidosTodas.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Pedido p = ds.getValue(Pedido.class);
                    if (p != null) {
                        listaPedidosTodas.add(p);
                    }
                }

                // Por defecto, aplicar rango actual (hoy-hoy).
                try {
                    Date dDesde = sdf.parse(fechaDesdeStr);
                    Date dHasta = sdf.parse(fechaHastaStr);
                    if (dDesde != null && dHasta != null) {
                        filtrarPorRangoFechas(dDesde, dHasta);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(
                            getContext(),
                            "Error al cargar pedidos: " + error.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    private void filtrarPorRangoFechas(Date inicio, Date fin) {
        listaPedidosMostrada.clear();
        for (Pedido p : listaPedidosTodas) {
            try {
                if (p.getFecha() == null) continue;
                Date fechaPedido = sdf.parse(p.getFecha());
                if (fechaPedido == null) continue;

                if ((fechaPedido.equals(inicio) || fechaPedido.after(inicio)) &&
                        (fechaPedido.equals(fin) || fechaPedido.before(fin))) {
                    listaPedidosMostrada.add(p);
                }
            } catch (ParseException ignored) { }
        }

        listaPedidosMostrada.sort((p1, p2) -> {
            int w1 = pesoEstado(p1.getEstado());
            int w2 = pesoEstado(p2.getEstado());
            if (w1 != w2) return Integer.compare(w1, w2);
            return p1.getId().compareTo(p2.getId());
        });

        adapter.notifyDataSetChanged();

        if (getContext() != null) {
            Toast.makeText(
                    getContext(),
                    "Mostrando pedidos del " + sdf.format(inicio) + " al " + sdf.format(fin),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private int pesoEstado(String estado) {
        if ("PEDIDO".equals(estado)) return 0;
        if ("ENTREGADO".equals(estado)) return 1;
        if ("CANCELADO".equals(estado)) return 2;
        return 3;
    }

    private void actualizarEstado(Pedido pedido, String nuevoEstado) {
        pedidosRef.child(pedido.getId())
                .child("estado")
                .setValue(nuevoEstado)
                .addOnSuccessListener(unused -> {
                    if (getContext() != null) {
                        Toast.makeText(
                                getContext(),
                                "Pedido " + pedido.getId() + " ahora está " + nuevoEstado,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    pedido.setEstado(nuevoEstado);

                    listaPedidosMostrada.sort((p1, p2) -> {
                        int w1 = pesoEstado(p1.getEstado());
                        int w2 = pesoEstado(p2.getEstado());
                        if (w1 != w2) return Integer.compare(w1, w2);
                        return p1.getId().compareTo(p2.getId());
                    });

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(
                                getContext(),
                                "Error al actualizar estado: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
