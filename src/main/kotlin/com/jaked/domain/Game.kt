package com.jaked.domain

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

//Tic Tac Toe Game
class Game {
    private val state = MutableStateFlow(GameState())

    //make sure we can access this hashmap from different threads
    private val playerSockets = ConcurrentHashMap<Char, WebSocketSession>()

    //coroutine scope inside the class to launch coroutine scope
    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var delayGameJob: Job? = null

    //place initialization code
    init {
        //every time state changes, call broadcase function with the new state
        state.onEach (::broadcast).launchIn(gameScope)
    }

    fun connectPlayer(session: WebSocketSession): Char? {
        val isPlayerX = state.value.connectedPlayers.any{ it == 'X'}
        val player = if(isPlayerX) 'O' else 'X'

        //update state flow
        state.update {
            //play already existed
            if(state.value.connectedPlayers.contains(player)) {
                return null
            }

            //assign player session
            if(!playerSockets.containsKey(player)){
                playerSockets[player] = session
            }

            it.copy(
                connectedPlayers = it.connectedPlayers + player
            )
        }
        println("player connected")
        return player
    }

    fun disconnectPlayer(player: Char){
        //remove player from the hashmap
        playerSockets.remove(player)

        //update state flow
        state.update {
            it.copy(
                connectedPlayers = it.connectedPlayers - player
            )
        }
    }

    suspend fun broadcast(state: GameState) {
        //broadcase gameState to all players
        playerSockets.values.forEach{
            socket -> socket.send(
                //serialize class into json string
                Json.encodeToString(state)
            )
        }
    }

    fun finishTurn(player: Char, x: Int, y:Int){
        if(state.value.field[y][x] != null || state.value.winningPlayer != null){
            return
        }

        if(state.value.playerAtTurn != player){
            return
        }

        val currentPlayer = state.value.playerAtTurn
        state.update {
            val newField = it.field.also { field ->
                field[y][x] = currentPlayer
            }

            val isBoardFull = newField.all{ it.all {it != null }}
            if(isBoardFull){
                startNewRoundDelayed()
            }

            //update new game state
            it.copy(
                playerAtTurn = if(currentPlayer == 'X') 'O' else 'X',
                field = newField,
                isBoardFull = isBoardFull,
                winningPlayer = getWinnerPlay()?.also{
                    startNewRoundDelayed()
                }
            )
        }
    }

    private fun startNewRoundDelayed() {
        delayGameJob?.cancel()
        delayGameJob = gameScope.launch {
            delay(5000L)
            //start a new game
            state.update {
                it.copy(
                    playerAtTurn = 'X',
                    field = GameState.emptyField(),
                    winningPlayer = null,
                    isBoardFull = false,
                )
            }
        }
    }

    //determine if we have a winner in the board
    private fun getWinnerPlay(): Char? {
        val field = state.value.field
        return if (field[0][0] != null && field[0][0] == field[0][1] && field[0][1] == field[0][2]) {
            field[0][0]
        } else if (field[1][0] != null && field[1][0] == field[1][1] && field[1][1] == field[1][2]) {
            field[1][0]
        } else if (field[2][0] != null && field[2][0] == field[2][1] && field[2][1] == field[2][2]) {
            field[2][0]
        } else if (field[0][0] != null && field[0][0] == field[1][0] && field[1][0] == field[2][0]) {
            field[0][0]
        } else if (field[0][1] != null && field[0][1] == field[1][1] && field[1][1] == field[2][1]) {
            field[0][1]
        } else if (field[0][2] != null && field[0][2] == field[1][2] && field[1][2] == field[2][2]) {
            field[0][2]
        } else if (field[0][0] != null && field[0][0] == field[1][1] && field[1][1] == field[2][2]) {
            field[0][0]
        } else if (field[0][2] != null && field[0][2] == field[1][1] && field[1][1] == field[2][0]) {
            field[0][2]
        } else null
    }
}