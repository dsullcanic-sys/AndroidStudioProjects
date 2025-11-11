package com.ryuk.horarioumsa

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ryuk.horarioumsa.models.Materia
import com.ryuk.horarioumsa.utils.InscripcionManager

class HorarioActivity : AppCompatActivity() {

    private lateinit var tableHorario: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horario)

        tableHorario = findViewById(R.id.tableHorario)

        // Recibir materias desde MainActivity
        val materias = intent.getSerializableExtra("materias") as? List<Materia> ?: emptyList()

        generarHorario(materias)
    }

    private fun generarHorario(materias: List<Materia>) {
        // Rangos de horas para mostrar (08:00 a 20:00)
        val horas = listOf("08:00-10:00","10:00-12:00","12:00-14:00",
            "14:00-16:00","16:00-18:00","18:00-20:00")

        for (hora in horas) {
            val row = TableRow(this)
            row.addView(crearCelda(hora, bold = true))

            val dias = listOf("Lunes","Martes","Miércoles","Jueves","Viernes")
            for (dia in dias) {
                val materia = materias.find { it.dias.contains(dia) && "${it.horaInicio}-${it.horaFin}" == hora }
                if (materia != null) {
                    val color = getColorForMateria(materia.sigla)
                    row.addView(crearCelda("${materia.sigla}\n${materia.aula}", color = color))
                } else {
                    row.addView(crearCelda(""))
                }
            }

            tableHorario.addView(row)
        }
    }

    private fun crearCelda(texto: String, bold: Boolean = false, color: Int? = null): TextView {
        val tv = TextView(this)
        tv.text = texto
        tv.setTextColor(Color.BLACK) // que se lea sobre cualquier fondo
        tv.setPadding(8, 8, 8, 8)
        tv.gravity = Gravity.CENTER
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD)
        if (color != null) tv.setBackgroundColor(color)
        return tv
    }

    private fun getColorForMateria(sigla: String): Int {
        val colores = listOf(
            Color.parseColor("#FFCDD2"), // rojo claro
            Color.parseColor("#C8E6C9"), // verde claro
            Color.parseColor("#BBDEFB"), // azul claro
            Color.parseColor("#FFF9C4"), // amarillo claro
            Color.parseColor("#D1C4E9"), // violeta claro
            Color.parseColor("#FFECB3"), // naranja claro
            Color.parseColor("#B2DFDB"), // turquesa claro
            Color.parseColor("#F8BBD0")  // rosado claro
        )
        val index = (sigla.hashCode() and 0x7FFFFFFF) % colores.size
        return colores[index]
    }

}
