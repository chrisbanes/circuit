package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.setValue
import com.slack.circuit.backstack.BackStackRecordLocalSaveableStateRegistry
import com.slack.circuit.retained.CanRetainChecker
import com.slack.circuit.retained.LocalCanRetainChecker
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter

public abstract class PauseablePresenter<UiState : CircuitUiState>(isPaused: Boolean = false) :
    Presenter<UiState> {

    public var isPaused: Boolean by mutableStateOf(isPaused)

    @Composable
    override fun present(): UiState {
        var lastState by remember { mutableStateOf<UiState?>(null) }

        if (!isPaused || lastState == null) {
            CompositionLocalProvider(LocalCanRetainChecker provides CanRetainChecker { !isPaused }) {
                val innerRetainedStateRegistry = rememberRetained { RetainedStateRegistry() }
                CompositionLocalProvider(LocalRetainedStateRegistry provides innerRetainedStateRegistry) {
                    lastState = _present()
                }
            }
        }

        return lastState!!
    }

    @Composable
    protected abstract fun _present(): UiState
}

public fun <UiState : CircuitUiState> Presenter<UiState>.toPauseablePresenter(
    isPaused: Boolean = false
): PauseablePresenter<UiState> {
    if (this is PauseablePresenter<UiState>) return this
    // Else we wrap the presenter
    return object : PauseablePresenter<UiState>(isPaused) {
        @Composable
        override fun _present(): UiState = this@toPauseablePresenter.present()
    }
}
