package ir.sina.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ir.sina.permission.ui.theme.PermissionTheme
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val mainViewModel by viewModels<MainViewModel>()

    private val permissions = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
    )

    // Register for the activity result for opening a document
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            handleFileUri(it)
        }
    }

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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = {
                            var allPermissionsGranted = true
                            permissions.forEach { permission ->
                                val isGranted =
                                    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

                                if (!isGranted) {
                                    allPermissionsGranted = false
                                    if (shouldShowRequestPermissionRationale(permission)) {
                                        mainViewModel.updateShowDialog(true)
                                    } else {
                                        permissionsResult.launch(permissions)
                                    }
                                }
                            }
                            if (allPermissionsGranted) {
                                writeFileToStorage()
                            }
                        }) {
                            Text(text = "Request Permissions")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            openDocumentLauncher.launch(arrayOf("*/*"))
                        }) {
                            Text(text = "Open File Picker")
                        }
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

    private fun writeFileToStorage() {
        val fileName = "example.txt"
        val fileContent = "Hello, world!"
        val externalStorageVolumes: Array<out File> = ContextCompat.getExternalFilesDirs(applicationContext, null)
        val primaryExternalStorage = externalStorageVolumes[0]

        val file = File(primaryExternalStorage, fileName)
        try {
            file.writeText(fileContent)
            Toast.makeText(this, "File written to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to write file", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleFileUri(uri: Uri) {
        // Handle the file URI
        // For example, you can read the file content here
        val inputStream = contentResolver.openInputStream(uri)
        val fileContent = inputStream?.bufferedReader().use { it?.readText() }
        Toast.makeText(this, "File content: $fileContent", Toast.LENGTH_LONG).show()
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