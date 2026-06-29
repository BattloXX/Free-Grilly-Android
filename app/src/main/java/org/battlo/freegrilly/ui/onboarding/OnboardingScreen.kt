package org.battlo.freegrilly.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.battlo.freegrilly.R
import org.battlo.freegrilly.data.api.models.WifiNetwork
import org.battlo.freegrilly.ui.components.StepIndicator
import org.battlo.freegrilly.ui.theme.LocalGrillyColors

@Composable
fun OnboardingScreen(
    onSetupComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val colors = LocalGrillyColors.current

    LaunchedEffect(step) {
        if (step is OnboardingStep.Complete) onSetupComplete()
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Fehler") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } },
        )
    }

    val stepIndex = when (step) {
        is OnboardingStep.ApConnect -> 0
        is OnboardingStep.WifiScan -> 1
        is OnboardingStep.Credentials -> 2
        is OnboardingStep.Provisioning -> 3
        is OnboardingStep.Discovery, is OnboardingStep.Complete -> 4
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Image(
            painter = painterResource(R.drawable.logo_grilly),
            contentDescription = "Free-Grilly",
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(12.dp))
        Text("Free-Grilly", style = MaterialTheme.typography.headlineLarge, color = colors.emberOrange)
        Text(
            stringResource(R.string.onboarding_setup),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        StepIndicator(totalSteps = 5, currentStep = stepIndex)
        Spacer(Modifier.height(32.dp))

        when (val s = step) {
            is OnboardingStep.ApConnect -> ApConnectStep(
                onNext = { viewModel.onApConnected() },
                onDemo = { viewModel.skipToDemo() },
            )
            is OnboardingStep.WifiScan -> LoadingStep(stringResource(R.string.scanning_wifi))
            is OnboardingStep.Credentials -> CredentialsStep(
                networks = s.networks,
                onSubmit = { ssid, pw, name, unit -> viewModel.onCredentialsSubmitted(ssid, pw, name, unit) },
            )
            is OnboardingStep.Provisioning -> LoadingStep(stringResource(R.string.provisioning))
            is OnboardingStep.Discovery -> LoadingStep(stringResource(R.string.discovering_device))
            is OnboardingStep.Complete -> LoadingStep(stringResource(R.string.connecting))
        }
    }
}

@Composable
private fun ApConnectStep(onNext: () -> Unit, onDemo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.step_ap_connect_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.step_ap_connect_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.connected_continue))
        }
        TextButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.use_demo_mode))
        }
    }
}

@Composable
private fun LoadingStep(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CredentialsStep(
    networks: List<WifiNetwork>,
    onSubmit: (String, String, String, String) -> Unit,
) {
    var selectedSsid by remember { mutableStateOf(networks.firstOrNull()?.ssid ?: "") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var grillName by remember { mutableStateOf("My BBQ") }
    var unit by remember { mutableStateOf("celcius") }
    var expandSsid by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.step_credentials_title), style = MaterialTheme.typography.headlineSmall)

        ExposedDropdownMenuBox(expanded = expandSsid, onExpandedChange = { expandSsid = it }) {
            OutlinedTextField(
                value = selectedSsid,
                onValueChange = {},
                readOnly = true,
                label = { Text("WLAN-Netzwerk") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandSsid) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expandSsid, onDismissRequest = { expandSsid = false }) {
                networks.forEach { net ->
                    DropdownMenuItem(
                        text = { Text("${net.ssid}  (${net.rssi} dBm, ${net.encryption})") },
                        onClick = { selectedSsid = net.ssid; expandSsid = false },
                    )
                }
            }
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = grillName,
            onValueChange = { grillName = it },
            label = { Text(stringResource(R.string.grill_name)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = unit == "celcius", onClick = { unit = "celcius" }, label = { Text("°C") }, modifier = Modifier.weight(1f))
            FilterChip(selected = unit == "fahrenheit", onClick = { unit = "fahrenheit" }, label = { Text("°F") }, modifier = Modifier.weight(1f))
        }

        Button(
            onClick = { onSubmit(selectedSsid, password, grillName, unit) },
            enabled = selectedSsid.isNotEmpty() && password.isNotEmpty() && grillName.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.configure_device)) }
    }
}
