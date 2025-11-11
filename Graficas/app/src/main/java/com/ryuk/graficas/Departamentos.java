package com.ryuk.graficas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieData;
import android.graphics.Color;
import java.util.ArrayList;

public class Departamentos extends AppCompatActivity {
    EditText edtBuscarDepartamento;
    Button btnBuscarDepartamento, btnLimpiarDepartamento;
    TextView tvResultadoDepartamento;
    TableLayout tableDepartamentos;
    PieChart pieChartDepartamento;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_departamentos);

        edtBuscarDepartamento = findViewById(R.id.edtBuscarDepartamento);
        btnBuscarDepartamento = findViewById(R.id.btnBuscarDepartamento);
        btnLimpiarDepartamento = findViewById(R.id.btnLimpiarDepartamento);
        tvResultadoDepartamento = findViewById(R.id.tvResultadoDepartamento);
        tableDepartamentos = findViewById(R.id.tableDepartamentos);
        pieChartDepartamento = findViewById(R.id.pieChartDepartamento);

        tvResultadoDepartamento.setVisibility(View.GONE);
        tableDepartamentos.setVisibility(View.GONE);
        pieChartDepartamento.setVisibility(View.GONE);

        btnBuscarDepartamento.setOnClickListener(v -> buscarDepartamento());
        btnLimpiarDepartamento.setOnClickListener(v -> limpiarBusqueda());
    }

    private void buscarDepartamento() {
        String query = edtBuscarDepartamento.getText().toString().trim();
        tvResultadoDepartamento.setVisibility(View.GONE);
        tableDepartamentos.removeAllViews();
        tableDepartamentos.setVisibility(View.GONE);
        pieChartDepartamento.setVisibility(View.GONE);

        if (query.isEmpty()) {
            tableDepartamentos.setVisibility(View.VISIBLE);
            pieChartDepartamento.setVisibility(View.VISIBLE);
            mostrarTablaYGrafico(MainActivity.listaDepartamentos, false);
            return;
        }

        ArrayList<MainActivity.Departamento> deptosCoincidentes = new ArrayList<>();
        for (MainActivity.Departamento d : MainActivity.listaDepartamentos) {
            if (d.codepto.equalsIgnoreCase(query) || d.descripcion.toLowerCase().contains(query.toLowerCase()))
                deptosCoincidentes.add(d);
        }

        if (deptosCoincidentes.isEmpty()) {
            tvResultadoDepartamento.setText("No se encontró departamento.");
            tvResultadoDepartamento.setVisibility(View.VISIBLE);
            return;
        }

        tableDepartamentos.setVisibility(View.VISIBLE);
        pieChartDepartamento.setVisibility(View.VISIBLE);

        if (deptosCoincidentes.size() == 1) {
            mostrarUnDepartamentoEnTabla(deptosCoincidentes.get(0));
            mostrarPieUnDepartamento(deptosCoincidentes.get(0));
        } else {
            mostrarTablaYGrafico(deptosCoincidentes, true);
        }
    }

    // Ficha para un solo resultado
    private void mostrarUnDepartamentoEnTabla(MainActivity.Departamento d) {
        tableDepartamentos.removeAllViews();
        tableDepartamentos.addView(filaDato("ID:", d.codepto));
        tableDepartamentos.addView(filaDato("Descripción:", d.descripcion));
        int nroCuentas = 0;
        double saldoTotal = 0;
        for (MainActivity.Cliente cl : MainActivity.listaClientes)
            if (cl.codepto.equals(d.codepto))
                for (MainActivity.Cuenta c : MainActivity.listaCuentas)
                    if (c.carnet.equals(cl.carnet)) {
                        nroCuentas++;
                        for (MainActivity.Movimiento mov : MainActivity.listaMovimientos)
                            if (mov.cuenta.equals(c.cuenta))
                                try { saldoTotal += Double.parseDouble(mov.monto); } catch (Exception e) {}
                    }
        tableDepartamentos.addView(filaDato("Nro Cuentas:", String.valueOf(nroCuentas)));
        tableDepartamentos.addView(filaDato("Saldo Total:", String.valueOf(saldoTotal)));
    }

    private TableRow filaDato(String label, String dato) {
        TableRow fila = new TableRow(this);
        fila.addView(crearCelda(label));
        fila.addView(crearCelda(dato));
        return fila;
    }

    // Pie único dato
    private void mostrarPieUnDepartamento(MainActivity.Departamento d) {
        double saldoTotal = 0;
        for (MainActivity.Cliente cl : MainActivity.listaClientes)
            if (cl.codepto.equals(d.codepto))
                for (MainActivity.Cuenta c : MainActivity.listaCuentas)
                    if (c.carnet.equals(cl.carnet))
                        for (MainActivity.Movimiento mov : MainActivity.listaMovimientos)
                            if (mov.cuenta.equals(c.cuenta))
                                try { saldoTotal += Double.parseDouble(mov.monto); } catch (Exception e) {}

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) saldoTotal, d.descripcion));

        PieDataSet dataSet = new PieDataSet(entries, "Saldo por departamento");
        dataSet.setColors(Color.rgb(255,193,7));
        dataSet.setValueTextColor(
                (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES ? Color.WHITE : Color.BLACK
        );
        PieData data = new PieData(dataSet);
        pieChartDepartamento.setData(data);
        pieChartDepartamento.getDescription().setEnabled(false);
        pieChartDepartamento.setCenterText("Saldo total por departamento");
        pieChartDepartamento.invalidate();
    }

    // Tabla resumen/todos o varias coincidencias, clásico con cabecera
    private void mostrarTablaYGrafico(ArrayList<MainActivity.Departamento> lista, boolean soloMostrarCoincidentes) {
        tableDepartamentos.removeAllViews();
        TableRow cabecera = new TableRow(this);
        cabecera.addView(crearCelda("ID"));
        cabecera.addView(crearCelda("Descripción"));
        cabecera.addView(crearCelda("Nro Cuentas"));
        cabecera.addView(crearCelda("Saldo Total"));
        tableDepartamentos.addView(cabecera);

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (MainActivity.Departamento d : lista) {
            int nroCuentas = 0;
            double saldoTotal = 0;
            for (MainActivity.Cliente cl : MainActivity.listaClientes)
                if (cl.codepto.equals(d.codepto))
                    for (MainActivity.Cuenta c : MainActivity.listaCuentas)
                        if (c.carnet.equals(cl.carnet)) {
                            nroCuentas++;
                            for (MainActivity.Movimiento mov : MainActivity.listaMovimientos)
                                if (mov.cuenta.equals(c.cuenta))
                                    try { saldoTotal += Double.parseDouble(mov.monto); } catch (Exception e) {}
                        }
            TableRow fila = new TableRow(this);
            fila.addView(crearCelda(d.codepto));
            fila.addView(crearCelda(d.descripcion));
            fila.addView(crearCelda(String.valueOf(nroCuentas)));
            fila.addView(crearCelda(String.valueOf(saldoTotal)));
            tableDepartamentos.addView(fila);

            entries.add(new PieEntry((float) saldoTotal, d.descripcion));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Saldo por departamento");
        dataSet.setColors(
                Color.rgb(244,67,54),
                Color.rgb(33,150,243),
                Color.rgb(76,175,80),
                Color.rgb(255,193,7));
        dataSet.setValueTextColor(
                (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES ? Color.WHITE : Color.BLACK
        );
        PieData data = new PieData(dataSet);
        pieChartDepartamento.setData(data);
        pieChartDepartamento.getDescription().setEnabled(false);
        pieChartDepartamento.setCenterText("Saldo total por departamento");
        pieChartDepartamento.invalidate();
    }

    private TextView crearCelda(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(12, 8, 12, 8);
        tv.setTextSize(15);
        return tv;
    }

    private void limpiarBusqueda() {
        edtBuscarDepartamento.setText("");
        tvResultadoDepartamento.setVisibility(View.GONE);
        tableDepartamentos.removeAllViews();
        pieChartDepartamento.clear();
        tableDepartamentos.setVisibility(View.GONE);
        pieChartDepartamento.setVisibility(View.GONE);
    }
}
