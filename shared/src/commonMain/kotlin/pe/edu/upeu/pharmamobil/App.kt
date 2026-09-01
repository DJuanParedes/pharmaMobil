package pe.edu.upeu.pharmamobil

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteScreen
import pe.edu.upeu.pharmamobil.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobil.presentation.pedido.PedidoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.productosSimulados
import pe.edu.upeu.pharmamobil.theme.PharmaMobilTheme

private val anchoMediano = 600.dp
private val anchoAmpliado = 840.dp
private val anchoDrawer = 320.dp

@Composable
@Preview
fun App() {
    var darkTheme by remember { mutableStateOf(false) }

    PharmaMobilTheme(darkTheme = darkTheme) {
        PharmaMobilApp(
            darkTheme = darkTheme,
            onDarkThemeChange = { darkTheme = it },
        )
    }
}

@Composable
private fun PharmaMobilApp(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    var pantallaActual by remember { mutableStateOf<Screen>(Screen.Inicio) }
    var inventario by remember { mutableStateOf(productosSimulados) }
    val saveableStateHolder = rememberSaveableStateHolder()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        when {
            maxWidth < anchoMediano -> {
                NavegacionCompacta(
                    pantallaActual = pantallaActual,
                    darkTheme = darkTheme,
                    onPantallaSeleccionada = { pantallaActual = it },
                    onDarkThemeChange = onDarkThemeChange,
                    contenido = { onAbrirMenu ->
                        ContenidoPrincipal(
                            pantallaActual = pantallaActual,
                            darkTheme = darkTheme,
                            onDarkThemeChange = onDarkThemeChange,
                            onAbrirMenu = onAbrirMenu,
                        ) {
                            saveableStateHolder.SaveableStateProvider(pantallaActual.id) {
                                PantallaActiva(
                                    pantallaActual = pantallaActual,
                                    productos = inventario,
                                    onInventarioChange = { inventario = it },
                                    onPantallaSeleccionada = { pantallaActual = it },
                                )
                            }
                        }
                    },
                )
            }

            maxWidth < anchoAmpliado -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    BarraNavegacionMediana(
                        pantallaActual = pantallaActual,
                        onPantallaSeleccionada = { pantallaActual = it },
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        ContenidoPrincipal(
                            pantallaActual = pantallaActual,
                            darkTheme = darkTheme,
                            onDarkThemeChange = onDarkThemeChange,
                        ) {
                            saveableStateHolder.SaveableStateProvider(pantallaActual.id) {
                                PantallaActiva(
                                    pantallaActual = pantallaActual,
                                    productos = inventario,
                                    onInventarioChange = { inventario = it },
                                    onPantallaSeleccionada = { pantallaActual = it },
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet(modifier = Modifier.width(anchoDrawer)) {
                            ContenidoDrawer(
                                pantallaActual = pantallaActual,
                                darkTheme = darkTheme,
                                onPantallaSeleccionada = { pantallaActual = it },
                                onDarkThemeChange = onDarkThemeChange,
                            )
                        }
                    },
                ) {
                    ContenidoPrincipal(
                        pantallaActual = pantallaActual,
                        darkTheme = darkTheme,
                        onDarkThemeChange = onDarkThemeChange,
                    ) {
                        saveableStateHolder.SaveableStateProvider(pantallaActual.id) {
                            PantallaActiva(
                                pantallaActual = pantallaActual,
                                productos = inventario,
                                onInventarioChange = { inventario = it },
                                onPantallaSeleccionada = { pantallaActual = it },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavegacionCompacta(
    pantallaActual: Screen,
    darkTheme: Boolean,
    onPantallaSeleccionada: (Screen) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    contenido: @Composable ((() -> Unit)) -> Unit,
) {
    val drawerState = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(anchoDrawer)) {
                ContenidoDrawer(
                    pantallaActual = pantallaActual,
                    darkTheme = darkTheme,
                    onPantallaSeleccionada = { screen ->
                        onPantallaSeleccionada(screen)
                        scope.launch { drawerState.close() }
                    },
                    onDarkThemeChange = onDarkThemeChange,
                )
            }
        },
    ) {
        contenido { scope.launch { drawerState.open() } }
    }
}

@Composable
private fun ContenidoDrawer(
    pantallaActual: Screen,
    darkTheme: Boolean,
    onPantallaSeleccionada: (Screen) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 20.dp),
    ) {
        Text(
            text = "PharmaMobil",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            text = "Gestión farmacéutica",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        )

        HorizontalDivider()

        Screen.destinos.forEach { screen ->
            NavigationDrawerItem(
                icon = { DestinoIcono(screen) },
                label = { Text(screen.titulo) },
                selected = pantallaActual == screen,
                onClick = { onPantallaSeleccionada(screen) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Modo oscuro",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = darkTheme,
                onCheckedChange = onDarkThemeChange,
            )
        }
    }
}

@Composable
private fun BarraNavegacionMediana(
    pantallaActual: Screen,
    onPantallaSeleccionada: (Screen) -> Unit,
) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        Text(
            text = "PM",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 20.dp),
        )
        Screen.destinos.forEach { screen ->
            NavigationRailItem(
                selected = pantallaActual == screen,
                onClick = { onPantallaSeleccionada(screen) },
                icon = { DestinoIcono(screen) },
                label = { Text(screen.titulo) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoPrincipal(
    pantallaActual: Screen,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onAbrirMenu: (() -> Unit)? = null,
    contenido: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pantallaActual.titulo) },
                navigationIcon = {
                    if (onAbrirMenu != null) {
                        IconButton(onClick = onAbrirMenu) {
                            Text(
                                text = "☰",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { onDarkThemeChange(!darkTheme) }) {
                        Text(if (darkTheme) "Tema claro" else "Tema oscuro")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            contenido()
        }
    }
}

@Composable
private fun DestinoIcono(screen: Screen) {
    Text(
        text = screen.abreviatura,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun PantallaActiva(
    pantallaActual: Screen,
    productos: List<Producto>,
    onInventarioChange: (List<Producto>) -> Unit,
    onPantallaSeleccionada: (Screen) -> Unit,
) {
    when (pantallaActual) {
        Screen.Inicio -> InicioScreen(onPantallaSeleccionada)
        Screen.Productos -> ProductoScreen(
            productos = productos,
            onInventarioChange = onInventarioChange,
        )
        Screen.Clientes -> ClienteScreen()
        Screen.Pedidos -> PedidoScreen()
    }
}
