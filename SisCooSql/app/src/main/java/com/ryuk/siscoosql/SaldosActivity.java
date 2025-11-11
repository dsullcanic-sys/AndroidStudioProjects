package com.ryuk.siscoosql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class SaldosActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private Button btnBuscar, btnLimpiar, btnAnterior, btnSiguiente;
    private EditText etBuscar;
    private LinearLayout layoutDatosCliente, layoutCuentas;
    private TextView tvInfoPaginacion;
    private BarChart chartMovimientos;

    private static final String[] MESES_CORTOS = {
            "Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"
    };
    private static final int FILAS_POR_PAGINA = 50;
    private int paginaActualGeneral = 0;
    private int totalRegistros = 0;
    private int paginasTotales = 1;

    private final SimpleDateFormat formatoISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat formatoSalida = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saldos);

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        etBuscar = findViewById(R.id.etBuscar);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnLimpiar = findViewById(R.id.btnLimpiar);
        layoutDatosCliente = findViewById(R.id.layoutDatosCliente);
        layoutCuentas = findViewById(R.id.layoutCuentas);
        chartMovimientos = findViewById(R.id.chartMovimientos);

        btnAnterior = findViewById(R.id.btnAnterior);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        tvInfoPaginacion = findViewById(R.id.tvInfoPaginacion);

        btnBuscar.setOnClickListener(v -> accionBuscar());
        btnLimpiar.setOnClickListener(v -> limpiarVista());

        btnAnterior.setOnClickListener(v -> {
            if(paginaActualGeneral > 0) {
                paginaActualGeneral--;
                mostrarResumenGeneral();
            }
        });
        btnSiguiente.setOnClickListener(v -> {
            if(paginaActualGeneral < paginasTotales-1) {
                paginaActualGeneral++;
                mostrarResumenGeneral();
            }
        });

        limpiarVista(); // Abre limpio
    }

    private void accionBuscar() {
        String valor = etBuscar.getText().toString().trim();
        layoutDatosCliente.removeAllViews();
        layoutCuentas.removeAllViews();
        if (!valor.isEmpty()) {
            btnAnterior.setVisibility(Button.GONE);
            btnSiguiente.setVisibility(Button.GONE);
            tvInfoPaginacion.setVisibility(TextView.GONE);
            chartMovimientos.setVisibility(BarChart.VISIBLE);
            mostrarCliente(valor);
        } else {
            paginaActualGeneral = 0;
            mostrarResumenGeneral();
        }
    }

    private void limpiarVista() {
        etBuscar.setText("");
        layoutDatosCliente.removeAllViews();
        layoutCuentas.removeAllViews();
        btnAnterior.setVisibility(Button.GONE);
        btnSiguiente.setVisibility(Button.GONE);
        tvInfoPaginacion.setVisibility(TextView.GONE);
        chartMovimientos.setVisibility(BarChart.GONE);
    }

    // --- Resumen General: paginación ---
    private void mostrarResumenGeneral() {
        btnAnterior.setVisibility(Button.VISIBLE);
        btnSiguiente.setVisibility(Button.VISIBLE);
        tvInfoPaginacion.setVisibility(TextView.VISIBLE);
        chartMovimientos.setVisibility(BarChart.VISIBLE);
        mostrarTablaMovimientosPaginada();
        mostrarInfoPaginacion();
        mostrarGraficoGeneral();
    }

    private void mostrarTablaMovimientosPaginada() {
        layoutDatosCliente.removeAllViews();
        TableLayout tabla = new TableLayout(this);
        TableRow header = new TableRow(this);
        String[] columns = {"Cuenta", "Monto", "Fecha"};
        for (String h : columns) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setPadding(8,8,8,8);
            header.addView(tv);
        }
        tabla.addView(header);

        int offset = paginaActualGeneral * FILAS_POR_PAGINA;
        Cursor mov = db.rawQuery(
                "SELECT cuenta, monto, fecha FROM Movimientos ORDER BY fecha DESC LIMIT ? OFFSET ?",
                new String[]{String.valueOf(FILAS_POR_PAGINA), String.valueOf(offset)});
        while (mov.moveToNext()) {
            TableRow row = new TableRow(this);
            TextView tvCuenta = new TextView(this);
            tvCuenta.setText(mov.getString(0));
            tvCuenta.setPadding(8,8,8,8);
            row.addView(tvCuenta);

            TextView tvMonto = new TextView(this);
            tvMonto.setText(mov.getString(1));
            tvMonto.setPadding(8,8,8,8);
            row.addView(tvMonto);

            TextView tvFecha = new TextView(this);
            tvFecha.setText(formatearFecha(mov.getString(2)));
            tvFecha.setPadding(8,8,8,8);
            row.addView(tvFecha);

            tabla.addView(row);
        }
        mov.close();
        layoutDatosCliente.addView(tabla);
    }

    private String formatearFecha(String fechaISO) {
        try {
            return formatoSalida.format(formatoISO.parse(fechaISO));
        } catch (ParseException e) {
            return fechaISO;
        }
    }

    private void mostrarInfoPaginacion() {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM Movimientos", null);
        if(c.moveToFirst()) {
            totalRegistros = c.getInt(0);
            paginasTotales = ((totalRegistros-1) / FILAS_POR_PAGINA) + 1;
        }
        c.close();

        int paginaActualVista = paginaActualGeneral + 1;
        tvInfoPaginacion.setText("Página " + paginaActualVista + " de " + paginasTotales +
                " | Total: " + totalRegistros + " registros");

        btnAnterior.setEnabled(paginaActualGeneral > 0);
        btnSiguiente.setEnabled(paginaActualGeneral < paginasTotales-1);
    }

    private void mostrarGraficoGeneral() {
        chartMovimientos.setVisibility(BarChart.VISIBLE);
        Cursor cursor = db.rawQuery(
                "SELECT strftime('%Y-%m', fecha) AS mes, SUM(monto) AS total " +
                        "FROM Movimientos GROUP BY mes ORDER BY mes", null);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        int idxTotal = cursor.getColumnIndex("total");
        int idxMes = cursor.getColumnIndex("mes");
        while(cursor.moveToNext()) {
            if (idxTotal != -1 && idxMes != -1) {
                entries.add(new BarEntry(i, cursor.getInt(idxTotal)));
                String mes = cursor.getString(idxMes);
                String numMes = mes.substring(5,7);
                int mesIndex = Integer.parseInt(numMes) - 1;
                String etiqueta = (mesIndex >= 0 && mesIndex < 12) ? MESES_CORTOS[mesIndex] : mes;
                labels.add(etiqueta);
                i++;
            }
        }
        cursor.close();

        BarDataSet dataSet = new BarDataSet(entries, "Movimientos generales por mes");
        dataSet.setColors(
                Color.rgb(33,150,243),
                Color.rgb(255,193,7),
                Color.rgb(244,67,54),
                Color.rgb(76,175,80)
        );
        dataSet.setValueTextColor(Color.BLACK);
        BarData barData = new BarData(dataSet);
        chartMovimientos.setData(barData);

        XAxis xAxis = chartMovimientos.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        chartMovimientos.invalidate();
    }

    // --- Búsqueda por cliente específico ----
    private void mostrarCliente(String valor) {
        layoutDatosCliente.removeAllViews();
        layoutCuentas.removeAllViews();
        Cursor c = db.rawQuery(
                "SELECT cli.carnet, cli.nombres, p.descripcion, d.descripcion " +
                        "FROM Clientes cli " +
                        "JOIN Profesiones p ON cli.codprof=p.codprof " +
                        "JOIN Departamentos d ON cli.codepto=d.codepto " +
                        "WHERE cli.nombres LIKE ? OR cli.carnet=? OR cli.carnet=(SELECT carnet FROM Cuentas WHERE cuenta=?) LIMIT 1",
                new String[]{"%" + valor + "%", valor, valor});
        if (c.moveToFirst()) {
            String carnet = c.getString(0);
            String nombre = c.getString(1);
            String profesion = c.getString(2);
            String departamento = c.getString(3);

            TextView datos = new TextView(this);
            datos.setText("Carnet: " + carnet + "\nNombre: " + nombre + "\nProf.: " + profesion + "\nDepto: " + departamento);
            layoutDatosCliente.addView(datos);

            mostrarCuentasYMovimientos(carnet);
            mostrarGraficoMovimientosCliente(carnet);
        } else {
            TextView tv = new TextView(this);
            tv.setText("Cliente no encontrado...");
            layoutDatosCliente.addView(tv);
            chartMovimientos.setVisibility(BarChart.GONE);
        }
        c.close();
    }

    private void mostrarCuentasYMovimientos(String carnet) {
        Cursor cuentas = db.rawQuery(
                "SELECT cuenta, fapertura FROM Cuentas WHERE carnet=? ORDER BY fapertura DESC",
                new String[]{carnet});
        while (cuentas.moveToNext()) {
            String cuenta = cuentas.getString(0);
            String fapertura = cuentas.getString(1);

            TextView tcuenta = new TextView(this);
            tcuenta.setText("\nCuenta: " + cuenta + " \nF.Apertura: " + formatearFecha(fapertura));
            layoutCuentas.addView(tcuenta);
            mostrarMovimientosCuenta(cuenta);
        }
        cuentas.close();
    }

    private void mostrarMovimientosCuenta(String cuenta) {
        TableLayout tabla = new TableLayout(this);
        TableRow header = new TableRow(this);
        String[] head = {"Fecha", "Monto"};
        for (String h : head) {
            TextView t = new TextView(this);
            t.setText(h);
            t.setPadding(8,8,8,8);
            header.addView(t);
        }
        tabla.addView(header);

        Cursor mov = db.rawQuery("SELECT fecha, monto FROM Movimientos WHERE cuenta=? ORDER BY fecha DESC", new String[]{cuenta});
        int total = 0;
        while (mov.moveToNext()) {
            TableRow row = new TableRow(this);
            TextView tf = new TextView(this);
            tf.setText(formatearFecha(mov.getString(0)));
            tf.setPadding(8,8,8,8);
            row.addView(tf);
            TextView tm = new TextView(this);
            tm.setText(mov.getString(1));
            tm.setPadding(8,8,8,8);
            row.addView(tm);
            tabla.addView(row);
            total += mov.getInt(1);
        }
        mov.close();

        layoutCuentas.addView(tabla);

        TextView totalView = new TextView(this);
        totalView.setText("Total de mov.: " + total);
        layoutCuentas.addView(totalView);
    }

    private void mostrarGraficoMovimientosCliente(String carnet) {
        chartMovimientos.setVisibility(BarChart.VISIBLE);
        Cursor cursor = db.rawQuery(
                "SELECT strftime('%Y-%m', m.fecha) AS mes, SUM(m.monto) AS total " +
                        "FROM Movimientos m JOIN Cuentas c ON m.cuenta=c.cuenta " +
                        "WHERE c.carnet=? GROUP BY mes ORDER BY mes",
                new String[]{carnet});

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        int idxTotal = cursor.getColumnIndex("total");
        int idxMes = cursor.getColumnIndex("mes");
        while (cursor.moveToNext()) {
            if (idxTotal != -1 && idxMes != -1) {
                entries.add(new BarEntry(i, cursor.getInt(idxTotal)));
                String mes = cursor.getString(idxMes);
                String numMes = mes.substring(5,7);
                int mesIndex = Integer.parseInt(numMes) - 1;
                String etiqueta = (mesIndex >= 0 && mesIndex < 12) ? MESES_CORTOS[mesIndex] : mes;
                labels.add(etiqueta);
                i++;
            }
        }
        cursor.close();

        BarDataSet dataSet = new BarDataSet(entries, "Movimientos por mes");
        dataSet.setColors(
                Color.rgb(33,150,243),
                Color.rgb(255,193,7),
                Color.rgb(244,67,54),
                Color.rgb(76,175,80)
        );
        dataSet.setValueTextColor(Color.BLACK);
        BarData barData = new BarData(dataSet);
        chartMovimientos.setData(barData);

        XAxis xAxis = chartMovimientos.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        chartMovimientos.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
