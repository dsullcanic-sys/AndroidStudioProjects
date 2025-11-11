package com.ryuk.graficas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import android.graphics.Color;
import java.util.ArrayList;

public class Saldos extends AppCompatActivity {
    EditText edtCiBuscarSaldo;
    Button btnBuscarSaldo, btnLimpiarSaldo;
    TextView tvResultadoSaldo;
    TableLayout tableSaldos;
    LinearLayout layoutSaldosDatos;
    ScrollView scrollResumen, scrollDetalle;
    BarChart barChartSaldos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saldos);

        edtCiBuscarSaldo = findViewById(R.id.edtCiBuscarSaldo);
        btnBuscarSaldo = findViewById(R.id.btnBuscarSaldo);
        btnLimpiarSaldo = findViewById(R.id.btnLimpiarSaldo);
        tvResultadoSaldo = findViewById(R.id.tvResultadoSaldo);
        tableSaldos = findViewById(R.id.tableSaldos);
        layoutSaldosDatos = findViewById(R.id.layoutSaldosDatos);
        scrollResumen = findViewById(R.id.scrollResumen);
        scrollDetalle = findViewById(R.id.scrollDetalle);
        barChartSaldos = findViewById(R.id.barChartSaldos);

        tvResultadoSaldo.setVisibility(View.GONE);
        scrollResumen.setVisibility(View.GONE);
        scrollDetalle.setVisibility(View.GONE);
        barChartSaldos.setVisibility(View.GONE);

        btnBuscarSaldo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buscarSaldo();
            }
        });
        btnLimpiarSaldo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                limpiarBusqueda();
            }
        });
    }

    private void buscarSaldo() {
        String query = edtCiBuscarSaldo.getText().toString().trim().toLowerCase();
        layoutSaldosDatos.removeAllViews();
        tvResultadoSaldo.setVisibility(View.GONE);
        scrollResumen.setVisibility(View.GONE);
        scrollDetalle.setVisibility(View.GONE);
        barChartSaldos.setVisibility(View.GONE);

        if (query.isEmpty()) {
            scrollResumen.setVisibility(View.VISIBLE);
            mostrarTodosSaldos();
            mostrarBarraMontoPorMesResumenManual();
            return;
        }

        // Busca por carnet y por nombre además
        MainActivity.Cuenta cuentaEncontrada = null;
        MainActivity.Cliente titularEncontrado = null;

        for (MainActivity.Cuenta c : MainActivity.listaCuentas) {
            if ((c.cuenta != null && c.cuenta.toLowerCase().equals(query)) ||
                    (c.carnet != null && c.carnet.toLowerCase().equals(query))) {
                cuentaEncontrada = c; break;
            }
        }
        for (MainActivity.Cliente cl : MainActivity.listaClientes) {
            if (cl.nombres != null && cl.nombres.toLowerCase().contains(query)) {
                titularEncontrado = cl; break;
            }
        }

        if (cuentaEncontrada != null) {
            // Resumen tradicional por cuenta/carnet (idéntico al tuyo)
            scrollDetalle.setVisibility(View.VISIBLE);

            MainActivity.Cliente titular = null;
            for (MainActivity.Cliente cl : MainActivity.listaClientes) {
                if (cl.carnet != null && cl.carnet.equals(cuentaEncontrada.carnet)) {
                    titular = cl; break;
                }
            }
            ArrayList<MainActivity.Cuenta> cuentasTitular = new ArrayList<>();
            for (MainActivity.Cuenta c : MainActivity.listaCuentas) {
                if (c.carnet != null && c.carnet.equals(cuentaEncontrada.carnet)) {
                    cuentasTitular.add(c);
                }
            }
            TextView nombreTV = new TextView(this);
            nombreTV.setText("Nombre: " + (titular != null ? titular.nombres : "(Sin nombre)"));
            nombreTV.setTextSize(18);
            nombreTV.setPadding(10, 20, 10, 20);
            layoutSaldosDatos.addView(nombreTV);

            double saldoTotalCliente = 0;
            for (MainActivity.Cuenta cuenta : cuentasTitular) {
                TextView cuentaTV = new TextView(this);
                cuentaTV.setText("Cod Cuenta: " + cuenta.cuenta);
                cuentaTV.setPadding(10, 20, 10, 4);
                cuentaTV.setTextSize(16);
                layoutSaldosDatos.addView(cuentaTV);

                TableLayout tabla = new TableLayout(this);
                TableRow cabecera = new TableRow(this);
                cabecera.addView(crearCeldaMov("Monto", true));
                cabecera.addView(crearCeldaMov("Fecha", true));
                tabla.addView(cabecera);

                for (MainActivity.Movimiento mov : MainActivity.listaMovimientos) {
                    if (mov.cuenta != null && mov.cuenta.equals(cuenta.cuenta)) {
                        TableRow fila = new TableRow(this);
                        fila.addView(crearCeldaMov(mov.monto, false));
                        fila.addView(crearCeldaMov(mov.fecha, false));
                        tabla.addView(fila);
                        try { saldoTotalCliente += Double.parseDouble(mov.monto); } catch (Exception e) {}
                    }
                }
                tabla.setPadding(10, 0, 10, 16);
                layoutSaldosDatos.addView(tabla);
            }
            TextView saldoTV = new TextView(this);
            saldoTV.setText("SALDO TOTAL DEL CLIENTE: " + saldoTotalCliente);
            saldoTV.setTextSize(18);
            saldoTV.setPadding(10, 24, 10, 14);
            layoutSaldosDatos.addView(saldoTV);
            return;
        } else if (titularEncontrado != null) {
            // Resumen por nombre (cliente)
            scrollDetalle.setVisibility(View.VISIBLE);

            ArrayList<MainActivity.Cuenta> cuentasTitular = new ArrayList<>();
            for (MainActivity.Cuenta c : MainActivity.listaCuentas) {
                if (c.carnet != null && c.carnet.equals(titularEncontrado.carnet)) {
                    cuentasTitular.add(c);
                }
            }
            TextView nombreTV = new TextView(this);
            nombreTV.setText("Nombre: " + titularEncontrado.nombres);
            nombreTV.setTextSize(18);
            nombreTV.setPadding(10, 20, 10, 20);
            layoutSaldosDatos.addView(nombreTV);

            double saldoTotalCliente = 0;
            for (MainActivity.Cuenta cuenta : cuentasTitular) {
                TextView cuentaTV = new TextView(this);
                cuentaTV.setText("Cod Cuenta: " + cuenta.cuenta);
                cuentaTV.setPadding(10, 20, 10, 4);
                cuentaTV.setTextSize(16);
                layoutSaldosDatos.addView(cuentaTV);

                TableLayout tabla = new TableLayout(this);
                TableRow cabecera = new TableRow(this);
                cabecera.addView(crearCeldaMov("Monto", true));
                cabecera.addView(crearCeldaMov("Fecha", true));
                tabla.addView(cabecera);

                for (MainActivity.Movimiento mov : MainActivity.listaMovimientos) {
                    if (mov.cuenta != null && mov.cuenta.equals(cuenta.cuenta)) {
                        TableRow fila = new TableRow(this);
                        fila.addView(crearCeldaMov(mov.monto, false));
                        fila.addView(crearCeldaMov(mov.fecha, false));
                        tabla.addView(fila);
                        try { saldoTotalCliente += Double.parseDouble(mov.monto); } catch (Exception e) {}
                    }
                }
                tabla.setPadding(10, 0, 10, 16);
                layoutSaldosDatos.addView(tabla);
            }
            TextView saldoTV = new TextView(this);
            saldoTV.setText("SALDO TOTAL DEL CLIENTE: " + saldoTotalCliente);
            saldoTV.setTextSize(18);
            saldoTV.setPadding(10, 24, 10, 14);
            layoutSaldosDatos.addView(saldoTV);
            return;
        }

        tvResultadoSaldo.setText("No se encontró esa cuenta/carnet/nombre.");
        tvResultadoSaldo.setVisibility(View.VISIBLE);
    }

    private TextView crearCeldaMov(String texto, boolean esCabecera) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(18, 8, 18, 8);
        tv.setTextSize(esCabecera ? 16 : 15);
        if (esCabecera) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private void limpiarBusqueda() {
        edtCiBuscarSaldo.setText("");
        tvResultadoSaldo.setVisibility(View.GONE);
        layoutSaldosDatos.removeAllViews();
        scrollResumen.setVisibility(View.GONE);
        scrollDetalle.setVisibility(View.GONE);
        barChartSaldos.setVisibility(View.GONE);
        tableSaldos.removeAllViews();
        barChartSaldos.clear();
    }

    private void mostrarTodosSaldos() {
        tableSaldos.removeAllViews();
        TableRow cabecera = new TableRow(this);
        cabecera.addView(crearCelda("Cuenta"));
        cabecera.addView(crearCelda("Carnet"));
        cabecera.addView(crearCelda("Nombre"));
        cabecera.addView(crearCelda("Monto"));
        cabecera.addView(crearCelda("Fecha"));
        tableSaldos.addView(cabecera);

        for (MainActivity.Movimiento mov : MainActivity.listaMovimientos) {
            String cuenta = mov.cuenta;
            String monto = mov.monto;
            String fecha = mov.fecha;

            String carnet = "";
            for (MainActivity.Cuenta c : MainActivity.listaCuentas) {
                if (c.cuenta != null && c.cuenta.equals(cuenta)) {
                    carnet = (c.carnet != null) ? c.carnet : ""; break;
                }
            }
            String nombre = "";
            for (MainActivity.Cliente cl : MainActivity.listaClientes) {
                if (cl.carnet != null && cl.carnet.equals(carnet)) {
                    nombre = (cl.nombres != null) ? cl.nombres : ""; break;
                }
            }

            TableRow fila = new TableRow(this);
            fila.addView(crearCelda(cuenta));
            fila.addView(crearCelda(carnet));
            fila.addView(crearCelda(nombre));
            fila.addView(crearCelda(monto));
            fila.addView(crearCelda(fecha));
            tableSaldos.addView(fila);
        }
    }

    private TextView crearCelda(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setPadding(16, 8, 16, 8);
        tv.setTextSize(16);
        return tv;
    }

    // BarChart: suma mensual de Excel con colores personalizados
    private void mostrarBarraMontoPorMesResumenManual() {
        barChartSaldos.setVisibility(View.VISIBLE);
        String[] nombresMes = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        float[] montosMes = {
                203490, 221112, 226194, 218742, 227993, 212326, 235427, 212066, 234673, 213575, 237978, 245565
        };

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            entries.add(new BarEntry(i, montosMes[i]));
        }

        int[] colores = {
                Color.rgb(33,150,243),   // Ene
                Color.rgb(76,175,80),    // Feb
                Color.rgb(255,193,7),    // Mar
                Color.rgb(244,67,54),    // Abr
                Color.rgb(156,39,176),   // May
                Color.rgb(255,87,34),    // Jun
                Color.rgb(0,150,136),    // Jul
                Color.rgb(205,220,57),   // Ago
                Color.rgb(121,85,72),    // Sep
                Color.rgb(63,81,181),    // Oct
                Color.rgb(139,195,74),   // Nov
                Color.rgb(121,134,203)   // Dic
        };
        ArrayList<Integer> listaColores = new ArrayList<>();
        for (int color : colores) listaColores.add(color);

        BarDataSet dataSet = new BarDataSet(entries, "Monto por mes (Bs)");
        dataSet.setColors(listaColores);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(13f);

        BarData data = new BarData(dataSet);
        barChartSaldos.setData(data);

        barChartSaldos.getXAxis().setValueFormatter(new IndexAxisValueFormatter(nombresMes));
        barChartSaldos.getXAxis().setGranularity(1f);
        barChartSaldos.getXAxis().setGranularityEnabled(true);
        barChartSaldos.getXAxis().setTextSize(12f);
        barChartSaldos.getXAxis().setLabelRotationAngle(-25f);
        barChartSaldos.getDescription().setText("Dinero movido por mes (2023)");
        barChartSaldos.getDescription().setTextSize(13f);
        barChartSaldos.getAxisLeft().setTextSize(12f);
        barChartSaldos.getAxisRight().setEnabled(false);
        barChartSaldos.getLegend().setEnabled(false);

        barChartSaldos.invalidate();
    }
}