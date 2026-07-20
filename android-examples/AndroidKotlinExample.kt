// AndroidKotlinExample.kt
// EtherFlow Android Client — Kotlin (ViewModel + StateFlow)
//
// Full example showing how to use etherflow-client on Android in Kotlin
// using ViewModel, StateFlow, and coroutine-friendly Mono.subscribe().
//
// Dependency (build.gradle.kts):
//   implementation("io.github.kvarun701:etherflow-client:0.1.1")

package io.etherflow.android.example

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import io.etherflow.client.HttpClient
import io.etherflow.client.python.PythonApiClient
import io.etherflow.core.Mono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration

// ─────────────────────────────────────────────────────────────────────────────
// Data Models
// ─────────────────────────────────────────────────────────────────────────────

data class User(
    val id: String?,
    val name: String,
    val email: String
)

data class Post(
    val id: Int?,
    val title: String,
    val body: String,
    val userId: Int
)

// ─────────────────────────────────────────────────────────────────────────────
// Repository
// ─────────────────────────────────────────────────────────────────────────────

class UserRepository {

    // EtherFlow HttpClient — same API on Android as on server-side Java/Kotlin
    private val client = HttpClient.builder()
        .baseUrl("https://jsonplaceholder.typicode.com")
        .retry(3)
        .cache(Duration.ofMinutes(5), 100)
        .build()

    // PythonApiClient — call Flask (5001) and FastAPI (5002) from Android
    private val pythonClient = PythonApiClient.builder()
        .flaskUrl("http://10.0.2.2:5001")     // Android emulator → host machine
        .fastApiUrl("http://10.0.2.2:5002")
        .build()

    /** GET a single user — returns Mono<User>. */
    fun getUser(id: String): Mono<User> = client.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyTo(User::class.java)

    /** GET all users — returns Mono<List<User>>. */
    fun getUsers(): Mono<List<User>> = client.get()
        .uri("/users")
        .retrieve()
        .bodyTo(object : io.etherflow.client.ParameterizedTypeReference<List<User>>() {})

    /** POST a new user. */
    fun createUser(user: User): Mono<User> = client.post()
        .uri("/users")
        .body(user)
        .retrieve()
        .bodyTo(User::class.java)

    /** Call Flask backend from Android. */
    fun callFlaskHello(name: String): Mono<Map<*, *>> =
        pythonClient.flask().get("/api/flask/hello?name=$name", Map::class.java)

    /** Call FastAPI backend from Android. */
    fun callFastApiItem(id: Int): Mono<Map<*, *>> =
        pythonClient.fastApi().get("/api/fastapi/items/$id", Map::class.java)

    /** Aggregate Python services health. */
    fun checkPythonHealth(): Mono<Map<String, Any>> =
        pythonClient.checkHealth()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

sealed class UiState<out T> {
    object Idle    : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class UserViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _userState  = MutableStateFlow<UiState<User>>(UiState.Idle)
    val userState: StateFlow<UiState<User>> = _userState.asStateFlow()

    private val _usersState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val usersState: StateFlow<UiState<List<User>>> = _usersState.asStateFlow()

    private val _flaskState = MutableStateFlow<UiState<Map<*, *>>>(UiState.Idle)
    val flaskState: StateFlow<UiState<Map<*, *>>> = _flaskState.asStateFlow()

    /** Load a single user from the REST API. */
    fun loadUser(id: String) {
        _userState.value = UiState.Loading
        repository.getUser(id).subscribe(
            { user ->
                _userState.value = UiState.Success(user)
            },
            { error ->
                _userState.value = UiState.Error(error.message ?: "Unknown error")
            }
        )
    }

    /** Load all users. */
    fun loadAllUsers() {
        _usersState.value = UiState.Loading
        repository.getUsers().subscribe(
            { users ->
                _usersState.value = UiState.Success(users)
            },
            { error ->
                _usersState.value = UiState.Error(error.message ?: "Unknown error")
            }
        )
    }

    /** Create a new user and reload. */
    fun createUser(name: String, email: String) {
        val newUser = User(null, name, email)
        repository.createUser(newUser).subscribe(
            { created ->
                _userState.value = UiState.Success(created)
            },
            { error ->
                _userState.value = UiState.Error(error.message ?: "Failed to create user")
            }
        )
    }

    /** Call Flask Python backend from Android. */
    fun callFlask(name: String) {
        _flaskState.value = UiState.Loading
        repository.callFlaskHello(name).subscribe(
            { result ->
                _flaskState.value = UiState.Success(result)
            },
            { error ->
                _flaskState.value = UiState.Error("Flask error: ${error.message}")
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()

    // Bind your actual view IDs here
    private lateinit var nameText    : TextView
    private lateinit var emailText   : TextView
    private lateinit var flaskText   : TextView
    private lateinit var loadButton  : Button
    private lateinit var flaskButton : Button
    private lateinit var progressBar : ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main)

        // Bind views (replace with actual binding / ViewBinding)
        // nameText    = findViewById(R.id.nameText)
        // emailText   = findViewById(R.id.emailText)
        // flaskText   = findViewById(R.id.flaskText)
        // loadButton  = findViewById(R.id.loadButton)
        // flaskButton = findViewById(R.id.flaskButton)
        // progressBar = findViewById(R.id.progressBar)

        // ── Observe user state ──────────────────────────────────────────────
        viewModel.userState.observe(this) { state ->
            when (state) {
                is UiState.Loading      -> progressBar.visibility = android.view.View.VISIBLE
                is UiState.Success<*>  -> {
                    progressBar.visibility = android.view.View.GONE
                    val user = state.data as User
                    nameText.text  = user.name
                    emailText.text = user.email
                }
                is UiState.Error        -> {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        // ── Observe Flask state ─────────────────────────────────────────────
        viewModel.flaskState.observe(this) { state ->
            when (state) {
                is UiState.Success<*> -> flaskText.text = state.data.toString()
                is UiState.Error      -> Snackbar.make(
                    flaskText, state.message, Snackbar.LENGTH_LONG
                ).show()
                else -> {}
            }
        }

        // ── Button clicks ───────────────────────────────────────────────────
        // loadButton.setOnClickListener  { viewModel.loadUser("1") }
        // flaskButton.setOnClickListener { viewModel.callFlask("AndroidUser") }

        // Auto-load on launch
        viewModel.loadUser("1")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StateFlow observer extension (Lifecycle-aware)
// ─────────────────────────────────────────────────────────────────────────────

private fun <T> StateFlow<T>.observe(
    activity: AppCompatActivity,
    block: (T) -> Unit
) {
    activity.viewModelScope.launch {
        collect { block(it) }
    }
}
