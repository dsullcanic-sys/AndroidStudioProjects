package com.ryuk.sispreventas;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentEstadisticas extends Fragment {

    private EditText etEstFechaDesde, etEstFechaHasta;
    private Button btnEstAplicarRango;
    private TextView txtTituloEstadisticas;
    private PieChart pieChartEstados;

    private DatabaseReference pedidosRef;
    private List<Pedido> listaPedidosTodas = new ArrayList<>();

    // MISMO FORMATO QUE REPORTES Y JSON
    private SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private String estFechaDesdeStr;
    private String estFechaHastaStr;

    public FragmentEstadisticas() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_estadisticas, container, false);

        etEstFechaDesde = view.findViewById(R.id.etEstFechaDesde);
        etEstFechaHasta = view.findViewById(R.id.etEstFechaHasta);
        btnEstAplicarRango = view.findViewById(R.id.btnEstAplicarRango);
        txtTituloEstadisticas = view.findViewById(R.id.txtTituloEstadisticas);
        pieChartEstados = view.findViewById(R.id.pieChartEstados);

        pedidosRef = FirebaseDatabase
                .getInstance("https://sispreventa-ryuk-default-rtdb.firebaseio.com/")
                .getReference("pedidos"); // misma URL/nodo que en Reportes [web:482]

        configurarEditTextsFechas();
        configurarPieChart();

        // Fechas por defecto: hoy
        String hoyStr = sdf.format(new Date());
        estFechaDesdeStr = hoyStr;
        estFechaHastaStr = hoyStr;
        etEstFechaDesde.setText(hoyStr);
        etEstFechaHasta.setText(hoyStr);

        cargarPedidos();

        etEstFechaDesde.setOnClickListener(v -> mostrarDatePickerEst(true));
        etEstFechaHasta.setOnClickListener(v -> mostrarDatePickerEst(false));

        btnEstAplicarRango.setOnClickListener(v -> aplicarRangoManual());

        return view;
    }

    private void configurarEditTextsFechas() {
        etEstFechaDesde.setFocusable(false);
        etEstFechaDesde.setClickable(true);
        etEstFechaDesde.setLongClickable(false);

        etEstFechaHasta.setFocusable(false);
        etEstFechaHasta.setClickable(true);
        etEstFechaHasta.setLongClickable(false);
    }

    private void configurarPieChart() {
        // Solo cantidades, sin porcentaje
        pieChartEstados.setUsePercentValues(false); // clave para no mostrar %
        pieChartEstados.setDrawHoleEnabled(true);
        pieChartEstados.setHoleRadius(40f);
        pieChartEstados.setTransparentCircleRadius(45f);
        pieChartEstados.setEntryLabelColor(Color.BLACK);

        Description desc = new Description();
        desc.setText("");
        pieChartEstados.setDescription(desc);

        Legend l = pieChartEstados.getLegend();
        l.setEnabled(true);
    }

    private void cargarPedidos() {
        pedidosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaPedidosTodas.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Pedido p = ds.getValue(Pedido.class);
                    if (p != null) {
                        listaPedidosTodas.add(p);
                    }
                }
                // Por defecto: hoy
                try {
                    Date hoy = sdf.parse(estFechaDesdeStr);
                    if (hoy != null) {
                        txtTituloEstadisticas.setText("Estadísticas - Hoy");
                        mostrarPieParaRango(hoy, hoy);
                    }
                } catch (Exception ignored) { }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(
                            getContext(),
                            "Error al cargar pedidos: " + error.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    private void mostrarDatePickerEst(boolean esDesde) {
        if (getContext() == null) return;

        Calendar cal = Calendar.getInstance();
        try {
            String baseStr = esDesde ? estFechaDesdeStr : estFechaHastaStr;
            Date base = sdf.parse(baseStr);
            if (base != null) {
                cal.setTime(base);
            }
        } catch (ParseException ignored) { }

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH); // 0-11
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int y, int m, int d) {
                        Calendar c = Calendar.getInstance();
                        c.set(y, m, d, 0, 0, 0);
                        String seleccionadaStr = sdf.format(c.getTime());
                        if (esDesde) {
                            estFechaDesdeStr = seleccionadaStr;
                            etEstFechaDesde.setText(seleccionadaStr);
                        } else {
                            estFechaHastaStr = seleccionadaStr;
                            etEstFechaHasta.setText(seleccionadaStr);
                        }
                    }
                },
                year, month, day
        );
        dialog.show();
    }

    private void aplicarRangoManual() {
        try {
            Date dDesde = sdf.parse(estFechaDesdeStr);
            Date dHasta = sdf.parse(estFechaHastaStr);
            if (dDesde != null && dHasta != null) {
                if (dDesde.after(dHasta)) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Fecha desde no puede ser mayor que fecha hasta",
                                Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                txtTituloEstadisticas.setText(
                        "Estadísticas (" + estFechaDesdeStr + " a " + estFechaHastaStr + ")"
                );
                mostrarPieParaRango(dDesde, dHasta);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void mostrarPieParaRango(Date inicio, Date fin) {
        // Normalizar fechas a solo día
        Calendar cIni = Calendar.getInstance();
        cIni.setTime(inicio);
        cIni.set(Calendar.HOUR_OF_DAY, 0);
        cIni.set(Calendar.MINUTE, 0);
        cIni.set(Calendar.SECOND, 0);
        cIni.set(Calendar.MILLISECOND, 0);
        Date iniDia = cIni.getTime();

        Calendar cFin = Calendar.getInstance();
        cFin.setTime(fin);
        cFin.set(Calendar.HOUR_OF_DAY, 0);
        cFin.set(Calendar.MINUTE, 0);
        cFin.set(Calendar.SECOND, 0);
        cFin.set(Calendar.MILLISECOND, 0);
        Date finDia = cFin.getTime();

        int totalEntregado = 0;
        int totalCancelado = 0;

        for (Pedido p : listaPedidosTodas) {
            try {
                if (p.getFecha() == null) continue;
                Date fecha = sdf.parse(p.getFecha());
                if (fecha == null) continue;

                Calendar cF = Calendar.getInstance();
                cF.setTime(fecha);
                cF.set(Calendar.HOUR_OF_DAY, 0);
                cF.set(Calendar.MINUTE, 0);
                cF.set(Calendar.SECOND, 0);
                cF.set(Calendar.MILLISECOND, 0);
                Date fechaDia = cF.getTime();

                if ((fechaDia.equals(iniDia) || fechaDia.after(iniDia)) &&
                        (fechaDia.equals(finDia) || fechaDia.before(finDia))) {

                    String estado = p.getEstado();
                    if ("ENTREGADO".equals(estado)) totalEntregado++;
                    else if ("CANCELADO".equals(estado)) totalCancelado++;
                }
            } catch (Exception ignored) { }
        }

        List<PieEntry> entries = new ArrayList<>();
        if (totalEntregado > 0) {
            entries.add(new PieEntry(totalEntregado, "ENTREGADO"));
        }
        if (totalCancelado > 0) {
            entries.add(new PieEntry(totalCancelado, "CANCELADO"));
        }

        if (entries.isEmpty()) {
            pieChartEstados.clear();
            if (getContext() != null) {
                Toast.makeText(getContext(),
                        "No hay datos para este rango",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(3f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        // Sin formatter extra: mostrará la CANTIDAD (2, 3, etc.)
        pieChartEstados.setData(data);
        pieChartEstados.invalidate(); // redibuja el gráfico [web:490]
    }
}
