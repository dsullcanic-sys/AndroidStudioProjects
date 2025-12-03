package com.ryuk.sispreventas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ConfirmarPedidoActivity extends AppCompatActivity {

    private EditText edtNombre, edtApePaterno, edtApeMaterno, edtCelular;
    private TextView txtDetallePlatos, txtTotal;
    private Button btnConfirmar;

    private HashMap<String, Integer> itemsSeleccionados; // idPlato -> cantidad

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmar_pedido);

        edtNombre = findViewById(R.id.edtNombre);
        edtApePaterno = findViewById(R.id.edtApePaterno);
        edtApeMaterno = findViewById(R.id.edtApeMaterno);
        edtCelular = findViewById(R.id.edtCelular);
        txtDetallePlatos = findViewById(R.id.txtDetallePlatos);
        txtTotal = findViewById(R.id.txtTotal);
        btnConfirmar = findViewById(R.id.btnConfirmarPedido);

        itemsSeleccionados = (HashMap<String, Integer>) getIntent().getSerializableExtra("items");
        if (itemsSeleccionados == null || itemsSeleccionados.isEmpty()) {
            Toast.makeText(this, "No hay platos seleccionados", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarResumenDesdeFirebase();

        btnConfirmar.setOnClickListener(v -> guardarPedido());
    }

    private void cargarResumenDesdeFirebase() {
        DatabaseReference platosRef = FirebaseDatabase
                .getInstance("https://sispreventa-ryuk-default-rtdb.firebaseio.com/")
                .getReference("platos");

        platosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder detalle = new StringBuilder();
                int total = 0;

                for (Map.Entry<String, Integer> entry : itemsSeleccionados.entrySet()) {
                    String idPlato = entry.getKey();
                    int cant = entry.getValue();
                    if (cant <= 0) continue;

                    DataSnapshot platoSnap = snapshot.child(idPlato);
                    Plato p = platoSnap.getValue(Plato.class);
                    if (p != null) {
                        int subtotal = p.getPrecio() * cant;
                        total += subtotal;
                        detalle.append(p.getNombre())
                                .append(" x ")
                                .append(cant)
                                .append(" = Bs. ")
                                .append(subtotal)
                                .append("\n");
                    }
                }

                txtDetallePlatos.setText(detalle.toString());
                txtTotal.setText("Total: Bs. " + total);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ConfirmarPedidoActivity.this,
                        "Error al cargar resumen: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarPedido() {
        String nombre = edtNombre.getText().toString().trim();
        String apePat = edtApePaterno.getText().toString().trim();
        String apeMat = edtApeMaterno.getText().toString().trim();
        String celular = edtCelular.getText().toString().trim();

        if (nombre.isEmpty() || apePat.isEmpty() || celular.isEmpty()) {
            Toast.makeText(this, "Completa Nombre, Ap. Paterno y Celular", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar el celular en este dispositivo para usarlo en "Mis pedidos"
        getSharedPreferences("sispreventas_prefs", MODE_PRIVATE)
                .edit()
                .putString("ultimoCelular", celular)
                .apply();

        DatabaseReference rootRef = FirebaseDatabase
                .getInstance("https://sispreventa-ryuk-default-rtdb.firebaseio.com/")
                .getReference();

        DatabaseReference pedidosRef = rootRef.child("pedidos");

        pedidosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int maxNum = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String key = ds.getKey(); // ped001, ped002...
                    if (key != null && key.startsWith("ped")) {
                        try {
                            int num = Integer.parseInt(key.substring(3));
                            if (num > maxNum) maxNum = num;
                        } catch (NumberFormatException ignored) {}
                    }
                }
                int siguiente = maxNum + 1;
                String pedidoId = String.format("ped%03d", siguiente);

                // Fecha
                String fecha;
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    fecha = sdf.format(new java.util.Date());
                } catch (Exception e) {
                    fecha = "2025-11-30";
                }

                // Pedido con datos del cliente embebidos
                Pedido pedido = new Pedido(
                        pedidoId,
                        fecha,
                        "PEDIDO",
                        itemsSeleccionados,
                        nombre,
                        apePat,
                        apeMat,
                        celular
                );

                pedidosRef.child(pedidoId).setValue(pedido)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(ConfirmarPedidoActivity.this,
                                    "Pedido " + pedidoId + " registrado correctamente",
                                    Toast.LENGTH_SHORT).show();

                            // NUEVO: Ir a Mis Pedidos y limpiar hasta MainActivity
                            irAMisPedidos();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(ConfirmarPedidoActivity.this,
                                        "Error al guardar pedido: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show()
                        );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ConfirmarPedidoActivity.this,
                        "Error obteniendo pedidos: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Navega a MisPedidosActivity y limpia el stack hasta MainActivity
     * Si el usuario presiona "atrás" desde Mis Pedidos, volverá al MainActivity
     */
    private void irAMisPedidos() {
        Intent intent = new Intent(this, MisPedidosActivity.class);

        // Limpiar todas las actividades intermedias y volver al MainActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);

        // Cerrar esta actividad
        finish();
    }
}