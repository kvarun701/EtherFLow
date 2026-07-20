// AndroidJavaExample.java
// EtherFlow Android Client — Java (AppCompatActivity + ExecutorService)
//
// Full example showing how to use etherflow-client on Android in Java
// using thread-pool executors and runOnUiThread() for UI updates.
//
// Dependency (build.gradle):
//   implementation 'io.github.kvarun701:etherflow-client:0.1.1'

package io.etherflow.android.example;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.etherflow.client.HttpClient;
import io.etherflow.client.ParameterizedTypeReference;
import io.etherflow.client.python.FlaskApiClient;
import io.etherflow.client.python.FastApiClient;
import io.etherflow.client.python.PythonApiClient;
import io.etherflow.core.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class AndroidJavaExample extends AppCompatActivity {

    // ─────────────────────────────────────────────────────────────────────────
    // Data model (Java record — requires Java 16+ / Android Studio Bumblebee+)
    // ─────────────────────────────────────────────────────────────────────────

    record User(String id, String name, String email) {}
    record Post(Integer id, String title, String body, Integer userId) {}

    // ─────────────────────────────────────────────────────────────────────────
    // EtherFlow clients
    // ─────────────────────────────────────────────────────────────────────────

    // Reactive HttpClient — same API on Android as server-side Java/Kotlin
    private final HttpClient client = HttpClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com")
            .retry(3)
            .cache(Duration.ofMinutes(5), 100)
            .build();

    // PythonApiClient — call Flask & FastAPI backends from Android
    private final PythonApiClient pythonClient = PythonApiClient.builder()
            .flaskUrl("http://10.0.2.2:5001")    // Android emulator → host machine
            .fastApiUrl("http://10.0.2.2:5002")
            .build();

    // Dedicated Flask-only client
    private final FlaskApiClient flaskClient =
            FlaskApiClient.create("http://10.0.2.2:5001");

    // Dedicated FastAPI-only client
    private final FastApiClient fastApiClient =
            FastApiClient.create("http://10.0.2.2:5002");

    // ─────────────────────────────────────────────────────────────────────────
    // Views (bind to actual layout IDs in your project)
    // ─────────────────────────────────────────────────────────────────────────

    private TextView    nameText;
    private TextView    emailText;
    private TextView    statusText;
    private ProgressBar progressBar;
    private Button      loadButton;
    private Button      flaskButton;
    private Button      fastApiButton;

    // ─────────────────────────────────────────────────────────────────────────
    // Activity lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_main);

        // Bind views
        // nameText      = findViewById(R.id.nameText);
        // emailText     = findViewById(R.id.emailText);
        // statusText    = findViewById(R.id.statusText);
        // progressBar   = findViewById(R.id.progressBar);
        // loadButton    = findViewById(R.id.loadButton);
        // flaskButton   = findViewById(R.id.flaskButton);
        // fastApiButton = findViewById(R.id.fastApiButton);

        // Button clicks
        if (loadButton != null) {
            loadButton.setOnClickListener(v -> loadUser("1"));
        }
        if (flaskButton != null) {
            flaskButton.setOnClickListener(v -> callFlask("AndroidJavaUser"));
        }
        if (fastApiButton != null) {
            fastApiButton.setOnClickListener(v -> callFastApi(42));
        }

        // Auto-load on launch
        loadUser("1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EtherFlow API calls
    // ─────────────────────────────────────────────────────────────────────────

    /** GET /users/{id} — returns Mono<User>, subscribe is non-blocking. */
    private void loadUser(String userId) {
        showLoading(true);

        // GET single user — bodyTo(User.class)
        Mono<User> userMono = client.get()
                .uri("/users/{id}", userId)
                .retrieve()
                .bodyTo(User.class);

        userMono.subscribe(
                user -> runOnUiThread(() -> {
                    showLoading(false);
                    if (nameText != null)  nameText.setText(user.name());
                    if (emailText != null) emailText.setText(user.email());
                }),
                error -> runOnUiThread(() -> {
                    showLoading(false);
                    showError(error.getMessage());
                })
        );
    }

    /** GET /users — returns Mono<List<User>> using ParameterizedTypeReference. */
    private void loadAllUsers() {
        Mono<List<User>> usersMono = client.get()
                .uri("/users")
                .retrieve()
                .bodyTo(new ParameterizedTypeReference<List<User>>() {});

        usersMono.subscribe(
                users -> runOnUiThread(() -> {
                    if (statusText != null) {
                        statusText.setText("Loaded " + users.size() + " users");
                    }
                }),
                error -> runOnUiThread(() -> showError(error.getMessage()))
        );
    }

    /** POST /users — create a new user. */
    private void createUser(String name, String email) {
        User newUser = new User(null, name, email);

        Mono<User> createMono = client.post()
                .uri("/users")
                .body(newUser)
                .retrieve()
                .bodyTo(User.class);

        createMono.subscribe(
                created -> runOnUiThread(() -> {
                    Toast.makeText(this, "Created: " + created.name(), Toast.LENGTH_SHORT).show();
                }),
                error -> runOnUiThread(() -> showError(error.getMessage()))
        );
    }

    /** Call Flask Python backend from Android using PythonApiClient. */
    private void callFlask(String name) {
        Mono<Map> flaskMono = pythonClient.flask()
                .get("/api/flask/hello?name=" + name, Map.class);

        flaskMono.subscribe(
                result -> runOnUiThread(() -> {
                    if (statusText != null) {
                        statusText.setText("Flask: " + result.get("greeting"));
                    }
                }),
                error -> runOnUiThread(() -> showError("Flask error: " + error.getMessage()))
        );
    }

    /** Call FastAPI Python backend from Android using PythonApiClient. */
    private void callFastApi(int itemId) {
        Mono<Map> fastApiMono = pythonClient.fastApi()
                .get("/api/fastapi/items/" + itemId, Map.class);

        fastApiMono.subscribe(
                result -> runOnUiThread(() -> {
                    if (statusText != null) {
                        statusText.setText("FastAPI: " + result.toString());
                    }
                }),
                error -> runOnUiThread(() -> showError("FastAPI error: " + error.getMessage()))
        );
    }

    /** Aggregate health check for both Python backends. */
    private void checkPythonHealth() {
        pythonClient.checkHealth().subscribe(
                health -> runOnUiThread(() -> {
                    String overall = (String) health.getOrDefault("overallStatus", "UNKNOWN");
                    Toast.makeText(this, "Python services: " + overall, Toast.LENGTH_SHORT).show();
                }),
                error -> runOnUiThread(() -> showError(error.getMessage()))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message != null ? message : "Unknown error", Toast.LENGTH_LONG).show();
    }
}
