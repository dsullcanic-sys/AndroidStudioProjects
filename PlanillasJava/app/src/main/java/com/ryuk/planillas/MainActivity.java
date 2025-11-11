package com.ryuk.planillas;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // ===== COMPONENTES UI =====
    private EditText etCarnet;
    private Button btnBuscar, btnCargar;
    private LinearLayout containerResultados;
    private TableLayout tableDatos;

    // ===== LISTAS DE DATOS =====
    private ArrayList<Empleado> listaEmpleados = new ArrayList<>();
    private ArrayList<Cargo> listaCargos = new ArrayList<>();
    private ArrayList<PlanillaMensual> listaPlanillas = new ArrayList<>();

    // ===== SELECTOR DE ARCHIVOS =====
    private final ActivityResultLauncher<Intent> filePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    if (data.getClipData() != null) {
                        // Múltiples archivos seleccionados
                        for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                            leerArchivo(data.getClipData().getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        // Un solo archivo seleccionado
                        leerArchivo(data.getData());
                    }
                }
            }
    );

    // ===== MÉTODOS DEL CICLO DE VIDA =====

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar componentes UI
        etCarnet = findViewById(R.id.etCarnet);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnCargar = findViewById(R.id.btnCargar);
        containerResultados = findViewById(R.id.containerResultados);
        tableDatos = findViewById(R.id.tableDatos);

        // Configurar listeners
        btnCargar.setOnClickListener(v -> elegirArchivos());
        btnBuscar.setOnClickListener(v -> buscarYMostrar(etCarnet.getText().toString().trim()));
    }

    // ===== MÉTODOS DE SELECCIÓN DE ARCHIVOS =====

    private void elegirArchivos() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePicker.launch(intent);
    }

    // ===== MÉTODOS DE LECTURA DE ARCHIVOS =====

    private void leerArchivo(Uri uri) {
        try {
            String nombreArchivo = obtenerNombreArchivo(uri);
            if (nombreArchivo == null) return;

            nombreArchivo = nombreArchivo.toLowerCase(Locale.getDefault());
            InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) return;

            BufferedReader lector = new BufferedReader(new InputStreamReader(input));
            String linea;
            boolean primeraLinea = true;

            while ((linea = lector.readLine()) != null) {
                // Saltar la primera línea (encabezados)
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                // Saltar líneas vacías
                if (linea.trim().isEmpty()) continue;

                // Dividir la línea por punto y coma
                String[] datos = linea.split(";");

                // Procesar según el tipo de archivo
                if (nombreArchivo.contains("personal")) {
                    procesarPersonal(datos);
                } else if (nombreArchivo.contains("cargos")) {
                    procesarCargos(datos);
                } else if (nombreArchivo.startsWith("pla")) {
                    procesarPlanilla(nombreArchivo, datos);
                } else if (nombreArchivo.startsWith("bon")) {
                    procesarBono(nombreArchivo, datos);
                } else if (nombreArchivo.startsWith("des")) {
                    procesarDescuento(nombreArchivo, datos);
                }
            }

            lector.close();
            input.close();

        } catch (Exception ignored) {
            // Error al leer archivo - se ignora silenciosamente
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = null;

        // Intentar obtener nombre del cursor
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (columnIndex != -1) {
                nombre = cursor.getString(columnIndex);
            }
            cursor.close();
        }

        // Si no se pudo obtener del cursor, extraer del path
        if (nombre == null) {
            String path = uri.getPath();
            if (path != null) {
                String[] partes = path.split("/");
                nombre = partes[partes.length - 1];
            }
        }

        return nombre;
    }

    // ===== MÉTODOS DE PROCESAMIENTO DE DATOS =====

    private void procesarPersonal(String[] datos) {
        if (datos.length < 4) return;

        String carnet = datos[0].trim();
        String nombres = datos[3].trim();
        String apellidos = datos[1].trim() + " " + datos[2].trim();

        listaEmpleados.add(new Empleado(carnet, nombres + "|" + apellidos));
    }

    private void procesarCargos(String[] datos) {
        if (datos.length < 3) return;

        String idCargo = datos[0].trim();
        String descripcion = datos[1].trim();
        int salarioBasico = 0;

        try {
            salarioBasico = Integer.parseInt(datos[2].trim());
        } catch (Exception ignored) {
            // Error al convertir salario - se mantiene en 0
        }

        listaCargos.add(new Cargo(idCargo, descripcion, salarioBasico));
    }

    private void procesarPlanilla(String nombreArchivo, String[] datos) {
        if (datos.length < 3) return;

        String mes = nombreArchivo.substring(3, 7);
        String carnet = datos[1].trim();
        String idCargo = datos[2].trim();

        PlanillaMensual planilla = buscarOCrearPlanilla(mes, carnet);
        planilla.idCargo = idCargo;
    }

    private void procesarBono(String nombreArchivo, String[] datos) {
        if (datos.length < 3) return;

        String mes = nombreArchivo.substring(3, 7);
        String carnet = datos[1].trim();
        int montoBono = 0;

        try {
            montoBono = Integer.parseInt(datos[2].trim());
        } catch (Exception ignored) {
            // Error al convertir monto - se mantiene en 0
        }

        PlanillaMensual planilla = buscarOCrearPlanilla(mes, carnet);
        planilla.bonos.add(montoBono);
    }

    private void procesarDescuento(String nombreArchivo, String[] datos) {
        if (datos.length < 3) return;

        String mes = nombreArchivo.substring(3, 7);
        String carnet = datos[1].trim();
        int montoDescuento = 0;

        try {
            montoDescuento = Integer.parseInt(datos[2].trim());
        } catch (Exception ignored) {
            // Error al convertir monto - se mantiene en 0
        }

        PlanillaMensual planilla = buscarOCrearPlanilla(mes, carnet);
        planilla.descuentos.add(montoDescuento);
    }

    private PlanillaMensual buscarOCrearPlanilla(String mes, String carnet) {
        // Buscar planilla existente
        for (PlanillaMensual planilla : listaPlanillas) {
            if (planilla.mes.equals(mes) && planilla.carnet.equals(carnet)) {
                return planilla;
            }
        }

        // Si no existe, crear una nueva
        PlanillaMensual nuevaPlanilla = new PlanillaMensual(mes, carnet);
        listaPlanillas.add(nuevaPlanilla);
        return nuevaPlanilla;
    }

    // ===== MÉTODOS DE BÚSQUEDA Y VISUALIZACIÓN =====

    private void buscarYMostrar(String carnet) {
        // Limpiar resultados anteriores
        containerResultados.removeAllViews();
        tableDatos.removeAllViews();

        if (carnet.isEmpty()) return;

        // Buscar datos del empleado
        Empleado empleado = buscarEmpleado(carnet);
        mostrarDatosPersonales(carnet, empleado);

        // Obtener y ordenar planillas del empleado
        ArrayList<PlanillaMensual> planillasEmpleado = obtenerPlanillasDeEmpleado(carnet);
        ordenarPlanillasPorMes(planillasEmpleado);

        // Limpiar campo de búsqueda si hay resultados
        if (!planillasEmpleado.isEmpty()) {
            etCarnet.setText("");
        }

        // Mostrar cada planilla
        for (PlanillaMensual planilla : planillasEmpleado) {
            mostrarPlanilla(planilla);
        }
    }

    private Empleado buscarEmpleado(String carnet) {
        for (Empleado empleado : listaEmpleados) {
            if (empleado.carnet.equals(carnet)) {
                return empleado;
            }
        }
        return null;
    }

    private ArrayList<PlanillaMensual> obtenerPlanillasDeEmpleado(String carnet) {
        ArrayList<PlanillaMensual> resultado = new ArrayList<>();

        for (PlanillaMensual planilla : listaPlanillas) {
            if (planilla.carnet.equals(carnet)) {
                resultado.add(planilla);
            }
        }

        return resultado;
    }

    private void ordenarPlanillasPorMes(ArrayList<PlanillaMensual> planillas) {
        Collections.sort(planillas, (p1, p2) -> p1.mes.compareTo(p2.mes));
    }

    // ===== MÉTODOS DE CREACIÓN DE UI =====

    private void mostrarDatosPersonales(String carnet, Empleado empleado) {
        if (empleado != null) {
            String[] partes = empleado.nombreCompleto.split("\\|");
            String nombres = partes[0];
            String apellidos = partes.length > 1 ? partes[1] : "";

            tableDatos.addView(crearFilaTabla("Carnet:", carnet));
            tableDatos.addView(crearFilaTabla("Nombre:", nombres));
            tableDatos.addView(crearFilaTabla("Apellidos:", apellidos));
        } else {
            tableDatos.addView(crearFilaTabla("Carnet:", carnet + " (Empleado no encontrado)"));
        }
    }

    private TableRow crearFilaTabla(String etiqueta, String valor) {
        TableRow fila = new TableRow(this);

        // TextView para la etiqueta (en negrita)
        TextView tvEtiqueta = new TextView(this);
        tvEtiqueta.setText(etiqueta);
        tvEtiqueta.setTypeface(Typeface.DEFAULT_BOLD);
        tvEtiqueta.setPadding(8, 4, 8, 4);

        // TextView para el valor (fuente monospace)
        TextView tvValor = new TextView(this);
        tvValor.setText(valor);
        tvValor.setTypeface(Typeface.MONOSPACE);
        tvValor.setPadding(8, 4, 8, 4);

        // Agregar ambos TextViews a la fila
        fila.addView(tvEtiqueta);
        fila.addView(tvValor);

        return fila;
    }

    private void mostrarPlanilla(PlanillaMensual planilla) {
        // Obtener datos del cargo
        Cargo cargo = buscarCargo(planilla.idCargo);
        String nombreCargo = cargo != null ? cargo.descripcion : "Sin cargo";
        int salarioBasico = cargo != null ? cargo.salarioBasico : 0;

        // Calcular totales
        int totalBonos = sumarLista(planilla.bonos);
        int totalDescuentos = sumarLista(planilla.descuentos);
        int salarioNeto = salarioBasico + totalBonos - totalDescuentos;

        // Crear TextView del mes (clickeable)
        TextView tvMes = new TextView(this);
        tvMes.setText(formatearMes(planilla.mes));
        tvMes.setTextSize(18f);
        tvMes.setPadding(12, 12, 12, 12);
        tvMes.setTypeface(Typeface.DEFAULT_BOLD);

        // Crear tabla de detalles (inicialmente oculta)
        TableLayout tablaDetalle = new TableLayout(this);
        tablaDetalle.setPadding(24, 0, 12, 12);
        tablaDetalle.setVisibility(View.GONE);

        // Agregar filas a la tabla de detalles
        tablaDetalle.addView(crearFilaTabla("Cargo:", nombreCargo));
        tablaDetalle.addView(crearFilaTabla("Básico:", String.valueOf(salarioBasico)));
        tablaDetalle.addView(crearFilaTabla("Bonos:", planilla.bonos.size() + " (" + totalBonos + ")"));
        tablaDetalle.addView(crearFilaTabla("Descuentos:", planilla.descuentos.size() + " (" + totalDescuentos + ")"));
        tablaDetalle.addView(crearFilaTabla("Neto:", String.valueOf(salarioNeto)));

        // Configurar click listener para mostrar/ocultar detalles
        tvMes.setOnClickListener(v -> {
            tablaDetalle.setVisibility(
                    tablaDetalle.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );
        });

        // Agregar componentes al contenedor
        containerResultados.addView(tvMes);
        containerResultados.addView(tablaDetalle);
    }

    // ===== MÉTODOS AUXILIARES =====

    private Cargo buscarCargo(String idCargo) {
        for (Cargo cargo : listaCargos) {
            if (cargo.id.equals(idCargo)) {
                return cargo;
            }
        }
        return null;
    }

    private int sumarLista(ArrayList<Integer> lista) {
        int suma = 0;
        for (int valor : lista) {
            suma += valor;
        }
        return suma;
    }

    private String formatearMes(String mes) {
        if (mes.length() != 4) return mes;

        int numeroMes;
        try {
            numeroMes = Integer.parseInt(mes.substring(0, 2));
        } catch (Exception e) {
            return mes;
        }

        String año = "20" + mes.substring(2);
        String[] nombresMeses = {
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };

        if (numeroMes >= 1 && numeroMes <= 12) {
            return nombresMeses[numeroMes - 1] + " " + año;
        }

        return mes;
    }

    // ===== CLASES INTERNAS =====

    /**
     * Clase para representar un empleado
     */
    class Empleado {
        String carnet, nombreCompleto;

        Empleado(String c, String n) {
            carnet = c;
            nombreCompleto = n;
        }
    }

    /**
     * Clase para representar un cargo
     */
    class Cargo {
        String id, descripcion;
        int salarioBasico;

        Cargo(String i, String d, int s) {
            id = i;
            descripcion = d;
            salarioBasico = s;
        }
    }

    /**
     * Clase para representar una planilla mensual
     */
    class PlanillaMensual {
        String mes, carnet, idCargo;
        ArrayList<Integer> bonos, descuentos;

        PlanillaMensual(String m, String c) {
            mes = m;
            carnet = c;
            bonos = new ArrayList<>();
            descuentos = new ArrayList<>();
        }
    }
}