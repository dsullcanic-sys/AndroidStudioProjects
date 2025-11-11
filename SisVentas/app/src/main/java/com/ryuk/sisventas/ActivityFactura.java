package com.ryuk.sisventas;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ActivityFactura extends AppCompatActivity {

    private TableLayout tablaFactura;
    private TextView txtTitulo, txtTotal;
    private Button btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_factura);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar vistas
        txtTitulo = findViewById(R.id.txtTitulo);
        tablaFactura = findViewById(R.id.tablaFactura);
        txtTotal = findViewById(R.id.txtTotal);
        btnVolver = findViewById(R.id.btnVolver);

        // Obtener datos del Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int total = extras.getInt("total");
            String[] nombres = extras.getStringArray("nombres");
            int[] precios = extras.getIntArray("precios");
            int[] comprados = extras.getIntArray("comprados");

            // Mostrar la factura en tabla
            mostrarFacturaEnTabla(total, nombres, precios, comprados);
        }

        // Configurar botón para retornar
        btnVolver.setOnClickListener(v -> retornar(v));
    }

    private void mostrarFacturaEnTabla(int total, String[] nombres, int[] precios, int[] comprados) {
        // Limpiar tabla si tiene datos previos
        tablaFactura.removeAllViews();

        // Crear fila de encabezados
        TableRow headerRow = new TableRow(this);

        TextView header1 = new TextView(this);
        header1.setText("Producto");
        header1.setPadding(8, 8, 8, 8);
        header1.setTypeface(null, Typeface.BOLD); // CORRECCIÓN AQUÍ

        TextView header2 = new TextView(this);
        header2.setText("Cant");
        header2.setPadding(8, 8, 8, 8);
        header2.setTypeface(null, Typeface.BOLD); // CORRECCIÓN AQUÍ
        header2.setGravity(Gravity.CENTER);

        TextView header3 = new TextView(this);
        header3.setText("Precio");
        header3.setPadding(8, 8, 8, 8);
        header3.setTypeface(null, Typeface.BOLD); // CORRECCIÓN AQUÍ
        header3.setGravity(Gravity.END);

        TextView header4 = new TextView(this);
        header4.setText("Subtotal");
        header4.setPadding(8, 8, 8, 8);
        header4.setTypeface(null, Typeface.BOLD); // CORRECCIÓN AQUÍ
        header4.setGravity(Gravity.END);

        headerRow.addView(header1);
        headerRow.addView(header2);
        headerRow.addView(header3);
        headerRow.addView(header4);

        tablaFactura.addView(headerRow);

        // Agregar línea divisoria
        TableRow dividerRow = new TableRow(this);
        TextView divider = new TextView(this);
        divider.setText("------------------------------------------------");
        divider.setPadding(8, 4, 8, 4);
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.span = 4; // Ocupa todas las columnas
        divider.setLayoutParams(params);
        dividerRow.addView(divider);
        tablaFactura.addView(dividerRow);

        // Llenar tabla con datos
        for (int j = 0; j < nombres.length; j++) {
            if (comprados[j] > 0) {
                int subtotal = precios[j] * comprados[j];

                TableRow row = new TableRow(this);

                TextView tvProducto = new TextView(this);
                tvProducto.setText(nombres[j]);
                tvProducto.setPadding(8, 8, 8, 8);

                TextView tvCantidad = new TextView(this);
                tvCantidad.setText(String.valueOf(comprados[j]));
                tvCantidad.setPadding(8, 8, 8, 8);
                tvCantidad.setGravity(Gravity.CENTER);

                TextView tvPrecio = new TextView(this);
                tvPrecio.setText(precios[j] + " Bs");
                tvPrecio.setPadding(8, 8, 8, 8);
                tvPrecio.setGravity(Gravity.END);

                TextView tvSubtotal = new TextView(this);
                tvSubtotal.setText(subtotal + " Bs");
                tvSubtotal.setPadding(8, 8, 8, 8);
                tvSubtotal.setGravity(Gravity.END);

                row.addView(tvProducto);
                row.addView(tvCantidad);
                row.addView(tvPrecio);
                row.addView(tvSubtotal);

                tablaFactura.addView(row);
            }
        }

        // Mostrar total
        txtTotal.setText("TOTAL: " + total + " Bs");
    }

    public void retornar(View vista){
        // Indicar que la operación fue exitosa antes de cerrar
        setResult(RESULT_OK);
        finish();
    }
}