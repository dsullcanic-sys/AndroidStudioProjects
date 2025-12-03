package com.ryuk.sispreventas;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MisPedidosActivity extends AppCompatActivity {

    private RecyclerView rvMisPedidos;
    private PedidoAdapter adapter;
    private List<Pedido> listaPedidos = new ArrayList<>();
    private Map<String, Plato> mapaPlatos = new HashMap<>(); // para calcular total
    private DatabaseReference pedidosRef;
    private DatabaseReference platosRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_pedidos);

        rvMisPedidos = findViewById(R.id.rvMisPedidos);
        rvMisPedidos.setLayoutManager(new LinearLayoutManager(this));

        pedidosRef = FirebaseDatabase
                .getInstance("https://sispreventa-ryuk-default-rtdb.firebaseio.com/")
                .getReference("pedidos");

        platosRef = FirebaseDatabase
                .getInstance("https://sispreventa-ryuk-default-rtdb.firebaseio.com/")
                .getReference("platos");

        // IMPORTANTE: ahora usamos el constructor con 4 parámetros
        adapter = new PedidoAdapter(
                listaPedidos,
                mapaPlatos,
                "Cancelar",
                new PedidoAdapter.OnPedidoClickListener() {
                    @Override
                    public void onItemClick(Pedido pedido) {
                        // Click sobre todo el item (solo mostramos info)
                        Toast.makeText(MisPedidosActivity.this,
                                "Pedido " + pedido.getId() + " - Estado: " + pedido.getEstado(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onBotonClick(Pedido pedido) {
                        // Click en el botón "Cancelar"
                        if ("PEDIDO".equals(pedido.getEstado())) {
                            mostrarDialogoCancelar(pedido);
                        } else {
                            Toast.makeText(MisPedidosActivity.this,
                                    "Pedido " + pedido.getId() + " ya está " + pedido.getEstado(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        rvMisPedidos.setAdapter(adapter);

        // Primero cargamos platos, luego pedidos (para que el total salga bien)
        cargarPlatosYLuegoPedidos();
    }

    private void cargarPlatosYLuegoPedidos() {
        platosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mapaPlatos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Plato p = ds.getValue(Plato.class);
                    if (p != null) {
                        String idPlato = ds.getKey(); // p01, p02, etc.
                        mapaPlatos.put(idPlato, p);
                    }
                }
                cargarPedidosDesdeFirebase();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MisPedidosActivity.this,
                        "Error al cargar platos: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarPedidosDesdeFirebase() {

        // Leer el celular guardado en este dispositivo
        String ultimoCel = getSharedPreferences("sispreventas_prefs", MODE_PRIVATE)
                .getString("ultimoCelular", null);

        if (ultimoCel == null || ultimoCel.isEmpty()) {
            Toast.makeText(this,
                    "Aún no se detectó un celular. Haz un pedido primero.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Consultar solo los pedidos de este celular
        pedidosRef.orderByChild("celularCliente")
                .equalTo(ultimoCel)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaPedidos.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Pedido p = ds.getValue(Pedido.class);
                            if (p != null) {
                                listaPedidos.add(p);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MisPedidosActivity.this,
                                "Error al cargar pedidos: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarDialogoCancelar(Pedido pedido) {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar pedido")
                .setMessage("¿Seguro que quieres cancelar el pedido " + pedido.getId() + "?")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> cancelarPedido(pedido))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelarPedido(Pedido pedido) {
        pedidosRef.child(pedido.getId())
                .child("estado")
                .setValue("CANCELADO")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Pedido " + pedido.getId() + " cancelado",
                            Toast.LENGTH_SHORT).show();
                    pedido.setEstado("CANCELADO");
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error al cancelar: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
}
