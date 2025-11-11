package com.ryuk.comida;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Platos extends AppCompatActivity {

    private ImageView imageViewPlato;
    private TextView textViewNombrePlato;
    private Button btnAnteriorPlatos, btnSiguientePlatos, btnVolverPlatos;

    private int[] imagenesPlatos = {
            R.drawable.hamburguesa,
            R.drawable.pizza,
            R.drawable.pollo_broaster,
            R.drawable.salchipapa
    };

    private String[] nombresPlatos = {
            "Hamburguesa Premium",
            "Pizza Familiar",
            "Pollo Broaster",
            "Salchipapa Especial"
    };

    private int indiceActual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_platos);

        inicializarVistas();
        mostrarPlatoActual();
        configurarBotones();
    }

    private void inicializarVistas() {
        imageViewPlato = findViewById(R.id.imagenPlatos);
        textViewNombrePlato = findViewById(R.id.descripcionPlatos);
        btnAnteriorPlatos = findViewById(R.id.btnAnteriorPlatos);
        btnSiguientePlatos = findViewById(R.id.btnSiguientePlatos);
        btnVolverPlatos = findViewById(R.id.btnVolverPlatos);
    }

    private void mostrarPlatoActual() {
        imageViewPlato.setImageResource(imagenesPlatos[indiceActual]);
        textViewNombrePlato.setText(nombresPlatos[indiceActual]);
    }

    private void configurarBotones() {
        btnAnteriorPlatos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (indiceActual > 0) {
                    indiceActual--;
                } else {
                    indiceActual = imagenesPlatos.length - 1;
                }
                mostrarPlatoActual();
            }
        });

        btnSiguientePlatos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (indiceActual < imagenesPlatos.length - 1) {
                    indiceActual++;
                } else {
                    indiceActual = 0;
                }
                mostrarPlatoActual();
            }
        });

        btnVolverPlatos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Platos.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}