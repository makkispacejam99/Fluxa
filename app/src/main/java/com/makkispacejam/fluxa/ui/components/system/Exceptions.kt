package com.makkispacejam.fluxa.ui.components.system

// Error de internet
enum class ErrorType { NO_INTERNET, SERVER_ERROR }

class NewPipeServerException : Exception("¡Vaya!, ha ocurrido un error. Lo solucionaremos pronto.")
