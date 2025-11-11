package com.ryuk.kardex

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.net.Uri
import android.widget.EditText
import android.widget.Toast
import android.graphics.Color
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var btnCargar: Button
    private lateinit var txtInfo: TextView
    private lateinit var inputCarnet: EditText
    private lateinit var btnBuscar: Button
    private lateinit var barChart: HorizontalBarChart
    private lateinit var txtNombreDocente: TextView
    private lateinit var switchVista: SwitchCompat
    private lateinit var layoutSwitch: View

    private lateinit var notasDocentes: Map<String, List<Registro>>
    private var registrosActuales: List<Registro> = emptyList()
    private var carnetActual: String = ""

    private val materias = mutableListOf<Materia>()
    private val periodos = mutableListOf<Periodo>()
    private val docentes = mutableListOf<Docente>()
    private val estudiantes = mutableListOf<Estudiante>()
    private val kardex = mutableListOf<Kardex>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        btnCargar = findViewById(R.id.btnCargar)
        txtInfo = findViewById(R.id.txtInfo)
        inputCarnet = findViewById(R.id.inputCarnet)
        btnBuscar = findViewById(R.id.btnBuscar)
        barChart = findViewById(R.id.barChart)
        txtNombreDocente = findViewById(R.id.txtNombreDocente)
        switchVista = findViewById(R.id.switchVista)
        layoutSwitch = findViewById(R.id.layoutSwitch)

        btnCargar.setOnClickListener { seleccionarArchivos() }
        btnBuscar.setOnClickListener { buscar() }

        switchVista.setOnCheckedChangeListener { _, isChecked ->
            if (carnetActual.isNotEmpty() && registrosActuales.isNotEmpty()) {
                if (isChecked) {
                    mostrarGraficoPorAnio(carnetActual, registrosActuales)
                } else {
                    mostrarGraficoPorPeriodo(carnetActual, registrosActuales)
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
    }

    private val selector = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            resultado.data?.let { data ->
                if (data.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        leerArchivo(data.clipData!!.getItemAt(i).uri)
                    }
                } else if (data.data != null) {
                    leerArchivo(data.data!!)
                }
            }
            enlazar()
            txtInfo.text = "Archivos cargados: ${docentes.size} docentes"
        }
    }

    private fun seleccionarArchivos() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "text/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        selector.launch(intent)
    }

    private fun leerArchivo(uri: Uri) {
        try {
            val nombre = obtenerNombre(uri)
            contentResolver.openInputStream(uri)
                ?.bufferedReader(charset("Windows-1252"))
                ?.useLines { lineas ->
                    lineas.drop(1).forEach { procesarLinea(it, nombre) }
                }
        } catch (e: Exception) {
            txtInfo.text = "Error: ${e.message}"
        }
    }

    private fun obtenerNombre(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val indice = cursor.getColumnIndex("_display_name")
                if (indice != -1) return cursor.getString(indice)
            }
        }
        return "desconocido"
    }

    private fun procesarLinea(linea: String, archivo: String) {
        val partes = linea.split(';')

        when(partes.size) {
            2 -> {
                if (archivo.contains("materias", ignoreCase = true)){
                    materias.add(Materia(partes[0].trim(), partes[1].trim()))
                } else if (archivo.contains("periodos", ignoreCase = true)){
                    periodos.add(Periodo(partes[0].trim(), partes[1].trim()))
                }
            }
            4 -> {
                if (archivo.contains("estudiantes", ignoreCase = true)){
                    estudiantes.add(
                        Estudiante(
                            partes[0].trim(),
                            partes[1].trim(),
                            partes[2].trim(),
                            partes[3].trim()
                        )
                    )
                } else if (archivo.contains("docentes", ignoreCase = true)){
                    docentes.add(
                        Docente(
                            partes[0].trim(),
                            partes[1].trim(),
                            partes[2].trim(),
                            partes[3].trim()
                        )
                    )
                }
            }
            6 -> {
                if (archivo.contains("kardex", ignoreCase = true)){
                    kardex.add(
                        Kardex(
                            partes[0].trim(),
                            partes[1].trim(),
                            partes[2].trim(),
                            partes[3].trim(),
                            partes[4].trim().toInt(),
                            partes[5].trim()
                        )
                    )
                }
            }
        }
    }

    private fun enlazar(){
        val mapMaterias = materias.associateBy { it.sigla }
        val mapDocentes = docentes.associateBy { it.carnet }
        val mapPeriodos = periodos.associateBy { it.codigo }

        var descartados = 0
        var procesados = 0

        val registros = kardex.mapNotNull { k ->
            val doc = mapDocentes[k.carnetDoc]
            val mat = mapMaterias[k.siglaMateria]
            val per = mapPeriodos[k.codigoPeriodo]

            // Solo verificamos que existan docente, materia y período
            // El estudiante puede o no estar en estudiantes.csv
            if (doc != null && mat != null && per != null) {
                procesados++
                Registro(k.carnetEst, mat, doc, per, k.nota, k.obs)
            } else {
                descartados++
                // Debug: ver qué falta
                if (doc == null) android.util.Log.d("KARDEX", "Docente no encontrado: ${k.carnetDoc}")
                if (mat == null) android.util.Log.d("KARDEX", "Materia no encontrada: ${k.siglaMateria}")
                if (per == null) android.util.Log.d("KARDEX", "Periodo no encontrado: ${k.codigoPeriodo}")
                null
            }
        }

        notasDocentes = registros.groupBy { it.docente.carnet }

        android.util.Log.d("KARDEX", "Total kardex: ${kardex.size}, Procesados: $procesados, Descartados: $descartados")
        android.util.Log.d("KARDEX", "Docentes cargados: ${docentes.size}")
        android.util.Log.d("KARDEX", "Materias cargadas: ${materias.size}")
        android.util.Log.d("KARDEX", "Periodos cargados: ${periodos.size}")
    }

    private fun ordenar(periodos: List<String>): List<String> {
        return periodos.sortedWith(compareBy(
            { it.substringAfter('-') },
            { it.substringBefore('-') }
        ))
    }

    private fun buscar() {
        val carnet = inputCarnet.text.toString().trim().uppercase()

        if (carnet.isEmpty()) {
            Toast.makeText(this, "Introduce un carnet", Toast.LENGTH_SHORT).show()
            return
        }

        val registros = notasDocentes[carnet] ?: run {
            Toast.makeText(this, "Docente no encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        carnetActual = carnet
        registrosActuales = registros

        // Mostrar vista por períodos por defecto
        switchVista.isChecked = false
        mostrarGraficoPorPeriodo(carnet, registros)

        // Mostrar el switch y el gráfico
        layoutSwitch.visibility = View.VISIBLE
        barChart.visibility = View.VISIBLE
    }

    private fun mostrarGraficoPorPeriodo(carnet: String, registros: List<Registro>) {
        val doc = docentes.find { it.carnet == carnet }
        val nombre = "${doc?.nombres} ${doc?.paterno}"

        txtNombreDocente.text = "Docente: $nombre\nCarnet: $carnet"
        txtNombreDocente.visibility = View.VISIBLE
        txtInfo.visibility = View.GONE

        // SOLO agrupar por períodos donde el docente tiene registros
        val porPeriodo = registros.groupBy { it.periodo.codigo }
        val periodosOrdenados = ordenar(porPeriodo.keys.toList()).reversed()

        android.util.Log.d("KARDEX", "Docente: $carnet tiene registros en ${porPeriodo.size} períodos")
        android.util.Log.d("KARDEX", "Períodos: ${periodosOrdenados.joinToString(", ")}")

        val aprobadosEntries = mutableListOf<BarEntry>()
        val reprobadosEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        periodosOrdenados.forEachIndexed { index, periodo ->
            val notas = porPeriodo[periodo]!!
            val aprob = notas.count { it.nota >= 51 }
            val reprob = notas.size - aprob

            android.util.Log.d("KARDEX", "Periodo $periodo: Aprob=$aprob, Reprob=$reprob, Total=${notas.size}")

            aprobadosEntries.add(BarEntry(index.toFloat(), aprob.toFloat()))
            reprobadosEntries.add(BarEntry(index.toFloat(), reprob.toFloat()))
            labels.add(periodo)
        }

        configurarGrafico(aprobadosEntries, reprobadosEntries, labels, "Resumen por Períodos")
    }

    private fun mostrarGraficoPorAnio(carnet: String, registros: List<Registro>) {
        val doc = docentes.find { it.carnet == carnet }
        val nombre = "${doc?.nombres} ${doc?.paterno}"

        txtNombreDocente.text = "Docente: $nombre\nCarnet: $carnet"
        txtNombreDocente.visibility = View.VISIBLE
        txtInfo.visibility = View.GONE

        val porAnio = registros.groupBy { it.periodo.codigo.substringAfter('-') }
        val aniosOrdenados = porAnio.keys.sorted().reversed()

        val aprobadosEntries = mutableListOf<BarEntry>()
        val reprobadosEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        aniosOrdenados.forEachIndexed { index, anio ->
            val notas = porAnio[anio]!!
            val aprob = notas.count { it.nota >= 51 }
            val reprob = notas.size - aprob

            aprobadosEntries.add(BarEntry(index.toFloat(), aprob.toFloat()))
            reprobadosEntries.add(BarEntry(index.toFloat(), reprob.toFloat()))
            labels.add(anio)
        }

        configurarGrafico(aprobadosEntries, reprobadosEntries, labels, "Resumen por Años")
    }

    private fun configurarGrafico(
        aprobadosEntries: List<BarEntry>,
        reprobadosEntries: List<BarEntry>,
        labels: List<String>,
        titulo: String
    ) {
        // Colores pasteles
        val colorAprobados = Color.rgb(144, 238, 144) // Verde pastel
        val colorReprobados = Color.rgb(255, 182, 193) // Rojo pastel claro

        val dataSetAprobados = BarDataSet(aprobadosEntries, "Aprobados").apply {
            color = colorAprobados
            valueTextSize = 10f
            valueTextColor = Color.BLACK
        }

        val dataSetReprobados = BarDataSet(reprobadosEntries, "Reprobados").apply {
            color = colorReprobados
            valueTextSize = 10f
            valueTextColor = Color.BLACK
        }

        val barData = BarData(dataSetAprobados, dataSetReprobados).apply {
            barWidth = 0.4f
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) value.toInt().toString() else ""
                }
            })
        }

        barChart.apply {
            data = barData
            description.text = titulo
            description.textSize = 14f
            description.textColor = Color.BLACK

            setFitBars(true)
            animateY(800)
            setDrawGridBackground(false)

            // Configurar eje X (vertical en HorizontalBarChart)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                textSize = 10f
                textColor = Color.BLACK
                setDrawGridLines(false)
                labelCount = labels.size
                setLabelCount(labels.size, true)
            }

            // Configurar eje Y izquierdo (horizontal)
            axisLeft.apply {
                textSize = 10f
                textColor = Color.BLACK
                axisMinimum = 0f
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                granularity = 1f
            }

            // Desactivar eje Y derecho
            axisRight.isEnabled = false

            // Configurar leyenda
            legend.apply {
                textSize = 12f
                textColor = Color.BLACK
                formSize = 12f
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
            }

            // Ajustar altura según cantidad de datos
            val alturaPorBarra = 70
            val alturaMinima = 500
            val altura = (labels.size * alturaPorBarra).coerceAtLeast(alturaMinima)
            layoutParams.height = altura

            // Agrupar barras
            val groupSpace = 0.1f
            val barSpace = 0.05f
            groupBars(-0.5f, groupSpace, barSpace)

            // Hacer zoom out para mostrar todo
            setVisibleXRangeMaximum(labels.size.toFloat())

            invalidate()
        }
    }
}

data class Kardex(
    val carnetEst: String,
    val siglaMateria: String,
    val carnetDoc: String,
    val codigoPeriodo: String,
    val nota: Int,
    val obs: String
)

data class Estudiante(
    val carnet: String,
    val paterno: String,
    val materno: String,
    val nombres: String
)

data class Materia(
    val sigla: String,
    val descripcion: String
)

data class Docente(
    val carnet: String,
    val paterno: String,
    val materno: String,
    val nombres: String
)

data class Periodo(
    val codigo: String,
    val descripcion: String
)

data class Registro(
    val carnetEst: String,
    val materia: Materia,
    val docente: Docente,
    val periodo: Periodo,
    val nota: Int,
    val obs: String
)