package faulttolerance3.exception

@SerialVersionUID(1L)
class UnexpectedColumnsException(msg: String)
    extends Exception(msg)
    with Serializable
