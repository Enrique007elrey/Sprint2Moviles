package com.example.conversor;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // 1. Declaración de variables (componentes del layout)
    private EditText etValor;
    private Spinner spOrigen;
    private Spinner spDestino;
    private Button btnConvertir;
    private TextView tvResultado;

    // Arrays paralelos: misma posición = misma unidad
    // La posición 0 está vacía para que los spinners empiecen sin nada elegido
    private final String[] unidades = {"", "km", "m", "cm"};
    private final double[] metros   = {0, 1000, 1, 0.01};   // cuántos metros vale cada unidad

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Data binding: enlazar las variables con los elementos del layout
        etValor      = findViewById(R.id.etValor);
        spOrigen     = findViewById(R.id.spOrigen);
        spDestino    = findViewById(R.id.spDestino);
        btnConvertir = findViewById(R.id.btnConvertir);
        tvResultado  = findViewById(R.id.tvResultado);

        // 3. Rellenar los spinners con la lista de unidades
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, unidades);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrigen.setAdapter(adaptador);
        spDestino.setAdapter(adaptador);

        // 4. Al pulsar el botón, se ejecuta convertir()
        btnConvertir.setOnClickListener(v -> convertir());
    }

    private void convertir() {
        int posOrigen  = spOrigen.getSelectedItemPosition();
        int posDestino = spDestino.getSelectedItemPosition();
        String texto   = etValor.getText().toString().trim();

        // Control: los desplegables no pueden estar vacíos
        if (posOrigen == 0 || posDestino == 0) {
            mostrarError("Selecciona la unidad de origen y la de destino");
            return;
        }

        // Control: el campo del valor no puede estar vacío
        if (texto.isEmpty()) {
            mostrarError("Introduce un valor para convertir");
            return;
        }

        double valor;
        try {
            valor = Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            // Por si el usuario escribe solo "." o algo no válido
            mostrarError("El valor introducido no es válido");
            return;
        }

        // Control: el valor no puede ser negativo
        if (valor < 0) {
            mostrarError("El valor no puede ser negativo");
            return;
        }

        // Cálculo sin switch: se pasa a metros y de metros a la unidad destino
        double resultado = (metros[posOrigen] / metros[posDestino]) * valor;

        // Resultado con solo dos decimales
        tvResultado.setText(String.format("%.2f %s", resultado, unidades[posDestino]));
    }

    private void mostrarError(String mensaje) {
        tvResultado.setText("");
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}
