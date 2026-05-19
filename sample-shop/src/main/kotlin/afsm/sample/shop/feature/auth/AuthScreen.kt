package afsm.sample.shop.feature.auth

import afsm.compose.CollectAfsmEffects
import afsm.sample.shop.app.ShopAppContainer
import afsm.sample.shop.app.sampleViewModelFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthRoute(
    container: ShopAppContainer,
    onAuthenticated: () -> Unit,
) {
    val factory = remember(container) {
        sampleViewModelFactory {
            AuthViewModel(
                authRepository = container.authRepository,
                sessionRepository = container.sessionRepository,
            )
        }
    }
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val renderState = state.toRenderState()

    CollectAfsmEffects(viewModel.effects) { effect ->
        when (effect) {
            AuthEffect.OpenCatalog -> onAuthenticated()
        }
    }

    AuthScreen(
        state = renderState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun AuthScreen(
    state: AuthRenderState,
    onEvent: (AuthEvent) -> Unit,
) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Afsm Shop",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Reference app for Android state-machine flows",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (state.isAuthenticated) {
                Text(
                    text = "Signed in",
                    style = MaterialTheme.typography.titleLarge,
                )
                state.authenticatedEmail?.let { email ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = state.mode != AuthMode.Login && !state.isLoading,
                        onClick = { onEvent(AuthEvent.ModeChanged(AuthMode.Login)) },
                    ) {
                        Text("Login")
                    }
                    TextButton(
                        enabled = state.mode != AuthMode.Register && !state.isLoading,
                        onClick = { onEvent(AuthEvent.ModeChanged(AuthMode.Register)) },
                    ) {
                        Text("Register")
                    }
                }

                if (state.mode == AuthMode.Register) {
                    OutlinedTextField(
                        value = state.form.name,
                        enabled = !state.isLoading,
                        onValueChange = { onEvent(AuthEvent.NameChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Name") },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = state.form.email,
                    enabled = !state.isLoading,
                    onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.form.password,
                    enabled = !state.isLoading,
                    onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                )

                state.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    enabled = !state.isLoading,
                    onClick = { onEvent(AuthEvent.SubmitClicked) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = when (state.mode) {
                                AuthMode.Login -> "Login"
                                AuthMode.Register -> "Create account"
                            },
                        )
                    }
                }
            }
        }
    }
}
