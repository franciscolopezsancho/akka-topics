package faulttolerance3.exception

import java.io.File

//LogProcessor's parser exception
@SerialVersionUID(1L)
class ParseException(msg: String, val file: File)
    extends Exception(msg)
    with Serializable
