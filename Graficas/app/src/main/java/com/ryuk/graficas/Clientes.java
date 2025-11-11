package com.ryuk.graficas;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import android.graphics.Color;
import java.util.*;

public class Clientes extends AppCompatActivity {
    EditText edtCiBuscar;
    Button btnBuscarCliente, btnLimpiarCliente;
    TextView tvResultadoCliente;
    TableLayout tableClientes;
    BarChart barChartClientes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        edtCiBuscar = findViewById(R.id.edtCiBuscar);
        btnBuscarCliente = findViewById(R.id.btnBuscarCliente);
        btnLimpiarCliente = findViewById(R.id.btnLimpiarCliente);
        tvResultadoCliente = findViewById(R.id.tvResultadoCliente);
        tableClientes = findViewById(R.id.tableClientes);
        barChartClientes = findViewById(R.id.barChartClientes);

        // Todo oculto al inicio
        findViewById(R.id.scrollClientesVertical).setVisibility(View.GONE);
        barChartClientes.setVisibility(View.GONE);
        tvResultadoCliente.setVisibility(View.GONE);

        btnBuscarCliente.setOnClickListener(v -> buscarCliente());
        btnLimpiarCliente.setOnClickListener(v -> limpiarBusqueda());
    }

    private void buscarCliente() {
        String query = edtCiBuscar.getText().toString().trim().toLowerCase();

        // Ocultar todo
        findViewById(R.id.scrollClientesVertical).setVisibility(View.GONE);
        barChartClientes.setVisibility(View.GONE);
        tvResultadoCliente.setVisibility(View.GONE);

        if (query.isEmpty()) {
            // Mostrar TODOS
            findViewById(R.id.scrollClientesVertical).setVisibility(View.VISIBLE);
            barChartClientes.setVisibility(View.VISIBLE);
            mostrarTodosClientes();
            mostrarGraficoBarrasRangos();
            return;
        }

        MainActivity.Cliente encontrado = null;
        for (MainActivity.Cliente cl : MainActivity.listaClientes) {
            if (cl.carnet.equalsIgnoreCase(query) ||
                    cl.nombres.toLowerCase().contains(query)) {
                encontrado = cl; break;
            }
        }

        if (encontrado != null) {
            findViewById(R.id.scrollClientesVertical).setVisibility(View.VISIBLE);
            barChartClientes.setVisibility(View.VISIBLE);
            mostrarClienteTablaUnica(encontrado);
            mostrarGraficoCliente(encontrado);
        } else {
            tvResultadoCliente.setVisibility(View.VISIBLE);
            tvResultadoCliente.setText("No se encontró ese cliente.");
        }
    }

    private void limpiarBusqueda() {
        edtCiBuscar.setText("");
        tvResultadoCliente.setVisibility(View.GONE);
        // Mostrar TODOS
        findViewById(R.id.scrollClientesVertical).setVisibility(View.VISIBLE);
        barChartClientes.setVisibility(View.VISIBLE);
        mostrarTodosClientes();
        mostrarGraficoBarrasRangos();
    }

    // Tabla para TODOS
    private void mostrarTodosClientes() {
        tableClientes.removeAllViews();

        TableRow cabecera = new TableRow(this);
        cabecera.addView(crearCelda("Carnet"));
        cabecera.addView(crearCelda("Nombre"));
        cabecera.addView(crearCelda("Profesión"));
        cabecera.addView(crearCelda("Departamento"));
        cabecera.addView(crearCelda("Nro. Cuentas"));
        cabecera.addView(crearCelda("Saldo total"));
        tableClientes.addView(cabecera);

        for (MainActivity.Cliente cl : MainActivity.listaClientes) {
            String profesion = "";
            for (MainActivity.Profesion p : MainActivity.listaProfesiones)
                if (p.codprof.equals(cl.codprof)) { profesion = p.descripcion; break; }
            String departamento = "";
            for (MainActivity.Departamento d : MainActivity.listaDepartamentos)
                if (d.codepto.equals(cl.codepto)) { departamento = d.descripcion; break; }
            int nroCuentas = 0;
            double saldoTotal = 0;
            ArrayList<String> cuentas = new ArrayList<>();
            for (MainActivity.Cuenta c : MainActivity.listaCuentas)
                if (c.carnet.equals(cl.carnet)) cuentas.add(c.cuenta);
            nroCuentas = cuentas.size();
            for (MainActivity.Movimiento m : MainActivity.listaMovimientos)
                if (cuentas.contains(m.cuenta))
                    try { saldoTotal += Double.parseDouble(m.monto); } catch (Exception e) {}

            TableRow fila = new TableRow(this);
            fila.addView(crearCelda(cl.carnet));
            fila.addView(crearCelda(cl.nombres));
            fila.addView(crearCelda(profesion));
            fila.addView(crearCelda(departamento));
            fila.addView(crearCelda(String.valueOf(nroCuentas)));
            fila.addView(crearCelda(String.valueOf(saldoTotal)));
            tableClientes.addView(fila);
        }
    }

    // Tabla para un SOLO cliente (en columnas, como lista)
    private void mostrarClienteTablaUnica(MainActivity.Cliente cl) {
        tableClientes.removeAllViews();

        String profesion = "";
        for (MainActivity.Profesion p : MainActivity.listaProfesiones)
            if (p.codprof.equals(cl.codprof)) { profesion = p.descripcion; break; }
        String departamento = "";
        for (MainActivity.Departamento d : MainActivity.listaDepartamentos)
            if (d.codepto.equals(cl.codepto)) { departamento = d.descripcion; break; }
        ArrayList<String> cuentas = new ArrayList<>();
        for (MainActivity.Cuenta c : MainActivity.listaCuentas)
            if (c.carnet.equals(cl.carnet)) cuentas.add(c.cuenta);
        int nroCuentas = cuentas.size();
        double saldoTotal = 0.0;
        for (MainActivity.Movimiento m : MainActivity.listaMovimientos)
            if (cuentas.contains(m.cuenta))
                try { saldoTotal += Double.parseDouble(m.monto); } catch (Exception e) {}

        // Cada celda por fila (diseño de columna de datos)
        tableClientes.addView(filaDatos("Nombre:", cl.nombres));
        tableClientes.addView(filaDatos("Profesión:", profesion));
        tableClientes.addView(filaDatos("Departamento:", departamento));
        tableClientes.addView(filaDatos("Nro de Cuentas:", String.valueOf(nroCuentas)));
        tableClientes.addView(filaDatos("Saldo total:", String.valueOf(saldoTotal)));
    }

    // Fila con dos columnas (titulo y dato)
    private TableRow filaDatos(String label, String dato) {
        TableRow fila = new TableRow(this);
        fila.addView(crearCelda(label));
        fila.addView(crearCelda(dato));
        return fila;
    }

    private TextView crearCelda(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(20, 10, 20, 10);
        tv.setTextSize(16);
        return tv;
    }

    // Gráfico de cliente único
    private void mostrarGraficoCliente(MainActivity.Cliente cl) {
        ArrayList<String> cuentas = new ArrayList<>();
        for (MainActivity.Cuenta c : MainActivity.listaCuentas)
            if (c.carnet.equals(cl.carnet)) cuentas.add(c.cuenta);
        double saldoTotal = 0.0;
        for (MainActivity.Movimiento m : MainActivity.listaMovimientos)
            if (cuentas.contains(m.cuenta))
                try { saldoTotal += Double.parseDouble(m.monto); } catch (Exception e) {}

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) saldoTotal));
        ArrayList<String> labels = new ArrayList<>();
        labels.add(cl.nombres);

        BarDataSet dataSet = new BarDataSet(entries, "Saldo del cliente");
        dataSet.setColors(Color.rgb(33,150,243));
        dataSet.setValueTextColor(Color.BLACK);
        BarData data = new BarData(dataSet);
        barChartClientes.setData(data);
        barChartClientes.getDescription().setEnabled(false);
        barChartClientes.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartClientes.getXAxis().setGranularity(1f);
        barChartClientes.getXAxis().setGranularityEnabled(true);
        barChartClientes.getXAxis().setLabelRotationAngle(-45f);
        barChartClientes.invalidate();
    }

    // Gráfico de todos
    private void mostrarGraficoBarrasRangos() {
        int rango = 3000; // Cambiado a 3,000 por tu preferencia
        double saldoMax = 0;
        ArrayList<Double> saldosClientes = new ArrayList<>();

        // Calcular el saldo máximo
        for (MainActivity.Cliente cl : MainActivity.listaClientes) {
            double saldoTotal = 0;
            ArrayList<String> cuentas = new ArrayList<>();
            for (MainActivity.Cuenta c : MainActivity.listaCuentas)
                if (c.carnet.equals(cl.carnet)) cuentas.add(c.cuenta);
            for (MainActivity.Movimiento m : MainActivity.listaMovimientos)
                if (cuentas.contains(m.cuenta))
                    try { saldoTotal += Double.parseDouble(m.monto); } catch (Exception e) {}
            saldosClientes.add(saldoTotal);
            if (saldoTotal > saldoMax) saldoMax = saldoTotal;
        }

        // Crear rangos usando solo el límite superior
        LinkedHashMap<String, Integer> rangos = new LinkedHashMap<>();
        int hasta = rango;
        while (hasta <= saldoMax + rango) { // +rango para que incluya el último
            rangos.put(formatK(hasta), 0);
            hasta += rango;
        }

        // Distribuir los saldos en rangos
        for (double saldo : saldosClientes) {
            int limite = rango;
            for (String key : rangos.keySet()) {
                if (saldo < limite) {
                    rangos.put(key, rangos.get(key) + 1);
                    break;
                }
                limite += rango;
            }
        }

        // Armar los datos y etiquetas para el gráfico
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        for (String etiqueta : rangos.keySet()) {
            entries.add(new BarEntry(i, rangos.get(etiqueta)));
            labels.add(etiqueta);
            i++;
        }
        BarDataSet dataSet = new BarDataSet(entries, "Clientes por saldo (Bs)");
        dataSet.setColors(Color.rgb(33,150,243), Color.rgb(255,193,7), Color.rgb(244,67,54), Color.rgb(76,175,80));
        dataSet.setValueTextColor(Color.BLACK);
        BarData data = new BarData(dataSet);
        barChartClientes.setData(data);
        barChartClientes.getDescription().setEnabled(false);
        barChartClientes.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartClientes.getXAxis().setGranularity(1f);
        barChartClientes.getXAxis().setGranularityEnabled(true);
        barChartClientes.getXAxis().setLabelRotationAngle(-45f);
        barChartClientes.invalidate();
    }

    // Etiqueta compacta en "k"
    private String formatK(int n) {
        if (n >= 1000) {
            double val = n / 1000.0;
            if (val == (int) val) {
                return ((int) val) + "k";
            } else {
                return String.format("%.1fk", val);
            }
        }
        return String.valueOf(n);
    }

}
