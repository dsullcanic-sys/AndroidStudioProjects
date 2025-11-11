package com.ryuk.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
class HorarioViewModel : ViewModel() {
    private val _materiasDisponibles = MutableLiveData<List<Materia>>()
    val materiasDisponibles: LiveData<List<Materia>> = _materiasDisponibles

    private val _materiasInscritas = MutableLiveData<List<Materia>>()
    val materiasInscritas: LiveData<List<Materia>> = _materiasInscritas

    private val _errores = MutableLiveData<List<String>>()
    val errores: LiveData<List<String>> = _errores

    init {
        cargarMateriasDisponibles()
        _materiasInscritas.value = emptyList()
    }

    private fun cargarMateriasDisponibles() {
        val materias = listOf(
            Materia("INF 282", "Especificaciones Formales y Verificación", "Juan Gonzalo Contreras Candia", "A", listOf("Lunes", "Miércoles"), "10:00", "12:00", "P4-A2"),
            Materia("INF 391", "Simulación de Sistemas", "Rosa Flores Morales", "A", listOf("Lunes", "Miércoles"), "10:00", "12:00", "P4-A3"),
            Materia("INF 391", "Simulación de Sistemas", "Grover Alex Rodriguez Ramirez", "B", listOf("Martes", "Jueves"), "10:00", "12:00", "P1-A1"),
            Materia("INF 314", "Auditoría Informática", "Miguel Cotaña Mier", "A", listOf("Martes", "Jueves"), "12:00", "14:00", "P4-A1"),
            Materia("INF 317", "Sistemas en Tiempo Real y Distribuidos", "Moises Martin Silva Choque", "A", listOf("Lunes", "Miércoles"), "16:00", "18:00", "P4-Lab1"),
            Materia("INF 323", "Programación Gráfica", "Jhonny Roberto Felipez Andrade", "A", listOf("Martes", "Jueves"), "08:00", "10:00", "P4-Lab1"),
            Materia("INF 324", "Programación Multimedial", "Moises Martin Silva Choque", "A", listOf("Lunes", "Miércoles"), "14:00", "16:00", "P4-Lab1"),
            Materia("INF 325", "Programación Virtual", "Reynaldo Javier Zeballos Daza", "A", listOf("Lunes", "Miércoles"), "08:00", "10:00", "LASIN"),
            Materia("INF 329", "Idiomas II", "Virginia Nina Machaca", "A", listOf("Lunes", "Miércoles"), "10:00", "12:00", "P5-A5"),
            Materia("INF 329", "Idiomas II", "Fernando Nelzon Espinoza Centellas", "B", listOf("Lunes", "Miércoles"), "08:00", "10:00", "P5-A3"),
            Materia("INF 351", "Sistemas Expertos", "Luisa Velasquez Lopez", "A", listOf("Lunes", "Miércoles"), "14:00", "16:00", "P1-A3")
        )
        _materiasDisponibles.value = materias
    }

    fun agregarMateria(materia: Materia): Boolean {
        val currentList = _materiasInscritas.value ?: emptyList()

        // Validar duplicados
        if (currentList.any { it.sigla == materia.sigla }) {
            _errores.value = listOf("Ya estás inscrito en ${materia.sigla}")
            return false
        }

        // Validar máximo de materias
        if (currentList.size >= 8) {
            _errores.value = listOf("Límite de 8 materias alcanzado")
            return false
        }

        // Validar choques de horario
        val choques = currentList.filter { it.chocaCon(materia) }
        if (choques.isNotEmpty()) {
            val nombresChoques = choques.joinToString { it.sigla }
            _errores.value = listOf("Choque de horario con: $nombresChoques")
            return false
        }

        // Agregar materia
        val nuevaLista = currentList.toMutableList()
        nuevaLista.add(materia)
        _materiasInscritas.value = nuevaLista
        _errores.value = emptyList()
        return true
    }

    fun quitarMateria(sigla: String) {
        val currentList = _materiasInscritas.value ?: emptyList()
        _materiasInscritas.value = currentList.filter { it.sigla != sigla }
    }

    fun generarHorarioSemanal(): Map<String, List<Materia>> {
        val horario = mutableMapOf<String, MutableList<Materia>>()
        val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")

        dias.forEach { dia ->
            horario[dia] = mutableListOf()
        }

        _materiasInscritas.value?.forEach { materia ->
            materia.dias.forEach { dia ->
                horario[dia]?.add(materia)
            }
        }

        // Ordenar materias por hora de inicio
        horario.forEach { (_, materias) ->
            materias.sortBy { it.horaAMinutos(it.horaInicio) }
        }

        return horario
    }
}