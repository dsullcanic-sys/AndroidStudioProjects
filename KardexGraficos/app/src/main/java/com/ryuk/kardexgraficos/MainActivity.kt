package com.ryuk.kardexgraficos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity // 🎯 Nuevo
import androidx.activity.compose.setContent // 🎯 Nuevo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.ryuk.kardexgraficos.ui.theme.KardexGraficosTheme
// ^ Ojo: Este import de 'ui.theme' dará error de "Unresolved reference"
// hasta que creemos la carpeta y los archivos del tema.

// ==================================================================
// 1. DATA CLASSES (Modelos de datos para el Kardex)
// ==================================================================
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
// ==================================================================


class MainActivity : ComponentActivity() { // 🎯 CAMBIO: Hereda de ComponentActivity

    // 🎯 ESTADOS DE COMPOSE: Observables para la UI
    private var notasDocentes by mutableStateOf<Map<String, List<Registro>>>(emptyMap())
    private var infoCarga by mutableStateOf("Esperando carga de archivos...")

    // Almacenamiento temporal para la carga de archivos
    private val materias = mutableListOf<Materia>()
    private val periodos = mutableListOf<Periodo>()
    private val docentes = mutableListOf<Docente>()
    private val estudiantes = mutableListOf<Estudiante>()
    private val kardex = mutableListOf<Kardex>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent { // 🎯 CAMBIO: Inicializa la UI con Compose
            KardexGraficosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KardexScreen( // 💥 ESTO DARÁ ERROR POR AHORA 💥
                        infoCarga = infoCarga,
                        notasDocentes = notasDocentes,
                        onCargarArchivos = { seleccionarArchivos() },
                        mostrarToast = { mensaje ->
                            Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------
    // LÓGICA DE CARGA DE ARCHIVOS
    // ----------------------------------------------------------------------------------

    private val selector = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            materias.clear(); periodos.clear(); docentes.clear(); estudiantes.clear(); kardex.clear()

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
            val mensaje = "Archivos cargados: ${docentes.size} docentes. ${kardex.size} registros."
            infoCarga = mensaje
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
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
            infoCarga = "Error al leer ${obtenerNombre(uri)}: ${e.message}"
            Toast.makeText(this, infoCarga, Toast.LENGTH_LONG).show()
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
                        Estudiante(partes[0].trim(), partes[1].trim(), partes[2].trim(), partes[3].trim())
                    )
                } else if (archivo.contains("docentes", ignoreCase = true)){
                    docentes.add(
                        Docente(partes[0].trim(), partes[1].trim(), partes[2].trim(), partes[3].trim())
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

        val registros = kardex.mapNotNull { k ->
            val doc = mapDocentes[k.carnetDoc]
            val mat = mapMaterias[k.siglaMateria]
            val per = mapPeriodos[k.codigoPeriodo]

            if (doc != null && mat != null && per != null) {
                Registro(k.carnetEst, mat, doc, per, k.nota, k.obs)
            } else {
                null
            }
        }

        notasDocentes = registros.groupBy { it.docente.carnet }
    }
}