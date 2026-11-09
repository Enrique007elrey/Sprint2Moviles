package com.example.conversor;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    // 1. Declaración de variables (componentes del layout)
    private Spinner spMagnitud;
    private EditText etValor;
    private Spinner spOrigen;
    private Spinner spDestino;
    private Button btnConvertir;
    private TextView tvResultado;

    // Posición de "Temperatura" en el array de magnitudes (es la única que admite negativos)
    private static final int TEMPERATURA = 7;

    // Magnitudes. La posición 0 está vacía para que el spinner empiece sin nada elegido
    private final String[] magnitudes = {
            "", "Longitud", "Masa", "Volumen", "Datos", "Tiempo", "Potencia", "Temperatura", "Velocidad"
    };

    // Unidades de cada magnitud. unidades[magnitud][unidad]
    // En cada fila, la posición 0 también está vacía
    private final String[][] unidades = {
            {""},                                       // (sin magnitud)
            {"", "km", "m", "cm", "mm"},                // Longitud
            {"", "t", "kg", "g", "mg"},                 // Masa
            {"", "l", "dl", "cl", "ml"},                // Volumen
            {"", "TB", "GB", "MB", "KB"},               // Datos
            {"", "días", "h", "min", "s"},              // Tiempo
            {"", "MW", "kW", "W", "CV"},                // Potencia
            {"", "°C", "°F", "K"},                      // Temperatura
            {"", "km/h", "m/s", "mph", "nudos"}         // Velocidad
    };

    // Cuánto vale cada unidad en la unidad base de su magnitud
    // (misma posición que en el array de unidades)
    private final double[][] factores = {
            {0},
            {0, 1000, 1, 0.01, 0.001},                          // base: metro
            {0, 1000, 1, 0.001, 0.000001},                      // base: kilo
            {0, 1, 0.1, 0.01, 0.001},                           // base: litro
            {0, 1024, 1, 1.0 / 1024, 1.0 / (1024 * 1024)},      // base: GB (1 GB = 1024 MB)
            {0, 86400, 3600, 60, 1},                            // base: segundo
            {0, 1000000, 1000, 1, 735.5},                       // base: vatio (1 CV = 735,5 W)
            {0, 1, 5.0 / 9, 1},                                 // base: Celsius
            {0, 1 / 3.6, 1, 0.44704, 0.514444}                  // base: m/s
    };

    // Desplazamiento (offset) de cada unidad. Solo la temperatura lo necesita,
    // en el resto es 0 y la fórmula se queda en una simple multiplicación
    private final double[][] offsets = {
            {0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 32, 273.15},                                 // °C, °F, K
            {0, 0, 0, 0, 0}
    };

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
        spMagnitud   = findViewById(R.id.spMagnitud);
        etValor      = findViewById(R.id.etValor);
        spOrigen     = findViewById(R.id.spOrigen);
        spDestino    = findViewById(R.id.spDestino);
        btnConvertir = findViewById(R.id.btnConvertir);
        tvResultado  = findViewById(R.id.tvResultado);

        // 3. Rellenar el spinner de magnitudes
        spMagnitud.setAdapter(crearAdaptador(magnitudes));

        // Al empezar, origen y destino solo tienen la opción vacía
        cargarUnidades(0);

        // Cuando se elige una magnitud, se cargan sus unidades en origen y destino
        spMagnitud.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cargarUnidades(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 4. Al pulsar el botón, se ejecuta convertir()
        btnConvertir.setOnClickListener(v -> convertir());
    }

    private ArrayAdapter<String> crearAdaptador(String[] lista) {
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, lista);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adaptador;
    }

    private void cargarUnidades(int posMagnitud) {
        spOrigen.setAdapter(crearAdaptador(unidades[posMagnitud]));
        spDestino.setAdapter(crearAdaptador(unidades[posMagnitud]));
        tvResultado.setText("");
    }

    private void convertir() {
        int m          = spMagnitud.getSelectedItemPosition();
        int posOrigen  = spOrigen.getSelectedItemPosition();
        int posDestino = spDestino.getSelectedItemPosition();
        String texto   = etValor.getText().toString().trim();

        // Control: hay que elegir magnitud
        if (m == 0) {
            mostrarError("Selecciona una magnitud");
            return;
        }

        // Control: los desplegables de unidades no pueden estar vacíos
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
            mostrarError("El valor introducido no es válido");
            return;
        }

        // Control: no se permiten negativos, excepto en temperatura (-5 °C es válido)
        if (valor < 0 && m != TEMPERATURA) {
            mostrarError("El valor no puede ser negativo");
            return;
        }

        // Cálculo sin switch, la misma fórmula para todas las magnitudes:
        // 1) de la unidad origen a la unidad base
        double base = (valor - offsets[m][posOrigen]) * factores[m][posOrigen];
        // 2) de la unidad base a la unidad destino
        double resultado = base / factores[m][posDestino] + offsets[m][posDestino];

        // Resultado con solo dos decimales
        tvResultado.setText(String.format("%.2f %s", resultado, unidades[m][posDestino]));
    }

    private void mostrarError(String mensaje) {
        tvResultado.setText("");
        Snackbar.make(findViewById(R.id.main), mensaje, Snackbar.LENGTH_LONG).show();
    }
}
