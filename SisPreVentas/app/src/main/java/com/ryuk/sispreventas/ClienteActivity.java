package com.ryuk.sispreventas;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.content.Intent;
import java.util.HashMap;


public class ClienteActivity extends AppCompatActivity {

    private RecyclerView rvPlatos;
    private PlatoAdapter adapter;
    private List<Plato> listaPlatos = new ArrayList<>();
    private Button btnHacerPedido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cliente);

        rvPlatos = findViewById(R.id.rvPlatos);
        rvPlatos.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PlatoAdapter(this, listaPlatos);
        rvPlatos.setAdapter(adapter);

        btnHacerPedido = findViewById(R.id.btnHacerPedido);

        btnHacerPedido.setOnClickListener(v -> {
            Map<String, Integer> cantidades = adapter.getCantidadesSeleccionadas();

            // Filtrar solo platos con cantidad > 0
            HashMap<String, Integer> items = new HashMap<>();
            for (Map.Entry<String, Integer> entry : cantidades.entrySet()) {
                int cant = entry.getValue();
                if (cant > 0) {
                    items.put(entry.getKey(), cant);
                }
            }

            if (items.isEmpty()) {
                Toast.makeText(this, "No seleccionaste ningún plato", Toast.LENGTH_SHORT).show();
            } else {
                Intent i = new Intent(ClienteActivity.this, ConfirmarPedidoActivity.class);
                i.putExtra("items", items); // HashMap es Serializable [web:200][web:203]
                startActivity(i);
            }
        });

        cargarPlatosDesdeFirebase();
    }

    private void cargarPlatosDesdeFirebase() {
        DatabaseReference platosRef = FirebaseDatabase
                .getInstance("https://sispreventa-ryuk-default-rtdb.firebaseio.com/")
                .getReference("platos");

        platosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaPlatos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Plato p = ds.getValue(Plato.class);
                    if (p != null) {
                        listaPlatos.add(p);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ClienteActivity.this,
                        "Error al cargar platos: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
