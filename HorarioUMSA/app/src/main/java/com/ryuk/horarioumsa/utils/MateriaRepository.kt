package com.ryuk.horarioumsa.utils

import com.ryuk.horarioumsa.models.Materia

object MateriaRepository {
    val materiasDisponibles = listOf(
        Materia("INF 282", "Especificaciones Formales", "Juan Contreras", "A", listOf("Lunes", "Miércoles"), "10:00", "12:00", "P4-A2"),
        Materia("INF 391", "Simulación de Sistemas", "Grover Rodriguez", "B", listOf("Martes", "Jueves"), "10:00", "12:00", "P1-A1"),
        Materia("INF 323", "Programación Gráfica", "Jhonny Felipez", "A", listOf("Martes", "Jueves"), "08:00", "10:00", "P4-Lab1"),
        Materia("INF 324", "Programación Multimedial", "Moises Silva", "A", listOf("Lunes", "Miércoles"), "14:00", "16:00", "P4-Lab1"),
        Materia("INF 325", "Programación Virtual", "Reynaldo Zeballos", "A", listOf("Lunes", "Miércoles"), "08:00", "10:00", "LASIN"),
        Materia("INF 317", "Sistemas en Tiempo Real", "Moises Silva", "A", listOf("Lunes", "Miércoles"), "16:00", "18:00", "P4-Lab1")
    )
}