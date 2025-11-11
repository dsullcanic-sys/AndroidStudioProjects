package com.ryuk.siscoosql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class ProfesionesActivity extends AppCompatActivity {
    EditText edtBuscarProfesion;
    Button btnBuscarProfesion, btnLimpiarProfesion;
    TextView tvResultadoProfesion;
    TableLayout tableProfesiones;
    BarChart barChartProfesiones;

    private DBHelper dbHelper;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profesiones);

        edtBuscarProfesion = findViewById(R.id.edtBuscarProfesion);
        btnBuscarProfesion = findViewById(R.id.btnBuscarProfesion);
        btnLimpiarProfesion = findViewById(R.id.btnLimpiarProfesion);
        tvResultadoProfesion = findViewById(R.id.tvResultadoProfesion);
        tableProfesiones = findViewById(R.id.tableProfesiones);
        barChartProfesiones = findViewById(R.id.barChartProfesiones);

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        limpiarVista(); // Verdad inicial: limpio

        btnBuscarProfesion.setOnClickListener(v -> buscarProfesion());
        btnLimpiarProfesion.setOnClickListener(v -> limpiarVista());
    }

    private void buscarProfesion() {
        String query = edtBuscarProfesion.getText().toString().trim();
        tvResultadoProfesion.setVisibility(TextView.GONE);

        if (query.isEmpty()) {
            mostrarTablaYBarrasGeneral();
            return;
        }

        tableProfesiones.removeAllViews();
        barChartProfesiones.setVisibility(BarChart.GONE);

        Cursor cursor = db.rawQuery(
                "SELECT codprof, descripcion FROM Profesiones WHERE codprof=? OR lower(descripcion) LIKE ?",
                new String[]{query, "%" + query.toLowerCase(Locale.US) + "%"});
        ArrayList<Integer> idsCoincidentes = new ArrayList<>();
        while (cursor.moveToNext()) {
            idsCoincidentes.add(cursor.getInt(0));
        }
        cursor.close();

        if (idsCoincidentes.isEmpty()) {
            tvResultadoProfesion.setText("No se encontró profesión.");
            tvResultadoProfesion.setVisibility(TextView.VISIBLE);
            tableProfesiones.setVisibility(TableLayout.GONE);
            barChartProfesiones.setVisibility(BarChart.GONE);
            return;
        }
        mostrarTablaYBarras(idsCoincidentes);
    }

    private void limpiarVista() {
        edtBuscarProfesion.setText("");
        tvResultadoProfesion.setVisibility(TextView.GONE);
        tableProfesiones.setVisibility(TableLayout.GONE);
        barChartProfesiones.setVisibility(BarChart.GONE);
    }

    private void mostrarTablaYBarrasGeneral() {
        tableProfesiones.setVisibility(TableLayout.VISIBLE);
        barChartProfesiones.setVisibility(BarChart.VISIBLE);

        tableProfesiones.removeAllViews();
        TableRow cabecera = new TableRow(this);
        cabecera.addView(crearCelda("ID"));
        cabecera.addView(crearCelda("Descripción"));
        cabecera.addView(crearCelda("Nro Cuentas"));
        cabecera.addView(crearCelda("Saldo Total"));
        tableProfesiones.addView(cabecera);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT codprof, descripcion FROM Profesiones ORDER BY descripcion", null);
        int index = 0;
        while (cursor.moveToNext()) {
            int codprof = cursor.getInt(0);
            String descripcion = cursor.getString(1);

            ArrayList<String> cuentas = new ArrayList<>();
            Cursor cClientes = db.rawQuery("SELECT carnet FROM Clientes WHERE codprof=?", new String[]{String.valueOf(codprof)});
            while (cClientes.moveToNext()) {
                String carnet = cClientes.getString(0);
                Cursor cCuentas = db.rawQuery("SELECT cuenta FROM Cuentas WHERE carnet=?", new String[]{carnet});
                while (cCuentas.moveToNext())
                    cuentas.add(cCuentas.getString(0));
                cCuentas.close();
            }
            cClientes.close();

            int nroCuentas = cuentas.size(), saldoTotal = 0;
            if (!cuentas.isEmpty()) {
                String cuentasIN = "'" + join("','", cuentas) + "'";
                Cursor cSaldos = db.rawQuery("SELECT SUM(monto) FROM Movimientos WHERE cuenta IN (" + cuentasIN + ")", null);
                if (cSaldos.moveToFirst()) saldoTotal = cSaldos.getInt(0);
                cSaldos.close();
            }
            TableRow fila = new TableRow(this);
            fila.addView(crearCelda(String.valueOf(codprof)));
            fila.addView(crearCelda(descripcion));
            fila.addView(crearCelda(String.valueOf(nroCuentas)));
            fila.addView(crearCelda(String.valueOf(saldoTotal)));
            tableProfesiones.addView(fila);

            entries.add(new BarEntry(index, saldoTotal));
            labels.add(descripcion);
            index++;
        }
        cursor.close();

        mostrarGraficoBarra(labels, entries);
    }

    private void mostrarTablaYBarras(ArrayList<Integer> ids) {
        tableProfesiones.setVisibility(TableLayout.VISIBLE);
        barChartProfesiones.setVisibility(BarChart.VISIBLE);

        tableProfesiones.removeAllViews();
        TableRow cabecera = new TableRow(this);
        cabecera.addView(crearCelda("ID"));
        cabecera.addView(crearCelda("Descripción"));
        cabecera.addView(crearCelda("Nro Cuentas"));
        cabecera.addView(crearCelda("Saldo Total"));
        tableProfesiones.addView(cabecera);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;
        for (int codprof : ids) {
            Cursor cProf = db.rawQuery("SELECT descripcion FROM Profesiones WHERE codprof=?", new String[]{String.valueOf(codprof)});
            String descripcion = cProf.moveToFirst() ? cProf.getString(0) : "";
            cProf.close();

            ArrayList<String> cuentas = new ArrayList<>();
            Cursor cClientes = db.rawQuery("SELECT carnet FROM Clientes WHERE codprof=?", new String[]{String.valueOf(codprof)});
            while (cClientes.moveToNext()) {
                String carnet = cClientes.getString(0);
                Cursor cCuentas = db.rawQuery("SELECT cuenta FROM Cuentas WHERE carnet=?", new String[]{carnet});
                while (cCuentas.moveToNext())
                    cuentas.add(cCuentas.getString(0));
                cCuentas.close();
            }
            cClientes.close();

            int nroCuentas = cuentas.size(), saldoTotal = 0;
            if (!cuentas.isEmpty()) {
                String cuentasIN = "'" + join("','", cuentas) + "'";
                Cursor cSaldos = db.rawQuery("SELECT SUM(monto) FROM Movimientos WHERE cuenta IN (" + cuentasIN + ")", null);
                if (cSaldos.moveToFirst()) saldoTotal = cSaldos.getInt(0);
                cSaldos.close();
            }
            TableRow fila = new TableRow(this);
            fila.addView(crearCelda(String.valueOf(codprof)));
            fila.addView(crearCelda(descripcion));
            fila.addView(crearCelda(String.valueOf(nroCuentas)));
            fila.addView(crearCelda(String.valueOf(saldoTotal)));
            tableProfesiones.addView(fila);

            entries.add(new BarEntry(index, saldoTotal));
            labels.add(descripcion);
            index++;
        }
        mostrarGraficoBarra(labels, entries);
    }

    private void mostrarGraficoBarra(ArrayList<String> labels, ArrayList<BarEntry> entries) {
        BarDataSet dataSet = new BarDataSet(entries, "Saldo por profesión");
        dataSet.setColors(Color.rgb(33,150,243), Color.rgb(255,193,7), Color.rgb(244,67,54), Color.rgb(76,175,80));
        dataSet.setValueTextColor(Color.BLACK);

        BarData data = new BarData(dataSet);
        barChartProfesiones.setData(data);
        barChartProfesiones.getDescription().setEnabled(false);
        barChartProfesiones.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartProfesiones.getXAxis().setGranularity(1f);
        barChartProfesiones.getXAxis().setGranularityEnabled(true);
        barChartProfesiones.getXAxis().setLabelRotationAngle(-45f);
        barChartProfesiones.invalidate();
    }

    private TextView crearCelda(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(20, 10, 20, 10);
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
