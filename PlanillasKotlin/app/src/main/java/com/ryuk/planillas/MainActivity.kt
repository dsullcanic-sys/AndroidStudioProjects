package com.ryuk.planillas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var etCarnet: EditText
    private lateinit var btnBuscar: Button
    private lateinit var btnCargar: Button
    private lateinit var containerResultados: LinearLayout

    private val personal = HashMap<String, String>()
    private val cargos = HashMap<String, Pair<String, Int>>()
    private val planillas = HashMap<String, HashMap<String, String>>()
    private val bonos = HashMap<String, HashMap<String, ArrayList<Int>>>()
    private val descuentos = HashMap<String, HashMap<String, ArrayList<Int>>>()

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val data = res.data
                data?.clipData?.let { clip ->
                    for (i in 0 until clip.itemCount) leerArchivo(clip.getItemAt(i).uri)
                } ?: data?.data?.let { leerArchivo(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etCarnet = findViewById(R.id.etCarnet)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnCargar = findViewById(R.id.btnCargar)
        containerResultados = findViewById(R.id.containerResultados)

        btnCargar.setOnClickListener { elegirArchivos() }
        btnBuscar.setOnClickListener { mostrarCarnet(etCarnet.text.toString().trim()) }
    }

    private fun elegirArchivos() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        filePicker.launch(intent)
    }

    private fun leerArchivo(uri: Uri) {
        var nombre: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) nombre = cursor.getString(idx)
            }
        }
        if (nombre == null) nombre = uri.path?.split("/")?.lastOrNull()
        if (nombre == null) return

        val name = nombre.lowercase(Locale.getDefault())
        val input = contentResolver.openInputStream(uri) ?: return
        input.bufferedReader().use { br ->
            var first = true
            br.forEachLine { line ->
                if (first) { first = false; return@forEachLine }
                if (line.trim().isEmpty()) return@forEachLine
                val p = line.split(";")
                try {
                    when {
                        name.contains("personal") -> {
                            if (p.size >= 4) {
                                val carnet = p[0].trim()
                                personal[carnet] = "${p[3].trim()} ${p[1].trim()} ${p[2].trim()}"
                            }
                        }
                        name.contains("cargos") -> {
                            if (p.size >= 3) {
                                val id = p[0].trim()
                                val desc = p[1].trim()
                                val bas = p[2].trim().toIntOrNull() ?: 0
                                cargos[id] = Pair(desc, bas)
                            }
                        }
                        name.startsWith("pla") && name.length >= 7 -> {
                            val mes = name.substring(3, 7)
                            val map = planillas.getOrPut(mes) { HashMap() }
                            if (p.size >= 3) map[p[1].trim()] = p[2].trim()
                        }
                        name.startsWith("bon") && name.length >= 7 -> {
                            val mes = name.substring(3, 7)
                            val map = bonos.getOrPut(mes) { HashMap() }
                            if (p.size >= 3) map.getOrPut(p[1].trim()) { ArrayList() }.add(p[2].trim().toIntOrNull() ?: 0)
                        }
                        name.startsWith("des") && name.length >= 7 -> {
                            val mes = name.substring(3, 7)
                            val map = descuentos.getOrPut(mes) { HashMap() }
                            if (p.size >= 3) map.getOrPut(p[1].trim()) { ArrayList() }.add(p[2].trim().toIntOrNull() ?: 0)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun mostrarCarnet(carnet: String) {
        containerResultados.removeAllViews()
        if (carnet.isEmpty()) return

        val nombre = personal[carnet] ?: "Desconocido"
        val t = TextView(this)
        t.text = "Nombre: \n $nombre"
        t.textSize = 18f
        containerResultados.addView(t)

        val meses = planillas.keys.sorted()
        for (mes in meses) {
            val cargoId = planillas[mes]?.get(carnet) ?: continue
            val cargoPair = cargos[cargoId]
            val cargoName = cargoPair?.first ?: "Sin cargo"
            val basico = cargoPair?.second ?: 0
            val bList = bonos[mes]?.get(carnet) ?: arrayListOf()
            val dList = descuentos[mes]?.get(carnet) ?: arrayListOf()
            val totalBon = bList.sum()
            val totalDes = dList.sum()
            val neto = basico + totalBon - totalDes

            val tvMes = TextView(this)
            tvMes.text = formatearMes(mes)
            tvMes.textSize = 18f
            tvMes.setPadding(12, 12, 12, 12)
            tvMes.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            containerResultados.addView(tvMes)

            val detalle = TextView(this)
            detalle.text = """
                Cargo: $cargoName
                Básico: $basico
                Bonos: ${bList.size} ($totalBon)
                Descuentos: ${dList.size} ($totalDes)
                Neto: $neto
            """.trimIndent()
            detalle.textSize = 16f
            detalle.setPadding(24, 12, 12, 12)
            detalle.visibility = TextView.GONE
            containerResultados.addView(detalle)

            tvMes.setOnClickListener {
                detalle.visibility = if (detalle.visibility == TextView.VISIBLE) TextView.GONE else TextView.VISIBLE
            }
        }
    }

    private fun formatearMes(mes: String): String {
        if (mes.length != 4) return mes
        val m = mes.substring(0, 2).toIntOrNull() ?: return mes
        val y = "20${mes.substring(2)}"
        val nombres = listOf(
            "Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
        )
        return if (m in 1..12) "${nombres[m - 1]} $y" else mes
    }
}
