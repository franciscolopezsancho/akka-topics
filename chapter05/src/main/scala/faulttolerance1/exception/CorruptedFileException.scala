package faulttolerance1.exception

import java.io.File

@SerialVersionUID(1L)
class CorruptedFileException(msg: String, val file: File)
    extends Exception(msg)
    with Serializable
