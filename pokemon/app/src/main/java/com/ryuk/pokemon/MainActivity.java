package com.ryuk.pokemon;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textViewNombre, textViewImagen;

    // Nombres de las imágenes (sin extensión)
    private final String[] imagenes = {
            "bulbasaur",
            "charmander",
            "clefairy",
            "diglett",
            "farfetch",
            "gengar",
            "jigglypuff",
            "kakuna",
            "lickitung",
            "meowth",
            "metapod",
            "oddish",
            "psyduck",
            "rattata",
            "sandshrew",
            "slowpoke",
            "squirtle",
            "vulpix"
    };

    private int indice = 0;
    private boolean mostrandoBack = false; // indica si se está mostrando la imagen "back"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView2);
        textViewNombre = findViewById(R.id.textView4);
        textViewImagen = findViewById(R.id.textView5);

        textViewImagen.setTextSize(28); // nombre del Pokémon más grande

        // Ajuste de padding para barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mostrarImagen();
    }

    // Botón izquierda
    public void izquierda(android.view.View view) {
        mostrandoBack = false;
        indice--;
        if (indice < 0) indice = imagenes.length - 1;
        mostrarImagen();
    }

    // Botón derecha
    public void derecha(android.view.View view) {
        mostrandoBack = false;
        indice++;
        if (indice >= imagenes.length) indice = 0;
        mostrarImagen();
    }

    // Botón SHOW: alterna entre back y Pokémon actual
    public void mostrar(android.view.View view) {
        if (mostrandoBack) {
            // Volver al Pokémon actual
            mostrandoBack = false;
            mostrarImagen();
        } else {
            // Mostrar imagen "back"
            mostrandoBack = true;
            int resID = getResources().getIdentifier("back", "drawable", getPackageName());
            imageView.setImageResource(resID);
            textViewImagen.setText(""); // opcional: ocultar nombre mientras se ve back
        }
    }

    // Mostrar imagen actual del Pokémon
    private void mostrarImagen() {
        if (!mostrandoBack) {
            int resID = getResources().getIdentifier(imagenes[indice], "drawable", getPackageName());
            imageView.setImageResource(resID);
            textViewImagen.setText(formatearNombre(imagenes[indice]));
        }
    }

    // Convertir nombre del archivo a un formato legible
    private String formatearNombre(String nombreArchivo) {
        String[] partes = nombreArchivo.split("_");
        StringBuilder sb = new StringBuilder();
        for (String parte : partes) {
            sb.append(parte.substring(0,1).toUpperCase())
                    .append(parte.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }
}
