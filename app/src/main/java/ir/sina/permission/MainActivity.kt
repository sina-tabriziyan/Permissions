package ir.sina.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontVariation
import ir.sina.permission.ui.theme.PermissionTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel by viewModels<MainViewModel>()

    private val permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PermissionTheme {
                val showDialog = mainViewModel.showDialog.collectAsState().value
                val launchAppSettings = mainViewModel.launchAppAppSetting.collectAsState().value
                val permissionsResult =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions(),
                        onResult = { result: Map<String, Boolean> ->
                            permissions.forEach { permission ->
                                if (result[permission] == false) {
                                    if (!shouldShowRequestPermissionRationale(permission)) {
                                        mainViewModel.updateLaunchAppSettings(true)
                                    }
                                    mainViewModel.updateShowDialog(true)
                                }
                            }
                        })

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = {
                        permissions.forEach { permission ->
                            val isGranted =
                                checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

                            if (!isGranted) {
                                if (shouldShowRequestPermissionRationale(permission)) {
                                    mainViewModel.updateShowDialog(true)
                                } else {
                                    permissionsResult.launch(permissions)
                                }
                            }
                        }
                    }) {
                        Text(text = "Request Permissions")
                    }
                }

                if (showDialog) {
                    PermissionDialog(
                        onDismiss = { mainViewModel.updateShowDialog(false) },
                        onConfirm = {
                            mainViewModel.updateShowDialog(false)
                            if (launchAppSettings) {
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", packageName, null)
                                ).also {
                                    startActivity(it)
                                }
                                mainViewModel.updateLaunchAppSettings(false)

                            } else {
                                permissionsResult.launch(permissions)
                            }
                        })
                }
            }
        }
    }
}


@Composable
fun PermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {

    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Ok")
            }
        },
        title = {
            Text(text = "Storage need to be granted")
        },
        text = { Text(text = "This app needs access the Storage") })

}