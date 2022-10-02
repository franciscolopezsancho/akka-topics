package faulttolerance2.exception

@SerialVersionUID(1L)
class DbNodeDownException(msg: String)
    extends Exception(msg)
    with Serializable
