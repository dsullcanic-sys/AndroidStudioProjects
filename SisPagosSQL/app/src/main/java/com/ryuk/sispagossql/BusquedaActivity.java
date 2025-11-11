package com.ryuk.sispagossql;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class BusquedaActivity extends AppCompatActivity {

    private EditText etBuscar;
    private LinearLayout llResultados;
    private String[] meses = {"MARZO 2024", "ABRIL 2024", "MAYO 2024"};
    private String[] mesesCod = {"0324", "0424", "0524"};
    private DBHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busqueda);

        // Referenciar elementos del XML
        etBuscar = findViewById(R.id.etBuscar);
        llResultados = findViewById(R.id.llResultados);
        Button btnBuscar = findViewById(R.id.btnBuscar);
        Button btnVolver = findViewById(R.id.btnVolver);

        // Configurar listeners
        btnBuscar.setOnClickListener(v -> buscar());
        btnVolver.setOnClickListener(v -> finish());
    }

    private void buscar() {
        llResultados.removeAllViews();
        String termino = etBuscar.getText().toString().trim();

        if (termino.isEmpty()) {
            llResultados.addView(crearTexto("Ingrese un término de búsqueda", 16, false));
            return;
        }

        SQLiteDatabase db = openOrCreateDatabase(MainActivity.getDbName(), MODE_PRIVATE, null);
        helper = new DBHelper(db);

        try {
            // Buscar empleado usando la clase modelo
            Empleado emp = helper.buscarEmpleadoPorNombre(termino);

            if (emp == null) {
                llResultados.addView(crearTexto("No se encontraron resultados", 16, false));
                db.close();
                return;
            }

            // Card de datos personales usando objeto Empleado
            LinearLayout cardPersonal = crearCard();
            cardPersonal.addView(crearTexto("CARNET: " + emp.carnet, 18, true));
            cardPersonal.addView(crearTexto("NOMBRE: " + emp.nombres + " " + emp.paterno + " " + emp.materno, 16, false));
            llResultados.addView(cardPersonal);

            // Datos por mes usando clases
            double totalBonos = 0, totalDesc = 0, totalLiq = 0;

            for (int i = 0; i < meses.length; i++) {
                LinearLayout cardMes = crearCard();
                cardMes.addView(crearTexto(meses[i], 18, true));

                // Buscar cargo del mes usando DBHelper
                String cargoDesc = "Sin datos";
                double sueldo = 0;

                Integer idCargo = helper.buscarCargoPlanilla(emp.carnet, mesesCod[i]);
                if (idCargo != null) {
                    Cargo cargo = helper.buscarCargo(idCargo);
                    if (cargo != null) {
                        cargoDesc = cargo.descripcion;
                        sueldo = cargo.basico;
                    }
                }

                // Bonos y descuentos usando DBHelper
                double bonos = helper.sumarMontos("bon" + mesesCod[i], emp.carnet);
                double desc = helper.sumarMontos("des" + mesesCod[i], emp.carnet);
                double liquido = sueldo + bonos - desc;

                // Mostrar datos
                cardMes.addView(crearTexto("Cargo: " + cargoDesc, 14, false));
                cardMes.addView(crearTexto("Sueldo: Bs. " + String.format("%.2f", sueldo), 14, false));
                cardMes.addView(crearTexto("Bonos: Bs. " + String.format("%.2f", bonos), 14, false));
                cardMes.addView(crearTexto("Descuentos: Bs. " + String.format("%.2f", desc), 14, false));
                cardMes.addView(crearTexto("LÍQUIDO: Bs. " + String.format("%.2f", liquido), 16, true));

                llResultados.addView(cardMes);

                totalBonos += bonos;
                totalDesc += desc;
                totalLiq += liquido;
            }

            // Resumen total
            LinearLayout cardTotal = crearCard();
            cardTotal.addView(crearTexto("RESUMEN TOTAL", 18, true));
            cardTotal.addView(crearTexto("Total Bonos: Bs. " + String.format("%.2f", totalBonos), 14, false));
            cardTotal.addView(crearTexto("Total Descuentos: Bs. " + String.format("%.2f", totalDesc), 14, false));
            cardTotal.addView(crearTexto("TOTAL LÍQUIDO: Bs. " + String.format("%.2f", totalLiq), 16, true));
            llResultados.addView(cardTotal);

        } catch (Exception e) {
            llResultados.addView(crearTexto("Error: " + e.getMessage(), 14, false));
        }

        db.close();
    }

    private LinearLayout crearCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 15);
        card.setLayoutParams(params);

        return card;
    }

    private TextView crearTexto(String texto, int size, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(size);
        tv.setPadding(0, 5, 0, 5);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }
}