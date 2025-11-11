package com.ryuk.horarioumsa.utils

import com.ryuk.horarioumsa.models.Materia

class InscripcionManager {
    private val materiasInscritas = mutableListOf<Materia>()

    fun agregarMateria(materia: Materia): Result {
        if (yaEstaInscrita(materia.sigla)) {
            return Result(false, "Ya estás inscrito en ${materia.sigla}")
        }
        val choque = verificarChoques(materia)
        if (choque != null) {
            return Result(false, "Choque de horario con ${choque.sigla}")
        }
        if (materiasInscritas.size >= 8) {
            return Result(false, "Máximo 8 materias por semestre")
        }
        materiasInscritas.add(materia)
        return Result(true, "Materia agregada exitosamente")
    }

    fun quitarMateria(sigla: String): Boolean {
        return materiasInscritas.removeIf { it.sigla == sigla }
    }

    fun getMateriasInscritas(): List<Materia> = materiasInscritas.toList()

    private fun yaEstaInscrita(sigla: String): Boolean {
        return materiasInscritas.any { it.sigla == sigla }
    }

    private fun verificarChoques(nuevaMateria: Materia): Materia? {
        return materiasInscritas.find { hayChoqueHorario(it, nuevaMateria) }
    }

    private fun hayChoqueHorario(m1: Materia, m2: Materia): Boolean {
        val diasComunes = m1.dias.intersect(m2.dias.toSet())
        if (diasComunes.isEmpty()) return false
        val inicio1 = convertirHoraAMinutos(m1.horaInicio)
        val fin1 = convertirHoraAMinutos(m1.horaFin)
        val inicio2 = convertirHoraAMinutos(m2.horaInicio)
        val fin2 = convertirHoraAMinutos(m2.horaFin)
        return !(fin1 <= inicio2 || fin2 <= inicio1)
    }

    private fun convertirHoraAMinutos(hora: String): Int {
        val partes = hora.split(":")
        return partes[0].toInt() * 60 + partes[1].toInt()
    }

    data class Result(val exitoso: Boolean, val mensaje: String)
}
