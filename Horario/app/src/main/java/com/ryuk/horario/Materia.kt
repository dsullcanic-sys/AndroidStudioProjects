package com.ryuk.horario

data class Materia(
    val sigla: String,
    val nombre: String,
    val docente: String,
    val paralelo: String,
    val dias: List<String>,
    val horaInicio: String,
    val horaFin: String,
    val aula: String
) {
    // Función para convertir hora en formato HH:MM a minutos desde medianoche
    fun horaAMinutos(hora: String): Int {
        val partes = hora.split(":")
        return partes[0].toInt() * 60 + partes[1].toInt()
    }

    // Función para verificar si esta materia choca con otra
    fun chocaCon(otra: Materia): Boolean {
        for (dia in dias) {
            if (dia in otra.dias) {
                val inicio1 = horaAMinutos(horaInicio)
                val fin1 = horaAMinutos(horaFin)
                val inicio2 = horaAMinutos(otra.horaInicio)
                val fin2 = horaAMinutos(otra.horaFin)

                // Verificar si los intervalos se superponen
                if (inicio1 < fin2 && inicio2 < fin1) {
                    return true
                }
            }
        }
        return false
    }
}