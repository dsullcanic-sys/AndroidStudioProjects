package com.ryuk.sisventas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;

public class ActivityHistorial extends AppCompatActivity {

    private LinearLayout layoutHistorial;
    private ArrayList<String> fechas;
    private ArrayList<Integer> totales;
    private int numFacturas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historial);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        layoutHistorial = findViewById(R.id.layoutHistorial);

        // Obtener datos del historial
        fechas = getIntent().getStringArrayListExtra("fechas");
        totales = getIntent().getIntegerArrayListExtra("totales");
        numFacturas = getIntent().getIntExtra("num_facturas", 0);

        if (fechas != null && !fechas.isEmpty()) {
            mostrarHistorial();
        }
    }

    private void mostrarHistorial() {
        for (int i = 0; i < fechas.size(); i++) {
            final int index = i; // Necesario para el listener

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(16, 16, 16, 16);
            itemLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

            TextView tvFecha = new TextView(this);
            tvFecha.setText("Factura del: " + fechas.get(i) + "\nTotal: " + totales.get(i) + " Bs");
            tvFecha.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvFecha.setTextSize(14);

            Button btnVer = new Button(this);
            btnVer.setText("VER");
            btnVer.setOnClickListener(v -> verFactura(index));

            itemLayout.addView(tvFecha);
            itemLayout.addView(btnVer);

            layoutHistorial.addView(itemLayout);
        }
    }

    private void verFactura(int index) {
        // Obtener los datos de esta factura específica
        String[] nombresFactura = getIntent().getStringArrayExtra("nombres_" + index);
        int[] preciosFactura = getIntent().getIntArrayExtra("precios_" + index);
        int[] compradosFactura = getIntent().getIntArrayExtra("comprados_" + index);
        int totalFactura = totales.get(index);

        // Abrir ActivityFactura con los datos
        Intent intent = new Intent(this, ActivityFactura.class);
        intent.putExtra("total", totalFactura);
        intent.putExtra("nombres", nombresFactura);
        intent.putExtra("precios", preciosFactura);
        intent.putExtra("comprados", compradosFactura);
        startActivity(intent);
    }

    public void retornar(View vista) {
        finish();
    }
}