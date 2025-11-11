package com.ryuk.sisventas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import androidx.appcompat.app.AlertDialog;



public class MainActivity extends AppCompatActivity {

    // Arrays de datos de los platos
    int[] platos = {
            R.drawable.p01, R.drawable.p02, R.drawable.p03, R.drawable.p04, R.drawable.p05,
            R.drawable.p06, R.drawable.p07, R.drawable.p08, R.drawable.p09, R.drawable.p10,
            R.drawable.p11, R.drawable.p12, R.drawable.p13, R.drawable.p14, R.drawable.p15,
            R.drawable.p16, R.drawable.p17, R.drawable.p18, R.drawable.p19, R.drawable.p20,
            R.drawable.p21, R.drawable.p22, R.drawable.p23, R.drawable.p24, R.drawable.p25,
            R.drawable.p26
    };

    String[] nombres = {
            "Charquekan", "Papas a la Huancaina", "Majadito", "Pique Macho", "Hamburguesa",
            "Silpancho", "Plato Paceño", "Sajta", "Milanesa de Pollo", "Ramen",
            "Pollo al Horno", "Salchipapa", "Calamar", "Chicharon de Pollo", "Asado",
            "Chicharon de Cerdo", "Ispi", "Chairo", "Pure de Papa", "Aji de Fideo",
            "Sushi 1", "Sushi 2", "Aji de Racacha", "Biffe", "Salmon", "Silpancho Cochabambino"
    };

    int[] precios = {
            35, 20, 25, 40, 18, 28, 30, 27, 32, 45,
            33, 15, 60, 28, 55, 40, 22, 18, 12, 20,
            55, 75, 15, 45, 70, 35
    };

    int[] comprados = new int[26];
    int i = 0;
    int total = 0;
    TextView codigo, Descripcion, unidades, totalTextView;
    ImageView Imagen;
    Button btnAnterior, btnDevolver, btnFactura, bntComprar, btnSiguiente;

    private ArrayList<String> fechasHistorial = new ArrayList<>();
    private ArrayList<Integer> totalesHistorial = new ArrayList<>();
    private ArrayList<String[]> nombresHistorial = new ArrayList<>();
    private ArrayList<int[]> preciosHistorial = new ArrayList<>();
    private ArrayList<int[]> compradosHistorial = new ArrayList<>();

    private Button btnHistorial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Inicializar vistas
        codigo = findViewById(R.id.codigo);
        Descripcion = findViewById(R.id.Descripcion);
        unidades = findViewById(R.id.unidades);
        totalTextView = findViewById(R.id.total);
        Imagen = findViewById(R.id.Imagen);

        btnAnterior = findViewById(R.id.btnAnterior);
        btnDevolver = findViewById(R.id.btnDevolver);
        btnFactura = findViewById(R.id.btnFactura);
        bntComprar = findViewById(R.id.btnComprar);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        btnHistorial = findViewById(R.id.btnHistorial);

        // Configurar listeners de botones
        btnAnterior.setOnClickListener(v -> anterior());
        btnDevolver.setOnClickListener(v -> devolver());
        btnFactura.setOnClickListener(v -> factura());
        bntComprar.setOnClickListener(v -> comprar());
        btnSiguiente.setOnClickListener(v -> siguiente());
        btnHistorial.setOnClickListener(v -> verHistorial());

        // Mostrar el primer plato
        actualizarVista();
    }

    private void actualizarVista() {
        Descripcion.setText(nombres[i] + "  " + precios[i] + " Bs");
        Imagen.setImageResource(platos[i]);
        unidades.setText("Comprados: " + comprados[i]);
        totalTextView.setText("Total: " + total + " Bs");
    }

    private void anterior() {
        i--;
        if (i < 0) i = platos.length - 1;
        actualizarVista();
    }

    private void siguiente() {
        i++;
        if (i >= platos.length) i = 0;
        actualizarVista();
    }

    private void comprar() {
        comprados[i]++;
        total += precios[i];
        actualizarVista();
    }

    private void devolver() {
        if (comprados[i] > 0) {
            comprados[i]--;
            total -= precios[i];
            actualizarVista();
        }
    }

    /*private void factura() {
        if (total == 0) {
            Toast.makeText(this, "No hay productos en la factura", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder factura = new StringBuilder("TOTAL: " + total + " Bs\n\n");
        for (int j = 0; j < platos.length; j++) {
            if (comprados[j] > 0) {
                int subtotal = precios[j] * comprados[j];
                factura.append(nombres[j]).append(": ")
                        .append(comprados[j]).append(" x ")
                        .append(precios[j]).append("Bs = ")
                        .append(subtotal).append("Bs\n");
            }
        }

        // Usar AlertDialog en lugar de Toast
        new AlertDialog.Builder(this)
                .setTitle("📄 FACTURA")
                .setMessage(factura.toString())
                .setPositiveButton("Aceptar", null)
                .show();
    }*/
    private void factura() {
        if (total == 0) {
            Toast.makeText(this, "No hay productos en la factura", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar en historial ANTES de mostrar
        guardarFacturaEnHistorial();

        Intent vf = new Intent(this, ActivityFactura.class);
        vf.putExtra("total", total);
        vf.putExtra("nombres", nombres);
        vf.putExtra("precios", precios);
        vf.putExtra("comprados", comprados);

        // Usar startActivityForResult en lugar de startActivity
        startActivityForResult(vf, 1); // 1 es un código de solicitud
    }
    private void guardarFacturaEnHistorial() {
        // Guardar fecha actual
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        fechasHistorial.add(fecha);

        // Guardar total
        totalesHistorial.add(total);

        // Guardar copias de los arrays
        nombresHistorial.add(nombres.clone());
        preciosHistorial.add(precios.clone());
        compradosHistorial.add(comprados.clone());

        //Toast.makeText(this, "Factura guardada en historial", Toast.LENGTH_SHORT).show();
    }

    private void verHistorial() {
        if (fechasHistorial.isEmpty()) {
            Toast.makeText(this, "No hay facturas en el historial", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ActivityHistorial.class);

        // Pasar todos los datos del historial
        intent.putStringArrayListExtra("fechas", fechasHistorial);
        intent.putIntegerArrayListExtra("totales", totalesHistorial);

        // Pasar los arrays de arrays (esto es lo nuevo)
        intent.putExtra("num_facturas", fechasHistorial.size());

        // Pasar cada array individualmente
        for (int i = 0; i < fechasHistorial.size(); i++) {
            intent.putExtra("nombres_" + i, nombresHistorial.get(i));
            intent.putExtra("precios_" + i, preciosHistorial.get(i));
            intent.putExtra("comprados_" + i, compradosHistorial.get(i));
        }

        startActivity(intent);
    }

    // Método auxiliar para convertir ArrayList<Integer> a int[]
    private int[] convertToIntArray(ArrayList<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    // Método para resetear las selecciones
    private void resetearSelecciones() {
        // Resetear array de comprados
        for (int j = 0; j < comprados.length; j++) {
            comprados[j] = 0;
        }

        // Resetear total
        total = 0;

        // Actualizar vista
        actualizarVista();

       //Toast.makeText(this, "Selecciones reseteadas", Toast.LENGTH_SHORT).show();
    }

    // Método para manejar el resultado de ActivityFactura
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) { // Mismo código que usamos en startActivityForResult
            if (resultCode == RESULT_OK) {
                // Solo resetear si la factura se cerró correctamente
                resetearSelecciones();
            }
        }
    }
}