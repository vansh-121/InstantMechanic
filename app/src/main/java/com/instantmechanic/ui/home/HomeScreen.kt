package com.instantmechanic.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instantmechanic.R
import com.instantmechanic.core.ui.EmptyState
import com.instantmechanic.core.ui.ErrorState
import com.instantmechanic.core.ui.LoadingState
import com.instantmechanic.domain.model.Mechanic
import com.instantmechanic.domain.model.MechanicSort
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.ui.theme.InstantMechanicTheme

@Composable
fun HomeRoute(
    onMechanicClick: (String) -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onSearchTextChange = viewModel::onSearchTextChange,
        onClearSearch = viewModel::onClearSearch,
        onServiceSelected = viewModel::onServiceSelected,
        onOpenNowToggled = viewModel::onOpenNowToggled,
        onSortSelected = viewModel::onSortSelected,
        onClearFilters = viewModel::onClearFilters,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadNextPage,
        onToggleSimulatedFailure = viewModel::onToggleSimulatedFailure,
        onMechanicClick = onMechanicClick,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onServiceSelected: (ServiceType?) -> Unit,
    onOpenNowToggled: (Boolean) -> Unit,
    onSortSelected: (MechanicSort) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onToggleSimulatedFailure: () -> Unit,
    onMechanicClick: (String) -> Unit,
    onLoadMore: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        state.subtitleCount?.let { count ->
                            Text(
                                text = pluralStringResource(
                                    R.plurals.home_subtitle,
                                    count,
                                    count,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    ThemeToggleAction(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchField(
                value = state.searchText,
                onValueChange = onSearchTextChange,
                onClear = onClearSearch,
            )
            FilterBar(
                selectedService = state.selectedService,
                openNowOnly = state.openNowOnly,
                sort = state.sort,
                onServiceSelected = onServiceSelected,
                onOpenNowToggled = onOpenNowToggled,
                onSortSelected = onSortSelected,
            )

            RefreshBar(visible = state.isRefreshing)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val content = state.content) {
                HomeContent.Loading -> LoadingState()
                is HomeContent.Error -> ErrorState(error = content.error, onRetry = onRetry)
                HomeContent.Empty -> EmptyState(
                    onClearFilters = onClearFilters.takeIf { state.hasActiveFilters },
                )
                is HomeContent.Success -> MechanicList(
                    mechanics = content.mechanics,
                    canLoadMore = state.canLoadMore,
                    isLoadingMore = state.isLoadingMore,
                    totalItems = state.totalItems,
                    onLoadMore = onLoadMore,
                    onMechanicClick = onMechanicClick,
                )
            }
        }
    }
}

/**
 * Slim progress bar for *re*-loads (a new search or filter), as opposed to the full-screen spinner
 * used for the first load.
 *
 * Its 3dp height is reserved whether or not the bar is visible, so toggling a filter never nudges
 * the list up and down. Kept in its own composable so `AnimatedVisibility` resolves against
 * `BoxScope` rather than the caller's `ColumnScope`.
 */
@Composable
private fun RefreshBar(visible: Boolean) {
    Box(modifier = Modifier.height(3.dp)) {
        AnimatedVisibility(visible = visible) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MechanicList(
    mechanics: List<Mechanic>,
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    totalItems: Int,
    onLoadMore: () -> Unit,
    onMechanicClick: (String) -> Unit,
) {
    // Remembered by the list's identity so scroll position survives configuration changes.
    val listState = rememberLazyListState()

    // Trigger loadMore automatically when scrolling near the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 2
        }
    }

    LaunchedEffect(shouldLoadMore, canLoadMore) {
        if (shouldLoadMore && canLoadMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = mechanics, key = { it.id }) { mechanic ->
            MechanicCard(mechanic = mechanic, onClick = { onMechanicClick(mechanic.id) })
        }

        if (isLoadingMore) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Loading more garages...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (canLoadMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = onLoadMore) {
                        Text(
                            text = "Load more garages (${mechanics.size} of $totalItems)",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        } else if (mechanics.size >= totalItems && totalItems > 0) {
            item {
                Text(
                    text = "Showing all $totalItems garages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(28.dp)),
        placeholder = {
            Text(
                text = stringResource(R.string.home_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.home_search_clear),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

@Composable
private fun FilterBar(
    selectedService: ServiceType?,
    openNowOnly: Boolean,
    sort: MechanicSort,
    onServiceSelected: (ServiceType?) -> Unit,
    onOpenNowToggled: (Boolean) -> Unit,
    onSortSelected: (MechanicSort) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = openNowOnly,
            onClick = { onOpenNowToggled(!openNowOnly) },
            shape = RoundedCornerShape(20.dp),
            label = { Text(stringResource(R.string.home_filter_open_now)) },
            leadingIcon = if (openNowOnly) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
        )
        ServiceFilterMenu(selected = selectedService, onSelected = onServiceSelected)
        Spacer(modifier = Modifier.weight(1f))
        SortMenu(sort = sort, onSortSelected = onSortSelected)
    }
}

@Composable
private fun ServiceFilterMenu(
    selected: ServiceType?,
    onSelected: (ServiceType?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            shape = RoundedCornerShape(20.dp),
            label = {
                Text(selected?.label ?: stringResource(R.string.home_filter_all_services))
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_filter_all_services)) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
                trailingIcon = { if (selected == null) SelectedTick() },
            )
            HorizontalDivider()
            ServiceType.entries.filter { it != ServiceType.OTHER }.forEach { service ->
                DropdownMenuItem(
                    text = { Text(service.label) },
                    onClick = {
                        onSelected(service)
                        expanded = false
                    },
                    trailingIcon = { if (selected == service) SelectedTick() },
                )
            }
        }
    }
}

@Composable
private fun SortMenu(
    sort: MechanicSort,
    onSortSelected: (MechanicSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.clickable { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = stringResource(R.string.home_sort_by, sort.label),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = sort.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MechanicSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    trailingIcon = { if (option == sort) SelectedTick() },
                )
            }
        }
    }
}

@Composable
private fun SelectedTick() {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun ThemeToggleAction(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    IconButton(onClick = onToggleTheme) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            contentDescription = stringResource(R.string.home_theme_toggle),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    InstantMechanicTheme {
        HomeScreen(
            state = HomeUiState(content = HomeContent.Loading),
            onSearchTextChange = {},
            onClearSearch = {},
            onServiceSelected = {},
            onOpenNowToggled = {},
            onSortSelected = {},
            onClearFilters = {},
            onRetry = {},
            onToggleSimulatedFailure = {},
            onMechanicClick = {},
        )
    }
}
