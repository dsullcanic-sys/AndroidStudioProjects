package com.ryuk.graficas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class Profesiones extends AppCompatActivity {
    EditText edtBuscarProfesion;
    Button btnBuscarProfesion, btnLimpiarProfesion;
    TextView tvResultadoProfesion;
    TableLayout tableProfesiones;
    BarChart barChartProfesiones;

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

        tvResultadoProfesion.setVisibility(View.GONE);
        tableProfesiones.setVisibility(View.GONE);
        barChartProfesiones.setVisibility(View.GONE);

        btnBuscarProfesion.setOnClickListener(v -> buscarProfesion());
        btnLimpiarProfesion.setOnClickListener(v -> limpiarBusqueda());
    }

    private void buscarProfesion() {
        String query = edtBuscarProfesion.getText().toString().trim();
        tvResultadoProfesion.setVisibility(View.GONE);
        tableProfesiones.removeAllViews();
        tableProfesiones.setVisibility(View.GONE);
        barChartProfesiones.setVisibility(View.GONE);

        // Buscar vacío = mostrar resumen
        if (query.isEmpty()) {
            tableProfesiones.setVisibility(View.VISIBLE);
            barChartProfesiones.setVisibility(View.VISIBLE);
            mostrarTablaYBarras(MainActivity.listaProfesiones, false);
            return;
        }

        // Filtra profesiones según búsqueda
        ArrayList<MainActivity.Profesion> profesionesCoincidentes = new ArrayList<>();
        for (MainActivity.Profesion p : MainActivity.listaProfesiones) {
            if (p.codprof.equalsIgnoreCase(query) || p.descripcion.toLowerCase().contains(query.toLowerCase())) {
                profesionesCoincidentes.add(p);
            }
        }

        if (profesionesCoincidentes.isEmpty()) {
            tvResultadoProfesion.setText("No se encontró profesión.");
            tvResultadoProfesion.setVisibility(View.VISIBLE);
            return;
        }

        tableProfesiones.setVisibility(View.VISIBLE);
        barChartProfesiones.setVisibility(View.VISIBLE);

        // Si es solo una profesión: ficha (uno debajo de otro)
        if (profesionesCoincidentes.size() == 1) {
            mostrarUnaProfesionEnTabla(profesionesCoincidentes.get(0));
            mostrarBarraUnaProfesion(profesionesCoincidentes.get(0));
        } else {
            // Varias: vista normal tipo tabla resumen
            mostrarTablaYBarras(profesionesCoincidentes, true);
        }
    }

    // Vista ficha de una sola profesión (uno debajo de otro)
    private void mostrarUnaProfesionEnTabla(MainActivity.Profesion p) {
        tableProfesiones.removeAllViews();
        tableProfesiones.addView(filaDato("ID:", p.codprof));
        tableProfesiones.addView(filaDato("Descripción:", p.descripcion));
        int nroCuentas = 0;
        double saldoTotal = 0;
        for (MainActivity.Cliente cl : MainActivity.listaClientes) {
            if (cl.codprof.equals(p.codprof)) {
                for (MainActivity.Cuenta c : MainActivity.listaCuentas) {
                    if (c.carnet.equals(cl.carnet)) {
                        nroCuentas++;
                        for (MainActivity.Movimiento mov : MainActivity.listaMovimientos) {
                            if (mov.cuenta.equals(c.cuenta)) {
                                try { saldoTotal += Double.parseDouble(mov.monto); } catch (Exception e) {}
                            }
                        }
                    }
                }
            }
        }
        tableProfesiones.addView(filaDato("Nro Cuentas:", String.valueOf(nroCuentas)));
        tableProfesiones.addView(filaDato("Saldo Total:", String.valueOf(saldoTotal)));
    }

    private TableRow filaDato(String label, String dato) {
        TableRow fila = new TableRow(this);
        fila.addView(crearCelda(label));
        fila.addView(crearCelda(dato));
        return fila;
    }

    // Solo barra para una profesión
    private void mostrarBarraUnaProfesion(MainActivity.Profesion p) {
        int nroCuentas = 0;
        double saldoTotal = 0;
        for (MainActivity.Cliente cl : MainActivity.listaClientes) {
            if (cl.codprof.equals(p.codprof)) {
                for (MainActivity.Cuenta c : MainActivity.listaCuentas) {
                    if (c.carnet.equals(cl.carnet)) {
                        nroCuentas++;
                        for (MainActivity.Movimiento mov : MainActivity.listaMovimientos) {
                            if (mov.cuenta.equals(c.cuenta)) {
                                try { saldoTotal += Double.parseDouble(mov.monto); } catch (Exception e) {}
                            }
                        }
                    }
                }
            }
        }
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        entries.add(new BarEntry(0, (float) saldoTotal));
        labels.add(p.descripcion);

        BarDataSet dataSet = new BarDataSet(entries, "Saldo por profesión");
        dataSet.setColors(Color.rgb(33,150,243));
        // Adaptativo claro/oscuro:
        dataSet.setValueTextColor(
                (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES ? Color.WHITE : Color.BLACK
        );

        BarData data = new BarData(dataSet);

        barChartProfesiones.setData(data);
        barChartProfesiones.getDescription().setEnabled(false);
        barChartProfesiones.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartProfesiones.getXAxis().setGranularity(1f);
        barChartProfesiones.getXAxis().setGranularityEnabled(true);
        barChartProfesiones.getXAxis().setLabelRotationAngle(-45f);
        barChartProfesiones.invalidate();
    }

    // Tabla normal con varias profesiones
    private void mostrarTablaYBarras(ArrayList<MainActivity.Profesion> lista, boolean soloMostrarCoincidentes) {
        tableProfesiones.removeAllViews();
        TableRow cabecera = new TableRow(this);
        cabecera.addView(crearCelda("ID"));
        cabecera.addView(crearCelda("Descripción"));
        cabecera.addView(crearCelda("Nro Cuentas"));
        cabecera.addView(crearCelda("Saldo Total"));
        tableProfesiones.addView(cabecera);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;

        for (MainActivity.Profesion p : lista) {
            int nroCuentas = 0;
            double saldoTotal = 0;
            for (MainActivity.Cliente cl : MainActivity.listaClientes) {
                if (cl.codprof.equals(p.codprof)) {
                    for (MainActivity.Cuenta c : MainActivity.listaCuentas) {
                        if (c.carnet.equals(cl.carnet)) {
                            nroCuentas++;
                            for (MainActivity.Movimiento mov : MainActivity.listaMovimientos) {
                                if (mov.cuenta.equals(c.cuenta)) {
                                    try { saldoTotal += Double.parseDouble(mov.monto); } catch (Exception e) {}
                                }
                            }
                        }
                    }
                }
            }

            TableRow fila = new TableRow(this);
            fila.addView(crearCelda(p.codprof));
            fila.addView(crearCelda(p.descripcion));
            fila.addView(crearCelda(String.valueOf(nroCuentas)));
            fila.addView(crearCelda(String.valueOf(saldoTotal)));
            tableProfesiones.addView(fila);

            entries.add(new BarEntry(i, (float) saldoTotal));
            labels.add(p.descripcion);
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Saldo por profesión");
        dataSet.setColors(Color.rgb(33,150,243), Color.rgb(255,193,7), Color.rgb(244,67,54), Color.rgb(76,175,80));
        // Adaptativo claro/oscuro:
        dataSet.setValueTextColor(
                (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES ? Color.WHITE : Color.BLACK
        );

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
        tv.setPadding(12, 8, 12, 8);
        tv.setTextSize(15);
        return tv;
    }

    private void limpiarBusqueda() {
        edtBuscarProfesion.setText("");
        tvResultadoProfesion.setVisibility(View.GONE);
        tableProfesiones.removeAllViews();
        barChartProfesiones.clear();
        tableProfesiones.setVisibility(View.GONE);
        barChartProfesiones.setVisibility(View.GONE);
    }
}
