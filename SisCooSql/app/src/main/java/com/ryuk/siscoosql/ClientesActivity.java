package com.ryuk.siscoosql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

public class ClientesActivity extends AppCompatActivity {
    private EditText edtCiBuscar;
    private Button btnBuscarCliente, btnLimpiarCliente, btnAnteriorCliente, btnSiguienteCliente;
    private TextView tvResultadoCliente, tvPaginacionCliente;
    private TableLayout tableClientes;
    private BarChart barChartClientes;
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private static final int FILAS_POR_PAGINA = 50;
    private int paginaActual = 0;
    private int totalRegistros = 0;
    private int totalPaginas = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        edtCiBuscar = findViewById(R.id.edtCiBuscar);
        btnBuscarCliente = findViewById(R.id.btnBuscarCliente);
        btnLimpiarCliente = findViewById(R.id.btnLimpiarCliente);
        btnAnteriorCliente = findViewById(R.id.btnAnteriorCliente);
        btnSiguienteCliente = findViewById(R.id.btnSiguienteCliente);
        tvResultadoCliente = findViewById(R.id.tvResultadoCliente);
        tvPaginacionCliente = findViewById(R.id.tvPaginacionCliente);
        tableClientes = findViewById(R.id.tableClientes);
        barChartClientes = findViewById(R.id.barChartClientes);

        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        limpiarVista();

        btnBuscarCliente.setOnClickListener(v -> buscarCliente());
        btnLimpiarCliente.setOnClickListener(v -> limpiarVista());

        btnAnteriorCliente.setOnClickListener(v -> {
            if (paginaActual > 0) {
                paginaActual--;
                mostrarPaginaClientes();
            }
        });
        btnSiguienteCliente.setOnClickListener(v -> {
            if (paginaActual < totalPaginas - 1) {
                paginaActual++;
                mostrarPaginaClientes();
            }
        });
    }

    private void buscarCliente() {
        String query = edtCiBuscar.getText().toString().trim().toLowerCase();
        tvResultadoCliente.setVisibility(View.GONE);

        if (query.isEmpty()) {
            paginaActual = 0;
            mostrarPaginaClientes();
            return;
        }

        tableClientes.removeAllViews();
        barChartClientes.setVisibility(View.GONE);
        tvPaginacionCliente.setVisibility(View.GONE);
        btnAnteriorCliente.setVisibility(View.GONE);
        btnSiguienteCliente.setVisibility(View.GONE);

        Cursor c = db.rawQuery(
                "SELECT carnet, nombres, codprof, codepto FROM Clientes WHERE lower(nombres) LIKE ? OR carnet=? LIMIT 1",
                new String[]{"%" + query + "%", query});

        if (c.moveToFirst()) {
            tableClientes.setVisibility(View.VISIBLE);
            barChartClientes.setVisibility(View.VISIBLE);
            mostrarClienteTablaUnica(c.getString(0), c.getString(1), c.getInt(2), c.getInt(3));
            mostrarGraficoCliente(c.getString(0), c.getString(1));
        } else {
            tvResultadoCliente.setVisibility(View.VISIBLE);
            tvResultadoCliente.setText("No se encontró ese cliente.");
            tableClientes.setVisibility(View.GONE);
            barChartClientes.setVisibility(View.GONE);
        }
        c.close();
    }

    private void limpiarVista() {
        edtCiBuscar.setText("");
        tvResultadoCliente.setVisibility(View.GONE);
        tableClientes.setVisibility(View.GONE);
        barChartClientes.setVisibility(View.GONE);
        tvPaginacionCliente.setVisibility(View.GONE);
        btnAnteriorCliente.setVisibility(View.GONE);
        btnSiguienteCliente.setVisibility(View.GONE);
    }

    private void mostrarPaginaClientes() {
        tableClientes.setVisibility(View.VISIBLE);
        barChartClientes.setVisibility(View.VISIBLE);
        btnAnteriorCliente.setVisibility(View.VISIBLE);
        btnSiguienteCliente.setVisibility(View.VISIBLE);
        tvPaginacionCliente.setVisibility(View.VISIBLE);

        tableClientes.removeAllViews();
        TableRow cabecera = new TableRow(this);
        cabecera.addView(crearCelda("Carnet"));
        cabecera.addView(crearCelda("Nombre"));
        cabecera.addView(crearCelda("Profesión"));
        cabecera.addView(crearCelda("Departamento"));
        cabecera.addView(crearCelda("Nro. Cuentas"));
        cabecera.addView(crearCelda("Saldo total"));
        tableClientes.addView(cabecera);

        int offset = paginaActual * FILAS_POR_PAGINA;
        Cursor clientes = db.rawQuery(
                "SELECT carnet, nombres, codprof, codepto FROM Clientes ORDER BY nombres LIMIT ? OFFSET ?",
                new String[]{String.valueOf(FILAS_POR_PAGINA), String.valueOf(offset)});

        while (clientes.moveToNext()) {
            String carnet = clientes.getString(0);
            String nombres = clientes.getString(1);
            String profesion = getDescripcion("Profesiones", "codprof", clientes.getInt(2));
            String departamento = getDescripcion("Departamentos", "codepto", clientes.getInt(3));
            ArrayList<String> cuentasList = new ArrayList<>();
            Cursor cuentas = db.rawQuery(
                    "SELECT cuenta FROM Cuentas WHERE carnet=?", new String[]{carnet});
            while(cuentas.moveToNext()) cuentasList.add(cuentas.getString(0));
            cuentas.close();

            int nroCuentas = cuentasList.size();
            int saldoTotal = 0;
            if (!cuentasList.isEmpty()) {
                String cuentasIN = "'" + join("','", cuentasList) + "'";
                Cursor movs = db.rawQuery(
                        "SELECT SUM(monto) FROM Movimientos WHERE cuenta IN (" + cuentasIN + ")", null);
                if (movs.moveToFirst()) saldoTotal = movs.getInt(0);
                movs.close();
            }
            TableRow fila = new TableRow(this);
            fila.addView(crearCelda(carnet));
            fila.addView(crearCelda(nombres));
            fila.addView(crearCelda(profesion));
            fila.addView(crearCelda(departamento));
            fila.addView(crearCelda(String.valueOf(nroCuentas)));
            fila.addView(crearCelda(String.valueOf(saldoTotal)));
            tableClientes.addView(fila);
        }
        clientes.close();

        Cursor count = db.rawQuery("SELECT COUNT(*) FROM Clientes", null);
        if(count.moveToFirst()) totalRegistros = count.getInt(0);
        count.close();

        totalPaginas = Math.max(1, ((totalRegistros-1) / FILAS_POR_PAGINA) + 1);
        tvPaginacionCliente.setText("Página " + (paginaActual+1) + " de " + totalPaginas + " | Total: " + totalRegistros + " registros");
        btnAnteriorCliente.setEnabled(paginaActual > 0);
        btnSiguienteCliente.setEnabled(paginaActual < totalPaginas-1);

        mostrarGraficoBarrasRangos(); // SIEMPRE muestra gráfico con rango de saldos en modo general
    }

    private void mostrarClienteTablaUnica(String carnet, String nombre, int codprof, int codepto) {
        tableClientes.removeAllViews();
        String profesion = getDescripcion("Profesiones", "codprof", codprof);
        String departamento = getDescripcion("Departamentos", "codepto", codepto);

        Cursor cuentas = db.rawQuery(
                "SELECT cuenta FROM Cuentas WHERE carnet=?", new String[]{carnet});
        ArrayList<String> cuentasList = new ArrayList<>();
        while(cuentas.moveToNext()) cuentasList.add(cuentas.getString(0));
        cuentas.close();

        int nroCuentas = cuentasList.size();
        int saldoTotal = 0;
        if (!cuentasList.isEmpty()) {
            String cuentasIN = "'" + join("','", cuentasList) + "'";
            Cursor movs = db.rawQuery(
                    "SELECT SUM(monto) FROM Movimientos WHERE cuenta IN (" + cuentasIN + ")", null);
            if (movs.moveToFirst()) saldoTotal = movs.getInt(0);
            movs.close();
        }

        tableClientes.addView(filaDatos("Nombre:", nombre));
        tableClientes.addView(filaDatos("Profesión:", profesion));
        tableClientes.addView(filaDatos("Departamento:", departamento));
        tableClientes.addView(filaDatos("Nro de Cuentas:", String.valueOf(nroCuentas)));
        tableClientes.addView(filaDatos("Saldo total:", String.valueOf(saldoTotal)));
    }

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

    private String join(String sep, ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<list.size();i++) {
            sb.append(list.get(i));
            if(i<list.size()-1) sb.append(sep);
        }
        return sb.toString();
    }

    private String getDescripcion(String tabla, String clave, int valor) {
        Cursor c = db.rawQuery(
                "SELECT descripcion FROM " + tabla + " WHERE " + clave + "=? LIMIT 1",
                new String[]{String.valueOf(valor)});
        String desc = "";
        if (c.moveToFirst()) desc = c.getString(0);
        c.close();
        return desc;
    }

    private void mostrarGraficoCliente(String carnet, String nombre) {
        ArrayList<String> cuentasList = new ArrayList<>();
        Cursor cuentas = db.rawQuery(
                "SELECT cuenta FROM Cuentas WHERE carnet=?", new String[]{carnet});
        while(cuentas.moveToNext()) cuentasList.add(cuentas.getString(0));
        cuentas.close();

        int saldoTotal = 0;
        if (!cuentasList.isEmpty()) {
            String cuentasIN = "'" + join("','", cuentasList) + "'";
            Cursor movs = db.rawQuery(
                    "SELECT SUM(monto) FROM Movimientos WHERE cuenta IN (" + cuentasIN + ")", null);
            if (movs.moveToFirst()) saldoTotal = movs.getInt(0);
            movs.close();
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, saldoTotal));
        ArrayList<String> labels = new ArrayList<>();
        labels.add(nombre);

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

    private void mostrarGraficoBarrasRangos() {
        int rango = 3000;
        int saldoMax = 0;
        ArrayList<Integer> saldosClientes = new ArrayList<>();

        Cursor clientes = db.rawQuery(
                "SELECT carnet FROM Clientes", null);
        while (clientes.moveToNext()) {
            String carnet = clientes.getString(0);
            ArrayList<String> cuentasList = new ArrayList<>();
            Cursor cuentas = db.rawQuery(
                    "SELECT cuenta FROM Cuentas WHERE carnet=?", new String[]{carnet});
            while(cuentas.moveToNext()) cuentasList.add(cuentas.getString(0));
            cuentas.close();

            int saldoTotal = 0;
            if (!cuentasList.isEmpty()) {
                String cuentasIN = "'" + join("','", cuentasList) + "'";
                Cursor movs = db.rawQuery(
                        "SELECT SUM(monto) FROM Movimientos WHERE cuenta IN (" + cuentasIN + ")", null);
                if (movs.moveToFirst()) saldoTotal = movs.getInt(0);
                movs.close();
            }
            saldosClientes.add(saldoTotal);
            if (saldoTotal > saldoMax) saldoMax = saldoTotal;
        }
        clientes.close();

        LinkedHashMap<String, Integer> rangos = new LinkedHashMap<>();
        int hasta = rango;
        while (hasta <= saldoMax + rango) {
            rangos.put(formatK(hasta), 0);
            hasta += rango;
        }

        for (int saldo : saldosClientes) {
            int limite = rango;
            for (String key : rangos.keySet()) {
                if (saldo < limite) {
                    rangos.put(key, rangos.get(key) + 1);
                    break;
                }
                limite += rango;
            }
        }

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

    private String formatK(int n) {
        if (n >= 1000) {
            double val = n / 1000.0;
            if (val == (int) val) {
                return ((int) val) + "k";
            } else {
                return String.format(Locale.US,"%.1fk", val);
            }
        }
        return String.valueOf(n);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
