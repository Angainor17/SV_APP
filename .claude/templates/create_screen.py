#!/usr/bin/env python3
"""
Генератор каркаса нового экрана для SV APP.
Использование: python create_screen.py --feature <name> --package <package>
Пример: python create_screen.py --feature bookmarks --package su.sv.books
"""

import argparse
import os
import re
from pathlib import Path


def to_camel_case(snake_str: str) -> str:
    """Convert snake_case to CamelCase"""
    return ''.join(x.title() for x in snake_str.split('_'))


def to_pascal_case(snake_str: str) -> str:
    """Convert snake_case to PascalCase (alias for camel_case with first letter upper)"""
    return to_camel_case(snake_str)


def to_snake_case(camel_str: str) -> str:
    """Convert CamelCase to snake_case"""
    return re.sub(r'(?<!^)(?=[A-Z])', '_', camel_str).lower()


def create_file(path: Path, content: str):
    """Create file with given content"""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
    print(f"Created: {path}")


def generate_domain_models(feature_name: str, package: str) -> dict:
    """Generate domain layer files"""
    pascal = to_pascal_case(feature_name)
    camel = pascal

    # Domain Model
    model_content = f'''package {package}.{feature_name}.domain.model

data class {camel}Item(
    val id: String,
    val title: String,
    val description: String,
)
'''

    # Repository Interface
    repo_content = f'''package {package}.{feature_name}.domain.repository

import {package}.{feature_name}.domain.model.{camel}Item

interface {camel}Repository {{
    suspend fun getItems(): Result<List<{camel}Item>>
}}
'''

    # UseCase
    usecase_content = f'''package {package}.{feature_name}.domain

import {package}.{feature_name}.domain.model.{camel}Item
import {package}.{feature_name}.domain.repository.{camel}Repository
import javax.inject.Inject

class Get{camel}ItemsUseCase @Inject constructor(
    private val repository: {camel}Repository,
) {{
    suspend fun execute(): Result<List<{camel}Item>> {{
        return repository.getItems()
    }}
}}
'''

    return {
        f'domain/model/{camel}Item.kt': model_content,
        f'domain/repository/{camel}Repository.kt': repo_content,
        f'domain/Get{camel}ItemsUseCase.kt': usecase_content,
    }


def generate_presentation_files(feature_name: str, package: str) -> dict:
    """Generate presentation layer files"""
    pascal = to_pascal_case(feature_name)
    camel = pascal

    # UI State
    state_content = f'''package {package}.{feature_name}.presentation.root.model

import {package}.{feature_name}.presentation.model.Ui{camel}Item

sealed class UiRoot{camel}State {{
    object Loading : UiRoot{camel}State()

    data class Content(
        val items: List<Ui{camel}Item>,
        val isRefreshing: Boolean = false,
    ) : UiRoot{camel}State()

    data class Error(val message: String) : UiRoot{camel}State()

    object Empty : UiRoot{camel}State()
}}
'''

    # UI Item
    item_content = f'''package {package}.{feature_name}.presentation.model

data class Ui{camel}Item(
    val id: String,
    val title: String,
    val description: String,
)
'''

    # Actions
    actions_content = f'''package {package}.{feature_name}.presentation.root.viewmodel.actions

import {package}.{feature_name}.presentation.model.Ui{camel}Item

sealed class {camel}Actions {{
    object OnRetryClick : {camel}Actions()
    object OnSwipeRefresh : {camel}Actions()
    data class OnItemClick(val item: Ui{camel}Item) : {camel}Actions()
}}
'''

    # Actions Handler
    handler_content = f'''package {package}.{feature_name}.presentation.root.viewmodel.actions

interface {camel}ActionsHandler {{
    fun onAction(action: {camel}Actions)
}}
'''

    # Effects
    effects_content = f'''package {package}.{feature_name}.presentation.root.viewmodel.effects

import {package}.{feature_name}.presentation.model.Ui{camel}Item

sealed class {camel}OneTimeEffect {{
    data class OpenDetail(val item: Ui{camel}Item) : {camel}OneTimeEffect()
    data class ShowErrorSnackBar(val text: String) : {camel}OneTimeEffect()
}}
'''

    # Mapper
    mapper_content = f'''package {package}.{feature_name}.presentation.root.mapper

import {package}.{feature_name}.domain.model.{camel}Item
import {package}.{feature_name}.presentation.model.Ui{camel}Item
import javax.inject.Inject

class Ui{camel}Mapper @Inject constructor() {{

    fun mapToUi(domain: {camel}Item): Ui{camel}Item {{
        return Ui{camel}Item(
            id = domain.id,
            title = domain.title,
            description = domain.description,
        )
    }}

    fun mapToUiList(domain: List<{camel}Item>): List<Ui{camel}Item> {{
        return domain.map {{ mapToUi(it) }}
    }}
}}
'''

    # ViewModel
    viewmodel_content = f'''package {package}.{feature_name}.presentation.root.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import {package}.{feature_name}.domain.Get{camel}ItemsUseCase
import {package}.{feature_name}.presentation.root.mapper.Ui{camel}Mapper
import {package}.{feature_name}.presentation.root.model.UiRoot{camel}State
import {package}.{feature_name}.presentation.root.viewmodel.actions.{camel}Actions
import {package}.{feature_name}.presentation.root.viewmodel.actions.{camel}ActionsHandler
import {package}.{feature_name}.presentation.root.viewmodel.effects.{camel}OneTimeEffect
import javax.inject.Inject

@HiltViewModel
class Root{camel}ViewModel @Inject constructor(
    private val getItemsUseCase: Get{camel}ItemsUseCase,
    private val uiMapper: Ui{camel}Mapper,
) : BaseViewModel(), {camel}ActionsHandler {{

    private val _state = MutableStateFlow<UiRoot{camel}State>(UiRoot{camel}State.Loading)
    val state: StateFlow<UiRoot{camel}State> get() = _state

    private val _oneTimeEffect = Channel<{camel}OneTimeEffect>(capacity = Channel.BUFFERED)
    val oneTimeEffect: Flow<{camel}OneTimeEffect> get() = _oneTimeEffect.receiveAsFlow()

    init {{
        loadItems()
    }}

    private fun loadItems() {{
        _state.value = UiRoot{camel}State.Loading
        viewModelScope.launch {{
            getItemsUseCase.execute().fold(
                onSuccess = {{ items ->
                    val uiItems = uiMapper.mapToUiList(items)
                    _state.value = if (uiItems.isEmpty()) {{
                        UiRoot{camel}State.Empty
                    }} else {{
                        UiRoot{camel}State.Content(items = uiItems)
                    }}
                }},
                onFailure = {{
                    _state.value = UiRoot{camel}State.Error(it.message ?: "Unknown error")
                }},
            )
        }}
    }}

    override fun onAction(action: {camel}Actions) {{
        when (action) {{
            is {camel}Actions.OnRetryClick -> {{
                loadItems()
            }}
            is {camel}Actions.OnSwipeRefresh -> {{
                updateState {{ it.copy(isRefreshing = true) }}
                loadItems()
            }}
            is {camel}Actions.OnItemClick -> {{
                _oneTimeEffect.trySend({camel}OneTimeEffect.OpenDetail(action.item))
            }}
        }}
    }}

    private fun updateState(action: (UiRoot{camel}State.Content) -> UiRoot{camel}State.Content) {{
        _state.update {{ state ->
            if (state is UiRoot{camel}State.Content) {{
                action.invoke(state)
            }} else {{
                state
            }}
        }}
    }}
}}
'''

    # Screen UI
    screen_content = f'''package {package}.{feature_name}.presentation.root.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import kotlinx.coroutines.launch
import su.sv.commonui.ui.FullScreenError
import su.sv.commonui.ui.FullScreenLoading
import su.sv.commonui.ui.OneTimeEffect
import {package}.{feature_name}.R
import {package}.{feature_name}.presentation.root.model.UiRoot{camel}State
import {package}.{feature_name}.presentation.root.viewmodel.Root{camel}ViewModel
import {package}.{feature_name}.presentation.root.viewmodel.actions.{camel}Actions
import {package}.{feature_name}.presentation.root.viewmodel.effects.{camel}OneTimeEffect

@Composable
fun Root{camel}(
    viewModel: Root{camel}ViewModel = hiltViewModel(),
) {{
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember {{ SnackbarHostState() }}

    HandleEffects(viewModel, snackbarHostState)

    Scaffold(
        snackbarHost = {{ SnackbarHost(hostState = snackbarHostState) }},
    ) {{
        when (val currentState = state.value) {{
            is UiRoot{camel}State.Loading -> FullScreenLoading()
            is UiRoot{camel}State.Content -> {{
                {camel}List(
                    state = currentState,
                    actions = viewModel,
                )
            }}
            is UiRoot{camel}State.Error -> FullScreenError {{
                viewModel.onAction({camel}Actions.OnRetryClick)
            }}
            is UiRoot{camel}State.Empty -> Empty{camel}()
        }}
    }}
}}

@Composable
private fun HandleEffects(
    viewModel: Root{camel}ViewModel,
    snackbarHostState: SnackbarHostState,
) {{
    val scope = rememberCoroutineScope()
    val stackNavigation = LocalStackNavigation.current

    OneTimeEffect(viewModel.oneTimeEffect) {{ effect ->
        when (effect) {{
            is {camel}OneTimeEffect.OpenDetail -> {{
                // TODO: Navigate to detail screen
                // stackNavigation.forward(DetailScreen(effect.item))
            }}
            is {camel}OneTimeEffect.ShowErrorSnackBar -> {{
                scope.launch {{
                    snackbarHostState.showSnackbar(
                        message = effect.text,
                        duration = SnackbarDuration.Short,
                    )
                }}
            }}
        }}
    }}
}}

@Composable
fun Empty{camel}() {{
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {{
        Column(modifier = Modifier.wrapContentSize()) {{
            Text(stringResource(R.string.{feature_name}_empty_list_title))
        }}
    }}
}}
'''

    # List
    list_content = f'''package {package}.{feature_name}.presentation.root.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import {package}.{feature_name}.presentation.model.Ui{camel}Item
import {package}.{feature_name}.presentation.root.model.UiRoot{camel}State
import {package}.{feature_name}.presentation.root.viewmodel.actions.{camel}ActionsHandler

@Composable
fun {camel}List(
    state: UiRoot{camel}State.Content,
    actions: {camel}ActionsHandler,
) {{
    LazyColumn {{
        items(items = state.items, key = {{ it.id }}) {{ item ->
            {camel}Item(
                item = item,
                onClick = {{ actions.onAction({camel}Actions.OnItemClick(item)) }}
            )
        }}
    }}
}}

@Composable
fun {camel}Item(
    item: Ui{camel}Item,
    onClick: () -> Unit,
) {{
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {{
        Column(
            modifier = Modifier.padding(16.dp)
        ) {{
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
            )
        }}
    }}
}}
'''

    return {
        f'presentation/root/model/UiRoot{camel}State.kt': state_content,
        f'presentation/model/Ui{camel}Item.kt': item_content,
        f'presentation/root/viewmodel/actions/{camel}Actions.kt': actions_content,
        f'presentation/root/viewmodel/actions/{camel}ActionsHandler.kt': handler_content,
        f'presentation/root/viewmodel/effects/{camel}OneTimeEffect.kt': effects_content,
        f'presentation/root/mapper/Ui{camel}Mapper.kt': mapper_content,
        f'presentation/root/viewmodel/Root{camel}ViewModel.kt': viewmodel_content,
        f'presentation/root/ui/Root{camel}.kt': screen_content,
        f'presentation/root/ui/{camel}List.kt': list_content,
    }


def generate_modo_screen(feature_name: str, package: str) -> str:
    """Generate Modo Screen file"""
    pascal = to_pascal_case(feature_name)

    return f'''package {package}.{feature_name}.nav

import android.os.Parcelable
import com.github.terrakok.modo.Screen
import kotlinx.parcelize.Parcelize
import {package}.{feature_name}.presentation.root.ui.Root{pascal}

@Parcelize
class {pascal}Screen : Screen, Parcelable {{

    @Composable
    override fun Content(modifier: Modifier) {{
        Root{pascal}()
    }}
}}
'''


def generate_di_module(feature_name: str, package: str) -> str:
    """Generate DI module"""
    pascal = to_pascal_case(feature_name)
    camel = pascal

    return f'''package {package}.{feature_name}.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object {camel}Module {{
    // TODO: Add DI providers for API, Repository, etc.
}}
'''


def generate_strings_xml(feature_name: str) -> str:
    """Generate strings.xml resource file"""
    return f'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="{feature_name}_empty_list_title">Нет данных</string>
    <string name="{feature_name}_error_loading">Ошибка загрузки</string>
</resources>
'''


def main():
    parser = argparse.ArgumentParser(description='Generate new screen skeleton for SV APP')
    parser.add_argument('--feature', required=True, help='Feature name in snake_case (e.g., bookmarks)')
    parser.add_argument('--package', required=True, help='Package name (e.g., su.sv.books)')
    parser.add_argument('--output', default='.', help='Output directory (default: current directory)')

    args = parser.parse_args()

    feature_name = args.feature.lower()
    package = args.package
    output_dir = Path(args.output) / feature_name / 'src' / 'main' / 'java' / package.replace('.', '/') / feature_name

    print(f"\nGenerating screen: {feature_name}")
    print(f"Package: {package}")
    print(f"Output: {output_dir}\n")

    # Generate domain files
    domain_files = generate_domain_models(feature_name, package)
    for file_path, content in domain_files.items():
        create_file(output_dir / file_path, content)

    # Generate presentation files
    presentation_files = generate_presentation_files(feature_name, package)
    for file_path, content in presentation_files.items():
        create_file(output_dir / file_path, content)

    # Generate Modo Screen
    screen_content = generate_modo_screen(feature_name, package)
    create_file(output_dir / 'nav' / f'{to_pascal_case(feature_name)}Screen.kt', screen_content)

    # Generate DI Module
    di_content = generate_di_module(feature_name, package)
    create_file(output_dir / 'di' / f'{to_pascal_case(feature_name)}Module.kt', di_content)

    # Generate strings.xml
    res_dir = Path(args.output) / feature_name / 'src' / 'main' / 'res' / 'values'
    create_file(res_dir / 'strings.xml', generate_strings_xml(feature_name))

    print(f"\n✅ Screen '{feature_name}' generated successfully!")
    print("\nNext steps:")
    print("1. Add the module to settings.gradle.kts")
    print("2. Add dependencies to the module's build.gradle.kts")
    print("3. Implement API and Repository")
    print("4. Add string resources")
    print("5. Write unit tests for UseCase and ViewModel")
    print("6. Run ./gradlew detekt to check code quality")


if __name__ == '__main__':
    main()
