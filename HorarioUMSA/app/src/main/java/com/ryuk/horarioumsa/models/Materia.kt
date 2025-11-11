package com.ryuk.horarioumsa.models

data class Materia(
    val sigla: String,
    val nombre: String,
    val docente: String,
    val paralelo: String,
    val dias: List<String>,
    val horaInicio: String,
    val horaFin: String,
    val aula: String
) : java.io.Serializable   // IMPORTANTE para pasarlo entre activities
