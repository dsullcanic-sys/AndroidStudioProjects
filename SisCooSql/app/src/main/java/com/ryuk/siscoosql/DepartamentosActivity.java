package com.ryuk.siscoosql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import java.util.ArrayList;
import java.util.Locale;

public class DepartamentosActivity extends AppCompatActivity {
    EditText edtBuscarDepartamento;
    Button btnBuscarDepartamento, btnLimpiarDepartamento;
    TextView tvResultadoDepartamento;
    TableLayout tableDepartamentos;
    PieChart pieChartDepartamento;

    private DBHelper dbHelper;
    private SQLiteDatabase db;

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

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        // Inicial: todo limpio
        limpiarVista();

        btnBuscarDepartamento.setOnClickListener(v -> buscarDepartamento());
        btnLimpiarDepartamento.setOnClickListener(v -> limpiarVista());
    }

    private void buscarDepartamento() {
        String query = edtBuscarDepartamento.getText().toString().trim();
        tvResultadoDepartamento.setVisibility(TextView.GONE);

        if (query.isEmpty()) {
            // Mostrar todos: resumen
            mostrarTablaYGraficoGeneral();
            return;
        }

        tableDepartamentos.removeAllViews();
        pieChartDepartamento.setVisibility(PieChart.GONE);

        Cursor cursor = db.rawQuery(
                "SELECT codepto, descripcion FROM Departamentos WHERE codepto=? OR lower(descripcion) LIKE ?",
                new String[]{query, "%" + query.toLowerCase(Locale.US) + "%"});
        ArrayList<Integer> idsCoincidentes = new ArrayList<>();
        while (cursor.moveToNext()) {
            idsCoincidentes.add(cursor.getInt(0));
        }
        cursor.close();

        if (idsCoincidentes.isEmpty()) {
            tvResultadoDepartamento.setText("No se encontró departamento.");
            tvResultadoDepartamento.setVisibility(TextView.VISIBLE);
            tableDepartamentos.setVisibility(TableLayout.GONE);
            pieChartDepartamento.setVisibility(PieChart.GONE);
            return;
        }

        // Mostrar coincidencias
        mostrarTablaYGrafico(idsCoincidentes);
    }

    private void limpiarVista() {
        edtBuscarDepartamento.setText("");
        tvResultadoDepartamento.setVisibility(TextView.GONE);
        tableDepartamentos.setVisibility(TableLayout.GONE);
        pieChartDepartamento.setVisibility(PieChart.GONE);
    }

    private void mostrarTablaYGraficoGeneral() {
        tableDepartamentos.setVisibility(TableLayout.VISIBLE);
        pieChartDepartamento.setVisibility(PieChart.VISIBLE);

        tableDepartamentos.removeAllViews();
        TableRow cabecera = new TableRow(this);
        cabecera.addView(crearCelda("ID"));
        cabecera.addView(crearCelda("Descripción"));
        cabecera.addView(crearCelda("Nro Cuentas"));
        cabecera.addView(crearCelda("Saldo Total"));
        tableDepartamentos.addView(cabecera);

        ArrayList<PieEntry> entries = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT codepto, descripcion FROM Departamentos ORDER BY descripcion", null);
        while (cursor.moveToNext()) {
            int codepto = cursor.getInt(0);
            String descripcion = cursor.getString(1);

            ArrayList<String> cuentas = new ArrayList<>();
            Cursor cCuentas = db.rawQuery(
                    "SELECT Cuentas.cuenta FROM Cuentas INNER JOIN Clientes ON Cuentas.carnet=Clientes.carnet WHERE Clientes.codepto=?",
                    new String[]{String.valueOf(codepto)});
            while (cCuentas.moveToNext()) cuentas.add(cCuentas.getString(0));
            cCuentas.close();

            int nroCuentas = cuentas.size();
            int saldoTotal = 0;
            if (!cuentas.isEmpty()) {
                String cuentasIN = "'" + join("','", cuentas) + "'";
                Cursor cSaldos = db.rawQuery(
                        "SELECT SUM(monto) FROM Movimientos WHERE cuenta IN (" + cuentasIN + ")", null);
                if (cSaldos.moveToFirst()) saldoTotal = cSaldos.getInt(0);
                cSaldos.close();
            }
            TableRow fila = new TableRow(this);
            fila.addView(crearCelda(String.valueOf(codepto)));
            fila.addView(crearCelda(descripcion));
            fila.addView(crearCelda(String.valueOf(nroCuentas)));
            fila.addView(crearCelda(String.valueOf(saldoTotal)));
            tableDepartamentos.addView(fila);

            entries.add(new PieEntry(saldoTotal, descripcion));
        }
        cursor.close();

        mostrarGraficoPie(entries);
    }

    private void mostrarTablaYGrafico(ArrayList<Integer> ids) {
        tableDepartamentos.setVisibility(TableLayout.VISIBLE);
        pieChartDepartamento.setVisibility(PieChart.VISIBLE);

        tableDepartamentos.removeAllViews();
        TableRow cabecera = new TableRow(this);
        cabecera.addView(crearCelda("ID"));
        cabecera.addView(crearCelda("Descripción"));
        cabecera.addView(crearCelda("Nro Cuentas"));
        cabecera.addView(crearCelda("Saldo Total"));
        tableDepartamentos.addView(cabecera);

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (int codepto : ids) {
            Cursor cur = db.rawQuery("SELECT descripcion FROM Departamentos WHERE codepto=?", new String[]{String.valueOf(codepto)});
            String descripcion = cur.moveToFirst() ? cur.getString(0) : "";
            cur.close();

            ArrayList<String> cuentas = new ArrayList<>();
            Cursor cCuentas = db.rawQuery(
                    "SELECT Cuentas.cuenta FROM Cuentas INNER JOIN Clientes ON Cuentas.carnet=Clientes.carnet WHERE Clientes.codepto=?",
                    new String[]{String.valueOf(codepto)});
            while (cCuentas.moveToNext()) cuentas.add(cCuentas.getString(0));
            cCuentas.close();

            int nroCuentas = cuentas.size();
            int saldoTotal = 0;
            if (!cuentas.isEmpty()) {
                String cuentasIN = "'" + join("','", cuentas) + "'";
                Cursor cSaldos = db.rawQuery(
                        "SELECT SUM(monto) FROM Movimientos WHERE cuenta IN (" + cuentasIN + ")", null);
                if (cSaldos.moveToFirst()) saldoTotal = cSaldos.getInt(0);
                cSaldos.close();
            }
            TableRow fila = new TableRow(this);
            fila.addView(crearCelda(String.valueOf(codepto)));
            fila.addView(crearCelda(descripcion));
            fila.addView(crearCelda(String.valueOf(nroCuentas)));
            fila.addView(crearCelda(String.valueOf(saldoTotal)));
            tableDepartamentos.addView(fila);

            entries.add(new PieEntry(saldoTotal, descripcion));
        }

        mostrarGraficoPie(entries);
    }

    private void mostrarGraficoPie(ArrayList<PieEntry> entries) {
        PieDataSet dataSet = new PieDataSet(entries, "Saldo por departamento");
        dataSet.setColors(
                Color.rgb(244,67,54),
                Color.rgb(33,150,243),
                Color.rgb(76,175,80),
                Color.rgb(255,193,7));
        dataSet.setValueTextColor(Color.BLACK);
        PieData data = new PieData(dataSet);
        pieChartDepartamento.setData(data);
        pieChartDepartamento.getDescription().setEnabled(false);
        pieChartDepartamento.setCenterText("Saldo total por departamento");
        pieChartDepartamento.invalidate();
    }

    private TextView crearCelda(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(18, 10, 18, 10);
        tv.setTextSize(16);
        return tv;
    }

    private String join(String sep, ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<list.size();i++) {
            sb.append(list.get(i));
            if(i<list.size()-1) sb.append(sep);
        }
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
